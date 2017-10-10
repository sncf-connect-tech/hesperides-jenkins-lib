// This file is part of the hesperides-jenkins-lib distribution.
// (https://github.com/voyages-sncf-technologies/hesperides-jenkins-lib)
// Copyright (c) 2017 VSCT.
//
// hesperides-jenkins-lib is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as
// published by the Free Software Foundation, version 3.
//
// hesperides-jenkins-lib is distributed in the hope that it will be useful, but
// WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program. If not, see <http://www.gnu.org/licenses/>.

package com.vsct.dt.hesperides.jenkins.pipelines


import static groovy.json.JsonOutput.*

import static com.vsct.dt.hesperides.jenkins.pipelines.LogUtils.*

import groovy.json.JsonSlurperClassic

import com.cloudbees.groovy.cps.NonCPS

import com.vsct.dt.hesperides.jenkins.pipelines.http.HttpException
import com.vsct.dt.hesperides.jenkins.pipelines.http.SerializableURIBuilder


// This class MUST be serializable in order to be usable in Jenkins pipeline
class Hesperides implements Serializable {

    private static final long serialVersionUID = 13648552184566987952L

    static final DEFAULT_API_ROOT_URL = 'https://hesperides'

    String apiRootUrl
    String authHeader
    Object steps
    Object httpRequester

    Hesperides(Map args = [:]) { required(args, ['httpRequester']) // optional: apiRootUrl, auth, steps -> indicates a Jenkins pipeline context
        this.httpRequester = args.httpRequester
        if (args.auth) {
            if (!(args.auth instanceof CharSequence)) {
                if (!args.auth.username || !args.auth.password) {
                    throw new IllegalArgumentException('auth.username & auth.password are required')
                }
                args.auth = "${args.auth.username}:${args.auth.password}"
            }
            this.authHeader = 'Basic ' + args.auth.bytes.encodeBase64()
        }
        this.apiRootUrl = args.apiRootUrl ?: DEFAULT_API_ROOT_URL
        if (this.apiRootUrl[-1] == '/') {
            this.apiRootUrl = this.apiRootUrl[0..-2]
        }
        this.steps = args.steps
    }



    /******************************************************************************

                                APPLICATIONS & PLATFORMS

    ******************************************************************************/

    def getAppInfo(app) { required(args, ['app'])
        httpRequest(path: "/rest/applications/${app}")
    }

    def getPlatformInfo(Map args) { required(args, ['app', 'platform'])
        httpRequest(path: "/rest/applications/${args.app}/platforms/${args.platform}")
    }

    def createPlatform(args) { required(args, ['app', 'platform', 'version']) // optional: modules, isProduction, fromApplication, fromPlatform
        def platformInfo = [
            application_name: args.app,
            platform_name: args.platform,
            application_version: args.version,
            modules: args.modules ?: [],
            production: args.isProduction ?: false,
            version_id: 0
        ]
        httpRequest(method: 'POST',
                    path: "/rest/applications/${args.app}/platforms",
                    query: [
                        application_name : args.app,
                        from_application: args.fromApplication,
                        from_platform: args.fromPlatform,
                        copyPropertiesForUpgradedModules: true
                    ],
                    body: toJson(platformInfo))
    }

    def deletePlatform(Map args) { required(args, ['app', 'platform'])
        httpRequest(method: 'DELETE',
                    path: "/rest/applications/${args.app}/platforms/${args.platform}"
        )
    }

    def setPlatformVersion(Map args) { required(args, ['app', 'platform', 'newVersion']) // optional: checkCurrentVersion
        def platformInfo = getPlatformInfo(args)
        if (args.checkCurrentVersion && platformInfo.application_version != args.checkCurrentVersion) {
            throw new ExpectedEnvironmentException("Actual app version ${platformInfo.application_version} does not match expect version $args.currentVersion")
        }
        platformInfo.application_version = args.newVersion
        updatePlatform(platformInfo: platformInfo, copyPropertiesForUpgradedModules: true)
    }



    /******************************************************************************

                                        MODULES

    ******************************************************************************/

    def createModule(Map args) { required(args, ['moduleName', 'version']) // optional: technos, fromModule
        def technos = args.technos ?: []
        def payload = [name: args.moduleName, version: args.version, working_copy: true, version_id: -1, technos: technos]
        def query = null
        if (args.fromModule) {
            args.fromModule.isWorkingcopy = args.fromModule.isWorkingcopy ?: false
            query = [from_module_name: args.fromModule.name, from_module_version: args.fromModule.version, from_is_working_copy: args.fromModule.isWorkingcopy]
        }
        httpRequest(method: 'POST',
                    path: '/rest/modules',
                    query: query,
                    body: toJson(payload))
    }

