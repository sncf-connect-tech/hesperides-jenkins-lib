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
        this.apiRootUrl = args.apiRootUrl ?: defaultApiRootUrl()
        if (this.apiRootUrl[-1] == '/') {
            this.apiRootUrl = this.apiRootUrl[0..-2]
        }
        this.steps = args.steps
    }

    @NonCPS
    private static defaultApiRootUrl() {
        def hostname = 'hesperides'
        try { // we try to build a FQDN based on the Jenkins executor own domain name
            def fqdn = InetAddress.getByName(hostname).canonicalHostName
            hostname += '.' + fqdn.split('\\.', 2)[1]
        } catch (UnknownHostException|SecurityException exception) {} // do nothing
        return "https://${hostname}"
    }


    /******************************************************************************

     CONSUME FILE DESCRIPTION AND UDPATE HESPERIDES

     ******************************************************************************/

    def upsertFromDescriptor(Map args) { required(args, ['descriptorPath', 'moduleVersion'])
        def descriptorContent = propertiesFromJsonFile(args.descriptorPath)
        descriptorContent.each { moduleName, moduleValue ->
            if (!doesWorkingcopyExistForModuleVersion(moduleName: moduleName, version: args.moduleVersion)) {
                createModule(moduleName: moduleName, version: args.moduleVersion)
            }
            // On supprime tous les templates au préalable :
            getTemplates(moduleName: moduleName, moduleVersion: args.moduleVersion, moduleType: 'workingcopy').each {
                it -> deleteTemplate(moduleName: moduleName, moduleVersion: args.moduleVersion, templateName: it.name)
            }
            moduleValue.each { templatePath, templateDefinition ->
                def title = templateDefinition.title ?: templateDefinition.filename
                createTemplate(
                        moduleName: moduleName,
                        moduleVersion: args.moduleVersion,
                        location: templateDefinition.location,
                        filename: templateDefinition.filename,
                        content: steps ? steps.readFile(templatePath) : new File(templatePath).text,
                        title: title,
                        filePerms: templateDefinition.filePerms)
            }
        }
    }


    /******************************************************************************

     APPLICATIONS & PLATFORMS

     ******************************************************************************/

    def getAppInfo(Map args) { required(args, ['app'])
        httpRequest(path: "/rest/applications/${args.app}")
    }

    def getPlatformInfo(Map args) { required(args, ['app', 'platform'])
        def response = httpRequest(path: "/rest/applications/${args.app}/platforms/${args.platform}")
        log('Platform version_id = ' + response.version_id)
        return response
    }

    def createPlatform(Map args) { required(args, ['app', 'platform', 'version']) // optional: modules, isProduction, fromApplication, fromPlatform, copyPropertiesForUpgradedModules
        def copyPropertiesForUpgradedModules = args.copyPropertiesForUpgradedModules != null ? args.copyPropertiesForUpgradedModules : true
        def platformInfo = [
                application_name: args.app,
                platform_name: args.platform,
                application_version: args.version,
                modules: args.modules ?: [],
                production: args.isProduction ?: false,
                version_id: 0,
        ]
        def response = httpRequest(method: 'POST',
                path: '/rest/applications',
                query: [
                        application_name : args.app,
                        from_application: args.fromApplication,
                        from_platform: args.fromPlatform,
                        copyPropertiesForUpgradedModules: copyPropertiesForUpgradedModules
                ],
                body: toJson(platformInfo))
        log('Platform created, version_id = ' + response.version_id)
        return response
    }

    def deletePlatform(Map args) { required(args, ['app', 'platform'])
        httpRequest(method: 'DELETE', path: "/rest/applications/${args.app}/platforms/${args.platform}")
    }

    def setPlatformVersion(Map args) { required(args, ['app', 'platform', 'newVersion']) // optional: checkCurrentVersion, copyPropertiesForUpgradedModules
        def copyPropertiesForUpgradedModules = args.copyPropertiesForUpgradedModules != null ? args.copyPropertiesForUpgradedModules : true
        def platformInfo = getPlatformInfo(args)
        if (args.checkCurrentVersion && platformInfo.application_version != args.checkCurrentVersion) {
            throw new ExpectedEnvironmentException("Actual app version ${platformInfo.application_version} does not match expect version $args.currentVersion")
        }
        platformInfo.application_version = args.newVersion
        updatePlatform(platformInfo: platformInfo, copyPropertiesForUpgradedModules: copyPropertiesForUpgradedModules)
    }

    def cleanUnusedProperties(Map args) { required(args, ['app', 'platform']) //optional: propertiesPath
        httpRequest(method: 'DELETE',
                    path: "/rest/applications/${args.app}/platforms/${args.platform}/properties/clean_unused_properties",
                    query: [properties_path: args.propertiesPath])
    }

    /******************************************************************************

     MODULES

     ******************************************************************************/

    def createModule(Map args) { required(args, ['moduleName', 'version']) // optional: technos, fromModule
        def technos = args.technos ?: []
        def payload = [name: args.moduleName, version: args.version, working_copy: true, version_id: -1, technos: technos]
        def query = null
        if (args.fromModule) {
            args.fromModule.isWorkingCopy = args.fromModule.isWorkingCopy ?: args.fromModule.isWorkingcopy ?: false
            query = [from_module_name: args.fromModule.name, from_module_version: args.fromModule.version, from_is_working_copy: args.fromModule.isWorkingCopy]
        }
        def response = httpRequest(method: 'POST',
                path: '/rest/modules',
                query: query,
                body: toJson(payload))
        log('Module created, version_id = ' + response.version_id)
        return response
    }

    def getModule(Map args) { required(args, ['moduleName', 'version', 'moduleType'])
        if (!['release', 'workingcopy'].contains(args.moduleType)) {
            throw new IllegalArgumentException("Invalid moduleType $args.moduleType")
        }
        def response = httpRequest(path: "/rest/modules/${args.moduleName}/${args.version}/${args.moduleType}")
        log('Module version_id = ' + response.version_id)
        return response
    }

    def getModuleVersions(Map args) { required(args, ['moduleName'])
        httpRequest(path: "/rest/modules/${args.moduleName}")
    }

    def releaseModule(Map args) { required(args, ['moduleName', 'workingcopyVersion', 'releaseVersion'])
        def response = httpRequest(method: 'POST',
                path: '/rest/modules/create_release',
                query: [module_name: args.moduleName, module_version: args.workingcopyVersion, release_version: args.releaseVersion])
        log('Module released, version_id = ' + response.version_id)
        return response
    }

    def deleteModule(Map args) { required(args, ['moduleName', 'version', 'moduleType'])
        if (!['release', 'workingcopy'].contains(args.moduleType)) {
            throw new IllegalArgumentException("Invalid moduleType $args.moduleType")
        }
        httpRequest(method: 'DELETE', path: "/rest/modules/${args.moduleName}/${args.version}/${args.moduleType}")
    }

    def putModuleOnPlatform(Map args) { required(args, ['app', 'platform', 'moduleName', 'moduleVersion', 'isWorkingCopy', 'logicGroupPath'])
        def platformInfo = getPlatformInfo(args)
        def modulePropertiesPath = "#${args.logicGroupPath.replaceFirst('^#', '')}#${args.moduleName}#${args.moduleVersion}#${args.isWorkingCopy ? 'WORKINGCOPY' : 'RELEASE'}"

        platformInfo.modules << [
                name: args.moduleName,
                version: args.moduleVersion,
                working_copy: args.isWorkingCopy,
                path: args.logicGroupPath,
                instances: [],
                id: (maxModulesIds(platformInfo.modules) ?: 0) + 1,
                properties_path: modulePropertiesPath,
        ]
        updatePlatform(platformInfo: platformInfo)
    }

    @NonCPS
    private maxModulesIds(List modules) {
        modules.collect { it.id.toInteger() }.max()
    }

    def setPlatformModuleVersion(Map args) { required(args, ['app', 'platform', 'moduleName', 'newVersion']) // optional: checkCurrentVersion, isWorkingCopy, path, copyPropertiesForUpgradedModules
        def copyPropertiesForUpgradedModules = args.copyPropertiesForUpgradedModules != null ? args.copyPropertiesForUpgradedModules : true
        def platformInfo = getPlatformInfo(args)
        def modules = selectModules(modules: platformInfo.modules, moduleName: args.moduleName, path: args.path)
        for (module in modules) {
            if (args.checkCurrentVersion) {
                if (module.version != args.checkCurrentVersion) {
                    throw new ExpectedEnvironmentException("Actual module $module.name version ${module.version} does not match expect version $args.currentVersion")
                }
            }
            module.version = args.newVersion
            if (args.isWorkingCopy != null || args.isWorkingcopy != null) {
                module.working_copy = args.isWorkingCopy ?: args.isWorkingcopy
            }
            updatePlatform(platformInfo: platformInfo, copyPropertiesForUpgradedModules: copyPropertiesForUpgradedModules)
        }
    }

    def setPlatformModulesVersion(Map args) { required(args, ['app', 'platform', 'newVersion']) // optional: checkCurrentVersion, isWorkingCopy, path, copyPropertiesForUpgradedModules
        def copyPropertiesForUpgradedModules = args.copyPropertiesForUpgradedModules != null ? args.copyPropertiesForUpgradedModules : true
        def platformInfo = getPlatformInfo(args)
        if (args.checkCurrentVersion) {
            for (int i = 0; i < platformInfo.modules.size(); i++) {
                def module = platformInfo.modules[i]
                if (module.version != args.checkCurrentVersion) {
                    throw new ExpectedEnvironmentException("Actual module $module.name version ${module.version} does not match expect version $args.currentVersion")
                }
            }
        }
        for (int i = 0; i < platformInfo.modules.size(); i++) {
            platformInfo.modules[i].version = args.newVersion
            if (args.isWorkingCopy != null || args.isWorkingcopy != null) {
                platformInfo.modules[i].working_copy = args.isWorkingCopy ?: args.isWorkingcopy
            }
        }
        updatePlatform(platformInfo: platformInfo, copyPropertiesForUpgradedModules: copyPropertiesForUpgradedModules)
    }

    def doesWorkingcopyExistForModuleVersion(Map args) { required(args, ['moduleName', 'version'])
        args.moduleType = 'workingcopy'
        return doesModuleExist(args)
    }

    def doesReleaseExistForModuleVersion(Map args) { required(args, ['moduleName', 'version'])
        args.moduleType = 'release'
        return doesModuleExist(args)
    }

    def doesModuleExist(Map args) { required(args, ['moduleName', 'version', 'moduleType'])
        try {
            httpRequest(path: "/rest/modules/${args.moduleName}/${args.version}/${args.moduleType}")
            return true
        } catch (HttpException httpException) {
            if (httpException.statusCode != 404) {
                throw httpException
            }
            return false
        }
    }

    /******************************************************************************

     TEMPLATES & PROPERTIES

     ******************************************************************************/

    def createTemplate(Map args) { required(args, ['moduleName', 'moduleVersion', 'location', 'filename', 'content']) // optional: title, filePerms
        def title = args.title ?: args.filename
        def filePerms = args.filePerms ?: [:]
        def payload = [
                name: title,
                filename: args.filename,
                location: args.location,
                content: args.content,
                version_id: -1,
                rights: filePerms,
        ]
        def response = httpRequest(method: 'POST',
                path: "/rest/modules/${args.moduleName}/${args.moduleVersion}/workingcopy/templates",
                body: toJson(payload))
        log('Template created, version_id = ' + response.version_id)
        return response
    }

    def updateTemplate(Map args) { required(args, ['moduleName', 'moduleVersion', 'location', 'filename', 'version_id', 'content']) // optional: title, filePerms
        def title = args.title ?: args.filename
        def filePerms = args.filePerms ?: [:]
        def payload = [
                name: title,
                filename: args.filename,
                location: args.location,
                content: args.content,
                version_id: args.version_id,
                rights: filePerms,
        ]
        log('Template version_id = ' + payload.version_id)
        def response = httpRequest(method: 'PUT',
                path: "/rest/modules/${args.moduleName}/${args.moduleVersion}/workingcopy/templates/",
                body: toJson(payload))
        log('Template updated, version_id = ' + response.version_id)
        return response
    }

    def deleteTemplate(Map args) { required(args, ['moduleName', 'moduleVersion', 'templateName'])
        return httpRequest(method: 'DELETE', path: "/rest/modules/${args.moduleName}/${args.moduleVersion}/workingcopy/templates/${args.templateName}")
    }

    def getTemplates(Map args) { required(args, ['moduleName', 'moduleVersion', 'moduleType'])
        return httpRequest(method: 'GET', path: "/rest/modules/${args.moduleName}/${args.moduleVersion}/${args.moduleType}/templates")
    }

    def getTemplate(Map args) { required(args, ['moduleName', 'moduleVersion', 'moduleType', 'filename']) // optional: title
        def title = args.title ?: args.filename
        def response = httpRequest(method: 'GET',
                path: "/rest/modules/${args.moduleName}/${args.moduleVersion}/${args.moduleType}/templates/${title}")
        log('Template version_id = ' + response.version_id)
        return response
    }

    def upsertTemplate(Map args) { required(args, ['moduleName', 'moduleVersion', 'location', 'filename', 'content']) // optional: title, filePerms
        args.moduleType = 'workingcopy'
        try {
            args.version_id = getTemplate(args).version_id
            updateTemplate(args)
        } catch (HttpException httpException) {
            if (httpException.statusCode != 404) {
                throw httpException
            }
            createTemplate(args)
        }
    }

    def getModuleTemplateProperties(Map args) { required(args, ['moduleName', 'version']) // optional: moduleType
        if (args.isRelease) {
            logWarn 'Argument isRelease is deprecated, use moduleType instead'
        }
        def releasePath = (args.isRelease || args.moduleType) ? 'release' : 'workingcopy'
        httpRequest(path: "/rest/modules/${args.moduleName}/${args.version}/${releasePath}/model")
    }

    def getModulePropertiesForPlatform(Map args) { required(args, ['app', 'platform', 'modulePropertiesPath'])
        def moduleProperties = httpRequest(path: "/rest/applications/${args.app}/platforms/${args.platform}/properties", query: [path: args.modulePropertiesPath])
        // empty lists from JSON data are immutable by defaut (adding new entries is going to be refused as unsupported operation)
        // -> we change it into a dynamic list
        if (moduleProperties.key_value_properties.isEmpty()) {
            moduleProperties.key_value_properties = []
        }
        if (moduleProperties.iterable_properties.isEmpty()) {
            moduleProperties.iterable_properties = []
        }
        return moduleProperties
    }

    def updatePropertiesForPlatform(Map args) { required(args, ['app', 'platform', 'modulePropertiesPath', 'commitMsg', 'platformVid', 'properties'])
        setPlatformProperties(platformInfo: [application_name: args.app, platform_name: args.platform, version_id: args.platformVid], modulePropertiesPath: args.modulePropertiesPath, commitMsg: args.commitMsg, properties: args.properties)
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
        // Split into the 3 types of properties we can have (normal and global, on specific path, on specific instance)
        moduleNames.findAll { it != 'GLOBAL' && !it.startsWith('path:') && it.contains('#') }.each {
            def modulePropertyChanges = propertyUpdates[it]
            log('------------ module name for specific instance: ' + it)
            updateInstanceProperties(platformInfo: platformInfo, modulePropertyChanges: modulePropertyChanges, moduleName: it)
        }
        moduleNames.findAll { (!it.startsWith('path:') && !it.contains('#')) }.each {
            def modulePropertyChanges = propertyUpdates[it]
            log('------------ module name : ' + it)
            updateModuleProperties(app: args.app, platform: args.platform, platformInfo: platformInfo, modulePropertyChanges: modulePropertyChanges, moduleName: it, commitMsg: args.commitMsg)
        }
        moduleNames.findAll { it.startsWith('path:') }.each {
            def modulePropertyChanges = propertyUpdates[it]
            log('------------ module name for specific path: ' + it)
            updatePathSpecificProperties(app: args.app, platform: args.platform, platformInfo: platformInfo, modulePropertyChanges: modulePropertyChanges, moduleName: it, commitMsg: args.commitMsg)
        }
        return platformInfo
    }

    // Method to get all the properties diff through Hesperides'API for a platform with itself or 2 platforms
    def getDiffProperties(Map args) { required(args, ['app', 'platform', 'modulePropertiesPath']) // optional: instanceName, toApplication, toPlatform, toModulePropertiesPath, toInstanceName, timestampDate, compareStoredValues
        def timestamp = args.timestampDate
        if (timestamp instanceof Date) { // Flexible: allow for a Date or a long
            timestamp = timestamp.getTime()
        }
        return httpRequest(
            path: "/rest/applications/${args.app}/platforms/${args.platform}/properties/diff",
            query: [
                path: args.modulePropertiesPath,
                instance_name: args.instanceName, // null si non fourni
                to_application: args.toApplication ?: args.app,
                to_platform: args.toPlatform ?: args.platform,
                to_path: args.toModulePropertiesPath ?: args.modulePropertiesPath,
                to_instance_name: args.toInstanceName ?: args.instanceName,
                compare_stored_values: args.compareStoredValues ?: false,
                timestamp: "${timestamp}"
            ])
    }

    // Function to display differences from getDiff() with a diffType (common/differing/left/right)
    // optional: instanceName, toApplication, toPlatform, toModulePropertiesPath, toInstanceName, timestampDate, compareStoredValues
    def getDiffPropertiesAsString(Map args) { required(args, ['app', 'platform', 'modulePropertiesPath', 'diffType'])
        def output = '' // Output variable
        def propDiff = this.getDiffProperties(args) // get the answer back from the getDiffProperties's function call
        // Count the total of item with diff.
        def listSize = propDiff.get(args.diffType).size()
        // Properties list which depend on the "param" value (only_left, only_right, common, differing)
        def propList = propDiff.get(args.diffType).collect { it }
        // LENGTH PROP : Check characters max to pad the column
        def maxPropNameLength = propList.collect { it.name.length() }.max() ?: 0
        def maxLeftFinalValueLength = propList.collect { it.left.finalValue.length() }.max() ?: 0
        def maxRightFinalValueLength = propList.collect { it.right.finalValue.length() }.max() ?: 0

        // DISPLAY : Variable for the column's display
        def title = '     REPORT DIFF PROPERTIES     '
        def colId = '   #   '
        def colProperty = '     PROPERTIES     '
        def colFinalLeftValue = '     FINAL LEFT VALUE     '
        def colFinalRightValue = '     FINAL RIGHT VALUE     '
        def noDifference = '     * * * * *   NO PROPERTIES ARE DIFFERING!   * * * * *     '

        /**********************************************************************
                                                 DISPLAY
        **********************************************************************/
        // Display of the diff. total
        output += '*********************************************************\n'
        output += "      Total of items in the \"${args.diffType}\" section : ${listSize}\n"
        output += '*********************************************************\n'


        // Columns variables
        def maxColumnContentLength = 80 // Max length for the value's column (char)
        def colIdLength = colId.length()
        def colPropertyLength = Math.min(Math.max(colProperty.length(), maxPropNameLength), maxColumnContentLength)
        def colLeftValueLength = Math.min(Math.max(colFinalLeftValue.length(), maxLeftFinalValueLength), maxColumnContentLength)
        def colRightValueLength = Math.min(Math.max(colFinalRightValue.length(), maxRightFinalValueLength), maxColumnContentLength)
        def initialTotalLength = ( colIdLength + colPropertyLength + colLeftValueLength + colRightValueLength )

        // Display variables
        def firstLine = "  ${'='.padRight(initialTotalLength + 9, '=')}"
        def fullTitle = title.center(initialTotalLength + 9)
        def secondLine = "| ${'='.padRight(initialTotalLength + 9, '=')} |"

        // Test if it is an empty array for the column's width
        if ( propList == [] ) {
            def empty = ' '.center(initialTotalLength + 9)
            def noDiff = noDifference.center(initialTotalLength + 9)

            // Empty display
            output += "\n${firstLine}\n"
            output += "| ${fullTitle} |\n"
            output += "${secondLine}\n"
            output += "| ${colId.center(colIdLength)} | ${colProperty.center(colPropertyLength)} | ${colFinalLeftValue.center(colLeftValueLength)} | ${colFinalRightValue.center(colRightValueLength)} |\n"
            output += "${secondLine}\n"
            output += "| ${empty} |\n"
            output += "| ${noDiff} |\n"
            output += "| ${empty} |\n"
            output += "${firstLine}\n"
        } else {
            // Properties display
            def propertyMap = [:] // Associative array
            propList.each { it -> propertyMap[it.name] = [left: it.left, right: it.right] } // Associative array initialization
            def propertiesASC = new ArrayList(propertyMap.keySet())
            propertiesASC.sort() // Items sorted by name

            // DISPLAY PROPERTIES ASC
            output += "\n${firstLine}\n"
            output += "| ${fullTitle} |\n"
            output += "${secondLine}\n"
            output += "| ${colId.center(colIdLength)} | ${colProperty.center(colPropertyLength)} | ${colFinalLeftValue.center(colLeftValueLength)} | ${colFinalRightValue.center(colRightValueLength)} |\n"
            output += "${secondLine}\n"

            propertiesASC.each { propName ->
                def property = propertyMap[propName]
                def leftFinalValue = property.left.finalValue
                def rightFinalValue = property.right.finalValue
                def displayedPropName = propName

                if (propName.length() > maxColumnContentLength) {
                    displayedPropName = propName.take(maxColumnContentLength - 5) + '(...)'
                }

                if (leftFinalValue.length() > maxColumnContentLength) {
                    leftFinalValue = leftFinalValue.take(maxColumnContentLength - 5) + '(...)'
                }

                if (rightFinalValue.length() > maxColumnContentLength) {
                    rightFinalValue = rightFinalValue.take(maxColumnContentLength - 5) + '(...)'
                }

                // Line pattern display
                output += "| ${(propertiesASC.indexOf(displayedPropName) + 1).toString().center(colIdLength)} | ${propName.padRight(colPropertyLength)} | ${leftFinalValue.center(colLeftValueLength)} | ${rightFinalValue.center(colRightValueLength)} |\n"
            }
            output += "${secondLine}\n"
        }
        return output
    }

    private updateModuleProperties(Map args) {
        def modulePropertiesPath = ['#']
        if (args.moduleName != 'GLOBAL') {
            modulePropertiesPath = []
            selectModules(modules: args.platformInfo.modules, moduleName: args.moduleName).each {
                modulePropertiesPath << it.properties_path
            }
        }
        modulePropertiesPath.each {
            log('-> properties_path: ' + it)
            def modulePlatformProperties = getModulePropertiesForPlatform(app: args.app,
                    platform: args.platform,
                    modulePropertiesPath: it)
            if (modulePlatformProperties.key_value_properties == null) {
                modulePlatformProperties.key_value_properties = []
            }

            def newIterableProperties = args.modulePropertyChanges.remove('iterable_properties')
            if (newIterableProperties) {
                handleIterableProperties(newIterableProperties, modulePlatformProperties.iterable_properties)
            }
            applyChanges(args.modulePropertyChanges, modulePlatformProperties.key_value_properties)

            setPlatformProperties(platformInfo: args.platformInfo,
                    modulePropertiesPath: it,
                    properties: modulePlatformProperties,
                    commitMsg: args.commitMsg)
        }
    }

    private updateInstanceProperties(Map args) {
        def splittedMod = args.moduleName.split('#')
        args.moduleName = splittedMod[0]
        def instance = splittedMod[1]
        def instanceInfo = extractInstanceInfo(modules: args.platformInfo.modules,
                moduleName: args.moduleName,
                instance: instance)
        instanceInfo.key_values = instanceInfo.key_values ?: []
        applyChanges(args.modulePropertyChanges, instanceInfo.key_values, "[instance=$instance] ")
        updatePlatform(platformInfo: args.platformInfo)
    }

    private updatePathSpecificProperties(Map args) {
        // changes to apply on a module to a specific path on all instances
        def moduleNameFromPath = args.moduleName.split('#').last()
        def path = args.moduleName - 'path:' - ('#' + moduleNameFromPath)
        def moduleFoundFromPath = selectModule(modules: args.platformInfo.modules, path: path, moduleName: moduleNameFromPath)
        log "-> properties_path: $moduleFoundFromPath.properties_path"
        def modulePlatformProperties = getModulePropertiesForPlatform(app: args.app,
                platform: args.platform,
                modulePropertiesPath: moduleFoundFromPath.properties_path)

        def newIterableProperties = args.modulePropertyChanges.remove('iterable_properties')
        if (newIterableProperties) {
            handleIterableProperties(newIterableProperties, modulePlatformProperties.iterable_properties)
        }
        applyChanges(args.modulePropertyChanges, modulePlatformProperties.key_value_properties)

        setPlatformProperties(platformInfo: args.platformInfo,
                modulePropertiesPath: moduleFoundFromPath.properties_path,
                properties: modulePlatformProperties,
                commitMsg: args.commitMsg)
    }

    private applyChanges(Map changes, List properties, String logPrefix = '') {
        if (properties == null) {
            throw new IllegalArgumentException('NULL properties argument provided')
        }
        def propNames = changes.keySet() as List
        for (int j = 0; j < propNames.size(); j++) {  // DAMN Jenkins pipelines that does not support .each
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

    // Populate the iterableProperties list with the given iterable properties
    private handleIterableProperties(Map newIterableProperties, List iterableProperties) {
        // For iterable properties, we do not "apply" changes, we simply use the values provided
        def iterableNames = newIterableProperties ? newIterableProperties.keySet() as List : []
        iterableNames.each {
            def iterableName = it
            def newIterableItemProperties = newIterableProperties[iterableName]
            def actualIterableProperties = listSelect(list: iterableProperties,
                    key: 'name',
                    value: iterableName)
            if (actualIterableProperties && newIterableItemProperties.size() < actualIterableProperties.iterable_valorisation_items.size()) {
                def diffSize = actualIterableProperties.iterable_valorisation_items.size() - newIterableItemProperties.size()
                log COLOR_RED + "$diffSize iterable properties where DELETED for iterable $iterableName" + COLOR_END
            }
            def iterableValorisationItems = []
            for (int k = 0; k < newIterableItemProperties.size(); k++) { // DAMN Jenkins pipelines that does not support .eachWithIndex
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
            // Removes the initial value of the iterable properties
            iterableProperties.removeAll {it.name == iterableName}
            iterableProperties << [name: iterableName, iterable_valorisation_items: iterableValorisationItems]
        }
    }

    private displayChanges(Map oldProps, Map newProps, String logPrefix = '') {
        def propNames = oldProps.keySet() + newProps.keySet()
        for (int j = 0; j < propNames.size(); j++) {  // DAMN Jenkins pipelines that does not support .each
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

    private propertiesFromJsonFile(String jsonFilePath) {
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
        if (instanceInfo.key_values.isEmpty()) {
            // empty lists from JSON data are immutable by defaut (adding new entries is going to be refused as unsupported operation)
            // -> we change it into a dynamic list
            instanceInfo.key_values = []
        }
        return instanceInfo
    }

    private setPlatformProperties(Map args) { required(args, ['platformInfo', 'modulePropertiesPath', 'commitMsg', 'properties'])
        def app = args.platformInfo.application_name
        def platform = args.platformInfo.platform_name
        httpRequest(method: 'PUT',
                path: "/rest/applications/$app/platforms/$platform/properties",
                query: [path: args.modulePropertiesPath, comment: args.commitMsg, platform_vid: args.platformInfo.version_id],
                body: toJson(args.properties))
        args.platformInfo.version_id++
    }



    /******************************************************************************

     INSTANCES

     ******************************************************************************/

    def getInstanceProperties(Map args) { required(args, ['app', 'platform', 'moduleName', 'instance']) // optional: path
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
        for (int i = 0; i < modules.size(); i++) {
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
        def instanceFiles = httpRequest(path: "/rest/applications/${args.app}/platforms/${args.platform}/${module.path}/${args.moduleName}/${module.version}/instances/${args.instance}/files/",
                query: [isWorkingCopy: module.working_copy, simulate: true])
        def instanceFilesContents = [:]
        for (templateFile in instanceFiles) {
            instanceFilesContents[templateFile.location] = httpRequest(url: "$apiRootUrl$templateFile.url", textOutput: true, accept: 'TEXT')
        }
        return instanceFilesContents
    }



    /******************************************************************************

     CORE UTILS

     ******************************************************************************/

    protected log(String msg) {
        if (steps) {
            steps.echo msg
        } else {
            System.err.println msg
        }
    }

    protected logWarn(String msg) {
        if (steps) {
            if (steps.sh(script:'echo $TERM', returnStdout: true).trim()) { // means ansiColor plugin is enabled
                steps.echo COLOR_RED + msg + COLOR_END
            } else {
                steps.echo msg
            }
            steps.echo msg
        } else {
            System.err.println COLOR_RED + msg + COLOR_END
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
        args.textOutput = args.textOutput ?: (args.method == 'DELETE')
        log "$args.method $args.uri"
        if (args.body) {
            log prettyPrint(args.body)
        }
        log "Content-Type: $args.contentType"
        args.accept = args.accept ?: (args.method == 'DELETE' ? 'ANY' : 'JSON')
        log "Accept: $args.accept"
        args.authHeader = args.authHeader ?: authHeader
        return httpRequester.performRequest(args)
    }

    @NonCPS
    private buildUri(String path, Map query = null) {
        def uriBuilder = new SerializableURIBuilder(apiRootUrl)
        uriBuilder.path = path
        if (query) {
            query.each { k, v ->
                if (v != null && v.toString() != '') {
                    uriBuilder.addParameter(k, v.toString())
                }
            }
        }
        return uriBuilder.build()
    }

    protected updatePlatform(Map args) { required(args, ['platformInfo']) // optional: copyPropertiesForUpgradedModules
        def copyPropertiesForUpgradedModules = args.copyPropertiesForUpgradedModules != null ? args.copyPropertiesForUpgradedModules : true
        log('Platform version_id = ' + args.platformInfo.version_id)
        def response = httpRequest(method: 'PUT',
                path: "/rest/applications/${args.platformInfo.application_name}/platforms",
                query: [application_name: args.platformInfo.application_name, copyPropertiesForUpgradedModules: copyPropertiesForUpgradedModules],
                body: toJson(args.platformInfo))
        args.platformInfo.version_id++
        log('Platform updated, version_id = ' + response.version_id)
        return response
    }

    @NonCPS
    protected selectModules(Map args) { required(args, ['modules', 'moduleName']) // optional: path
        def matchingModules = listSelectAll(list: args.modules, key: 'name', value: args.moduleName)
        if (!matchingModules) {
            throw new ExpectedEnvironmentException("No module found in platform for name ${args.moduleName}")
        }
        if (args.path) {
            matchingModules = listSelectAll(list: matchingModules, key: 'path', value: args.path)
            if (!matchingModules) {
                throw new ExpectedEnvironmentException("No module found in platform for properties_path ${args.path}")
            }
        }
        return matchingModules
    }

    @NonCPS
    protected selectModule(Map args) { required(args, ['modules', 'moduleName']) // optional: instance, path
        def matchingModules = selectModules(args)
        if (args.instance) {
            def filteredModules = []
            for (int i = 0; i < matchingModules.size(); i++) {
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
        return matchingModules[0]
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
        return matches[0]
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