    def getModule(Map args) { required(args, ['moduleName', 'version', 'moduleType'])
        if (!['release', 'workingcopy'].contains(args.moduleType)) {
            throw new IllegalArgumentException("Invalid moduleType $args.moduleType")
        }
        httpRequest(path: "/rest/modules/${args.moduleName}/${args.version}/${args.moduleType}")
    }

    def releaseModule(Map args) { required(args, ['moduleName', 'workingcopyVersion', 'releaseVersion'])
        httpRequest(method: 'POST',
                    path: '/rest/modules/create_release',
                    query: [module_name: args.moduleName, module_version: args.workingcopyVersion, release_version: args.releaseVersion])
    }

    def deleteModule(Map args) { required(args, ['moduleName', 'version', 'moduleType'])
        if (!['release', 'workingcopy'].contains(args.moduleType)) {
            throw new IllegalArgumentException("Invalid moduleType $args.moduleType")
        }
        httpRequest(method: 'DELETE',
                    path: "/rest/modules/${args.moduleName}/${args.version}/${args.moduleType}")
    }

    def putModuleOnPlatform(Map args) { required(args, ['app', 'platform', 'moduleName', 'moduleVersion', 'isWorkingCopy', 'logicGroupPath'])
        def platformInfo = getPlatformInfo(args)
        def modulePropertiesPath = "#${args.logicGroupPath}#${args.moduleName}#${args.moduleVersion}#${args.isWorkingCopy ? 'WORKINGCOPY' : 'RELEASE'}"
        platformInfo.modules << [
            name: args.moduleName,
            version: args.moduleVersion,
            working_copy: args.isWorkingCopy,
            path: args.logicGroupPath,
            instances: [],
            id: (maxModulesIds(platformInfo.modules) ?: 0) + 1,
            properties_path: modulePropertiesPath
        ]
        updatePlatform(platformInfo: platformInfo)
    }

    @NonCPS
    private maxModulesIds(modules) {
        modules.collect { it.id.toInteger() }.max()
    }

    def setPlatformModuleVersion(Map args) { required(args, ['app', 'platform', 'moduleName', 'newVersion']) // optional: checkCurrentVersion, isWorkingcopy
        def platformInfo = getPlatformInfo(args)
        def module = selectModule(modules: platformInfo.modules, moduleName: args.moduleName)
        if (args.checkCurrentVersion) {
            if (module.version != args.checkCurrentVersion) {
                throw new ExpectedEnvironmentException("Actual module $module.name version ${module.version} does not match expect version $args.currentVersion")
            }
        }
        module.version = args.newVersion
        if (args.isWorkingcopy != null) {
            module.working_copy = args.isWorkingcopy
        }
        updatePlatform(platformInfo: platformInfo, copyPropertiesForUpgradedModules: true)
    }

    def setPlatformModulesVersion(Map args) { required(args, ['app', 'platform', 'newVersion']) // optional: checkCurrentVersion, isWorkingcopy
        def platformInfo = getPlatformInfo(args)
        if (args.checkCurrentVersion) {
            for (def i = 0; i < platformInfo.modules.size(); i++) {
                def module = platformInfo.modules[i]
                if (module.version != args.checkCurrentVersion) {
                    throw new ExpectedEnvironmentException("Actual module $module.name version ${module.version} does not match expect version $args.currentVersion")
                }
            }
        }
        for (def i = 0; i < platformInfo.modules.size(); i++) {
            platformInfo.modules[i].version = args.newVersion
            if (args.isWorkingcopy != null) {
                platformInfo.modules[i].working_copy = args.isWorkingcopy
            }
        }
        updatePlatform(platformInfo: platformInfo, copyPropertiesForUpgradedModules: true)
    }

    def doesWorkingcopyExistForModuleVersion(Map args) { required(args, ['moduleName', 'version'])
        try {
            httpRequest(path: "/rest/modules/${args.moduleName}/${args.version}/workingcopy")
            true
        } catch (HttpException httpException) {
            if (httpException.statusCode != 404) {
                throw ex
            }
            false
        }
    }

    def doesReleaseExistForModuleVersion(Map args) { required(args, ['moduleName', 'version'])
        try {
            httpRequest(path: "/rest/modules/${args.moduleName}/${args.version}/release")
            true
        } catch (HttpException httpException) {
            if (httpException.statusCode != 404) {
                throw ex
            }
            false
        }
    }



    /******************************************************************************

                                TEMPLATES & PROPERTIES

    ******************************************************************************/

    def createTemplate(Map args) { required(args, ['moduleName', 'moduleVersion', 'location', 'filename', 'content']) // optional: title
        def title = args.title ?: args.filename
        def payload = [
            name: title,
            filename: args.filename,
            location: args.location,
            content: args.content,
            version_id: -1,
            rights: [
                user: [:],
                group: [:]
            ]
        ]
        httpRequest(method: 'POST',
                    path: "/rest/modules/${args.moduleName}/${args.moduleVersion}/workingcopy/templates",
                    body: toJson(payload))
    }

    def getModuleTemplateProperties(Map args) { required(args, ['moduleName', 'version']) // optional: isRelease
        def releasePath = args.isRelease ? 'release' : 'workingcopy'
        httpRequest(path: "/rest/modules/${args.moduleName}/${args.version}/${releasePath}/model")
    }

    def getModulePropertiesForPlatform(Map args) { required(args, ['app', 'platform', 'modulePropertiesPath'])
        def moduleProperties = httpRequest(path: "/rest/applications/${args.app}/platforms/${args.platform}/properties", query: [path: args.modulePropertiesPath])
        // empty lists from JSON data are immutable by defaut (adding new entries is going to be refused as unsupported operation)
        // -> we change it into a dynamic list
        if (moduleProperties.key_value_properties.empty) {
            moduleProperties.key_value_properties = []
        }
        if (moduleProperties.iterable_properties.empty) {
            moduleProperties.iterable_properties = []
        }
        moduleProperties
    }

    def updateProperties(Map args) { required(args, ['app', 'platform', 'jsonPropertyUpdates', 'commitMsg'])
        def platformInfo = getPlatformInfo(args)

        def propertyUpdates = args.jsonPropertyUpdates
        if (propertyUpdates instanceof CharSequence) {
            if (propertyUpdates.startsWith('http')) {
                propertyUpdates = httpRequest(url: propertyUpdates)
            } else {
                propertyUpdates = propertiesFromJsonFile(propertyUpdates)
            }
        }

        def moduleNames = propertyUpdates.keySet() as List
        for (def i = 0; i < moduleNames.size(); i++) {  // DAMN Jenkins pipelines that does not support .each
            def moduleName = moduleNames[i]
            def modulePropertyChanges = propertyUpdates[moduleName]
            log "---------\n# Module: $moduleName\n---------"
            log "---------${platformInfo.modules}---------"
            if (moduleName.startsWith("path:")) {
                // changes to apply on a module to a specific path on all instances
                def moduleNameFromPath = moduleName.split("#").last()
                def path = moduleName.minus("path:").minus("#"+moduleNameFromPath)
                def moduleFoundFromPath = selectModule(modules: platformInfo.modules, path: path, moduleName: moduleNameFromPath)
                log "-> properties_path: $moduleFoundFromPath.properties_path"
                for (def y = 0; y < moduleFoundFromPath.instances.size(); y++){
                    def instance = moduleFoundFromPath.instances[y].name
                    def instanceInfo = extractInstanceInfo(module: moduleFoundFromPath,
                                                           instance: instance)
                    applyChanges(modulePropertyChanges, instanceInfo.key_values, "[instance=$instance] ")
                }
                updatePlatform(platformInfo: platformInfo)
            } else if (moduleName.contains('#')) { // il s'agit de properties d'instance
                def splittedMod = moduleName.split('#')  // DAMN Jenkins pipelines that does not support tuples
                moduleName = splittedMod[0]
                def instance = splittedMod[1]
                def instanceInfo = extractInstanceInfo(modules: platformInfo.modules,
                                                       moduleName: moduleName,
                                                       instance: instance)
                applyChanges(modulePropertyChanges, instanceInfo.key_values, "[instance=$instance] ")
                updatePlatform(platformInfo: platformInfo)
            } else {  // il s'agit de properties globales ou de modules
                def modulePropertiesPath = ['#']
                if (moduleName != 'GLOBAL') {
                    def modules = selectModules(args)
                    for (int i = 0; i < modules.size(); i++){
                        modulePropertiesPath << modules[i].properties_path
                    }
                }
                for (int p = 0; p < modulePropertiesPath.size(); p++){
                    log("-> properties_path: "+modulePropertiesPath[p])
                    def modulePlatformProperties = getModulePropertiesForPlatform(app: args.app,
                                                                                  platform: args.platform,
                                                                                  modulePropertiesPath: modulePropertiesPath[p])
                    if (modulePlatformProperties.key_value_properties == null) {
                        modulePlatformProperties.key_value_properties = []
                    }
                    def newIterableProperties = modulePropertyChanges.remove('iterable_properties')
                    applyChanges(modulePropertyChanges, modulePlatformProperties.key_value_properties)

                    // For iterable properties, we do not "apply" changes, we simply use the values provided
                    def iterableProperties = []
                    def iterableNames = newIterableProperties ? newIterableProperties.keySet() as List : []
                    for (def j = 0; j < iterableNames.size(); j++) {  // DAMN Jenkins pipelines that does not support .each
                        def iterableName = iterableNames[j]
                        def newIterableItemProperties = newIterableProperties[iterableName]
                        def actualIterableProperties = listSelect(list: modulePlatformProperties.iterable_properties,
                                                                  key: 'name',
                                                                  value: iterableName)
                        if (actualIterableProperties && newIterableItemProperties.size() < actualIterableProperties.iterable_valorisation_items.size()) {
                            def diffSize = actualIterableProperties.iterable_valorisation_items.size() - newIterableItemProperties.size()
                            log COLOR_RED + "$diffSize iterable properties where DELETED for iterable $iterableName" + COLOR_END
                        }
                        def iterableValorisationItems = []
                        for (def k = 0; k < newIterableItemProperties.size(); k++) { // DAMN Jenkins pipelines that does not support .eachWithIndex
                            iterableValorisationItems << [title: 'not used', values: map2list(newIterableItemProperties[k], 'name', 'value')]
                            // Displaying props changes:
                            if (!actualIterableProperties) {
                                continue
                            }
                            def actualIterableItemProperties = actualIterableProperties.iterable_valorisation_items[k]
                            if (!actualIterableItemProperties) {
                                continue
                            }
                            displayChanges(list2map(actualIterableItemProperties.values, 'name', 'value'),
                                           newIterableItemProperties[k],
                                           "[iterable=$iterableName.$k] ")
                        }
                        iterableProperties << [name: iterableName, iterable_valorisation_items: iterableValorisationItems]
                    }
                    modulePlatformProperties.iterable_properties = iterableProperties

                    setPlatformProperties(platformInfo: platformInfo,
                                          modulePropertiesPath: modulePropertiesPath[p],
                                          properties: modulePlatformProperties,
                                          commitMsg: args.commitMsg)
                }
            }
        }
    }

    private applyChanges(changes, properties, logPrefix = '') {
        def propNames = changes.keySet() as List
        for (def j = 0; j < propNames.size(); j++) {  // DAMN Jenkins pipelines that does not support .each
            def propName = propNames[j]
            def newValue = changes[propName]
            def prop = listSelect(list: properties, key: 'name', value: propName)
            if (prop) {
                if ("${prop.value}" != "$newValue") { // converting to string before comparing
                    log COLOR_GREEN + "${logPrefix}UPDATED property $propName: ${prop.value} -> $newValue" + COLOR_END
                    prop.value = newValue
                }
            } else if (newValue != '') {
                log COLOR_GREEN + "${logPrefix}NEW property $propName: $newValue" + COLOR_END
                properties << [name: propName, value: newValue]
            }
        }
    }

    private displayChanges(oldProps, newProps, logPrefix = '') {
        def propNames = oldProps.keySet() + newProps.keySet()
        for (def j = 0; j < propNames.size(); j++) {  // DAMN Jenkins pipelines that does not support .each
            def propName = propNames[j]
            def newValue = newProps[propName]
            def oldValue = oldProps[propName]
            if ("$newValue" == "$oldValue") { // converting to string before comparing
                continue
            }
            if (newValue && !oldValue) {
                log COLOR_GREEN + "${logPrefix}NEW property $propName: $newValue" + COLOR_END
            } else if (!newValue && oldValue) {
                log COLOR_GREEN + "${logPrefix}DELETED property $propName: $oldValue" + COLOR_END
            } else {
                log COLOR_GREEN + "${logPrefix}UPDATED property $propName: $oldValue -> $newValue" + COLOR_END
            }
        }
    }

    private propertiesFromJsonFile(jsonFilePath) {
        def fileContent = steps ? steps.readFile(jsonFilePath) : new File(jsonFilePath).text
        new JsonSlurperClassic().parseText(fileContent)
    }

    @NonCPS
    private extractInstanceInfo(Map args) { required(args, ['instance']) // + required: module OR modules & moduleName
        def instanceInfo
        if (args.module) {
            instanceInfo = args.module.instances.find { it.name == args.instance }
            if (!instanceInfo) {
                throw new ExpectedEnvironmentException("No instance ${args.instance} found, in module named ${args.module.name}")
            }
        } else {
            def module = selectModule(args)
            if (!module) {
                throw new ExpectedEnvironmentException("No instance ${args.instance} found, in module named ${args.moduleName}")
            }
            instanceInfo = module.instances.find { it.name == args.instance }
        }
        if (instanceInfo.key_values.empty) {
            // empty lists from JSON data are immutable by defaut (adding new entries is going to be refused as unsupported operation)
            // -> we change it into a dynamic list
            instanceInfo.key_values = []
        }
        instanceInfo
    }

    private setPlatformProperties(Map args) { required(args, ['platformInfo', 'modulePropertiesPath', 'commitMsg', 'properties'])
        def app = args.platformInfo.application_name
        def platform = args.platformInfo.platform_name
        httpRequest(method: 'POST',
                    path: "/rest/applications/$app/platforms/$platform/properties",
                    query: [path: args.modulePropertiesPath, comment: args.commitMsg, platform_vid: args.platformInfo.version_id],
                    body: toJson(args.properties))
        args.platformInfo.version_id++
    }




    /******************************************************************************

                                    INSTANCES

    ******************************************************************************/

    def getInstanceProperties(Map args) { required(args, ['app', 'platform', 'instance'])
        def platformInfo = getPlatformInfo(args)
        def module = selectModule(modules: platformInfo.modules, moduleName: args.moduleName, path: args.path)
        listSelect(list: module.instances, key: 'name', value: args.instance)
    }

    def createInstance(Map args) { required(args, ['app', 'platform', 'moduleName', 'instance']) // optional: path
        def platformInfo = getPlatformInfo(args)
        def module = selectModule(modules: platformInfo.modules, moduleName: args.moduleName, path: args.path)
        module.instances.add([name: args.instance, key_values: []])
        updatePlatform(platformInfo: platformInfo)
    }

    def deleteInstance(Map args) { required(args, ['app', 'platform', 'moduleName', 'instance'])
        def platformInfo = getPlatformInfo(args)
        def modules = selectModules(modules: platformInfo.modules, moduleName: args.moduleName)
        for (def i = 0; i < modules.size(); i++) {
            def module = modules[i]
            if (args.instance == '*') {
                module.instances = []
            } else {
                // Note: there may be ZERO matching instance in this module
                // In that case, this simply won't do anything
                listRemove(list: module.instances, key: 'name', value: args.instance)
            }
        }
        updatePlatform(platformInfo: platformInfo)
    }

    def getInstanceFiles(Map args) { required(args, ['app', 'platform', 'moduleName', 'instance'])
        def platformInfo = getPlatformInfo(args)
        def module = selectModule(modules: platformInfo.modules, moduleName: args.moduleName)
        def instanceFiles = httpRequest(path: "/rest/files/applications/${args.app}/platforms/${args.platform}/${module.path}/${args.moduleName}/${module.version}/instances/${args.instance}/",
                                        query: [isWorkingCopy: module.working_copy])
        def instanceFilesContents = [:]
        for (templateFile in instanceFiles) {
            instanceFilesContents[templateFile.location] = httpRequest(url: "$apiRootUrl$templateFile.url", contentType: 'TEXT', accept: 'TEXT')
        }
        instanceFilesContents
    }



    /******************************************************************************

                                    CORE UTILS

    ******************************************************************************/

    protected log(msg) {
        if (steps) {
            steps.echo msg
        } else {
            System.err.println msg
        }
    }

    protected httpRequest(Map args) { // optional: query, method, body, authHeader, contentType, accept
        if (!args.uri) {
            if (args.url) {
                args.uri = new URI(args.url)
            } else if (args.path) {
                args.uri = buildUri(args.path, args.query)
            } else {
                throw new IllegalArgumentException("Either an 'URI uri' or 'String url' or 'String path' parameter is required")
            }
        }
        args.method = args.method ?: 'GET'
        log "$args.method $args.uri"
        if (args.body) {
            log prettyPrint(args.body)
        }
        log "Content-Type: $args.contentType"
        args.accept = args.accept ?: (args.method == 'DELETE' ? 'ANY' : 'JSON')
        log "Accept: $args.accept"
        if (!args.authHeader) {
            args.authHeader = authHeader
        }
        httpRequester.performRequest(args)
    }

    @NonCPS
    private buildUri(path, query = null) {
        def uriBuilder = new SerializableURIBuilder(apiRootUrl)
        uriBuilder.path = path
        if (query) {
            query.each { k, v ->
                if (v != null && v.toString() != '') {
                    uriBuilder.addParameter(k, v.toString())
                }
            }
        }
        uriBuilder.build()
    }

    protected updatePlatform(Map args) { required(args, ['platformInfo']) // optional: copyPropertiesForUpgradedModules
        def copyPropertiesForUpgradedModules = args.copyPropertiesForUpgradedModules ?: true
        httpRequest(method: 'PUT',
                    path: "/rest/applications/${args.platformInfo.application_name}/platforms",
                    query: [application_name: args.platformInfo.application_name, copyPropertiesForUpgradedModules: copyPropertiesForUpgradedModules],
                    body: toJson(args.platformInfo))
        args.platformInfo.version_id++
    }

    protected selectModules(Map args) { required(args, ['modules', 'moduleName']) // optional: path
        def matchingModules = listSelectAll(list: args.modules, key: 'name', value: args.moduleName)
        if (!matchingModules) {
            throw new ExpectedEnvironmentException("No module found in platform for name ${args.moduleName}")
        }
        if (args.path){
            matchingModules = listSelectAll(list: matchingModules, key: 'path', value: args.path)
            if (!matchingModules) {
                throw new ExpectedEnvironmentException("No module found in platform for properties_path ${args.path}")
            }
        }
        matchingModules
    }

    protected selectModule(Map args) { required(args, ['modules', 'moduleName']) // optional: instance, path
        def matchingModules = selectModules(args)
        if (args.instance) {
            def filteredModules = []
            for (def i = 0; i < matchingModules.size(); i++) {
                def module = matchingModules[i]
                if (listSelect(list: module.instances, key: 'name', value: args.instance)) {
                    filteredModules << module
                }
            }
            matchingModules = filteredModules
        }
        if (matchingModules.size() > 1) {
            def modulesString = modulesStringDescription(matchingModules)
            throw new ExpectedEnvironmentException("Multiple matching modules found in platform for name ${args.moduleName}" + (args.path ? "and properties_path ${args.path}" : '') + " : ${modulesString}")
        }
        matchingModules[0]
    }


    @NonCPS
    protected modulesStringDescription(List modules) {
        modules.inject([]) { moduleDescs, module -> moduleDescs << "${module.name}#${module.path}" }.join(', ')
    }

    @NonCPS
    protected listSelect(Map args) { required(args, ['list', 'key', 'value'])
        def matches = args.list.findAll { it && it[args.key] == args.value }
        if (!matches) {
            return null
        }
        if (matches.size() > 1) {
            throw new RuntimeException("Multiple matches found in list for key ${args.key} and value ${args.value}")
        }
        matches[0]
    }

    @NonCPS
    protected listSelectAll(Map args) { required(args, ['list', 'key', 'value'])
        args.list.findAll { it && it[args.key] == args.value }
    }

    @NonCPS
    protected listRemove(Map args) { required(args, ['list', 'key', 'value'])
        args.list.removeAll { it && it[args.key] == args.value }
    }

    @NonCPS
    protected list2map(List list, String keyProp, String valueProp) {
        list.collectEntries { entry -> [entry[keyProp], entry[valueProp]] }
    }

    @NonCPS
    protected map2list(Map map, String keyProp, String valueProp) {
        map.collect { key, value -> [(keyProp): key, (valueProp): value] }
    }

    @NonCPS
    protected required(Map args, List requiredArgs) {
        requiredArgs.each {
            if (!args.containsKey(it)) {
                throw new IllegalArgumentException("'$it' parameter is required")
            }
        }
    }
}
