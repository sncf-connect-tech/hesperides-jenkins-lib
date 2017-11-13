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

import spock.lang.Shared
import spock.lang.Specification
import com.vsct.dt.hesperides.jenkins.pipelines.Hesperides
import com.vsct.dt.hesperides.jenkins.pipelines.http.HTTPBuilderRequester


// Les méthodes définies dans un trait ne seront pas exécutées comme des tests
trait Helper {
    /**
     * +1 au dernier chiffre précédé d'un '.'
     * @param version au format #.#.# ...
     * @return
     */
    def nextVersion(String version) {
        def versionDigits = version.split('\\.')
        def baseVersion = versionDigits.length > 1 ? versionDigits[0..versionDigits.length - 2].join('.') + '.' : ''
        def lastNumber = versionDigits.last() as Integer

        "${baseVersion}${lastNumber + 1}"
    }
}

class HesperidesIntegrationSpec extends Specification implements Helper {

    static final ENV = System.properties
    @Shared
    Hesperides hesperides = new Hesperides(apiRootUrl: "${ENV.HESPERIDES_HOST}:${ENV.HESPERIDES_PORT}",
                                           auth: ENV.HESPERIDES_AUTH,
                                           httpRequester: new HTTPBuilderRequester())

    // Utilisation de valeurs random afin de ne pas avoir à docker-compose down/up entre chaque lancement en mode dev.
    static applicationName = 'app'
    static platformName = 'platform'
    static moduleName = 'module'
    static secondModuleName = 'moduletwo'
    static moduleVersion = '2.0.0.0'
    static instanceName = 'instance'
    static instanceNameTwo = 'instanceTwo'
    static instanceNameThree = 'instanceThree'
    static logicGroupName = 'GROUP'
    static logicGroupNameTwo = 'CUSTOMGROUP'
    static subLogicGroup = 'TECHNO'


    def setupSpec() {
        log "${ENV.HESPERIDES_AUTH}@${ENV.HESPERIDES_HOST}:${ENV.HESPERIDES_PORT}"
        hesperides.createPlatform(app: applicationName, platform: platformName, version: '1.0.0.0')
        def infos = hesperides.getPlatformInfo(app: applicationName, platform: platformName)
        hesperides.createModule(moduleName: moduleName, version: moduleVersion)
        hesperides.createModule(moduleName: secondModuleName, version: moduleVersion)
        hesperides.putModuleOnPlatform(app: applicationName,
                platform: platformName,
                moduleName: moduleName,
                moduleVersion: moduleVersion,
                isWorkingCopy: true,
                logicGroupPath: "#${logicGroupName}#${subLogicGroup}")
        hesperides.putModuleOnPlatform(app: applicationName,
                platform: platformName,
                moduleName: secondModuleName,
                moduleVersion: moduleVersion,
                isWorkingCopy: true,
                logicGroupPath: "#${logicGroupName}#${subLogicGroup}")
        hesperides.putModuleOnPlatform(app: applicationName,
                platform: platformName,
                moduleName: secondModuleName,
                moduleVersion: moduleVersion,
                isWorkingCopy: true,
                logicGroupPath: "#${logicGroupNameTwo}#${subLogicGroup}")
        hesperides.createInstance(app: applicationName, platform: platformName, moduleName: moduleName, instance: instanceName, path: "#${logicGroupName}#${subLogicGroup}")
        hesperides.createInstance(app: applicationName, platform: platformName, moduleName: secondModuleName, instance: instanceNameTwo, path: "#${logicGroupName}#${subLogicGroup}")
        hesperides.createInstance(app: applicationName, platform: platformName, moduleName: secondModuleName, instance: instanceNameThree, path: "#${logicGroupNameTwo}#${subLogicGroup}")
    }

    def cleanupSpec() {
        hesperides.deletePlatform(app:applicationName, platform: platformName)
        hesperides.deleteModule(moduleName: moduleName, version: moduleVersion, moduleType: 'workingcopy')
        hesperides.deleteModule(moduleName: secondModuleName, version: moduleVersion, moduleType: 'workingcopy')
    }

    def "Can get versions of a module"() {
        when:
        def versions = hesperides.getModuleVersions(moduleName: moduleName)
        then:
        versions == [moduleVersion]
    }

    def "Can create a module"() {
        when:
            def moduleName = 'module-' + UUID.randomUUID().toString()
            def moduleVersion = '2.0.0.1'
            def module = hesperides.createModule(moduleName: moduleName, version: moduleVersion)
        then:
            module.version == moduleVersion
            module.name == moduleName
        cleanup:
            hesperides.deleteModule(moduleName: moduleName, version: moduleVersion, moduleType: 'workingcopy')
    }

    def "Can get platform info"() {
        when:
            def platformInfo = hesperides.getPlatformInfo(app: applicationName, platform: platformName)
            log "platform info : \n ${platformInfo}"

        then:
            platformInfo['application_name'] == applicationName
            platformInfo['platform_name'] == platformName
            !platformInfo['production']
    }

    def "Can get properties for a given module template version"() {
        when:
            def props = hesperides.getModuleTemplateProperties(moduleName: moduleName, version: moduleVersion, isRelease: false)

        then:
            props['key_value_properties'] != null
            props['iterable_properties'] != null
    }

    def "Can get platform properties for a random module path"() {
        when:
            def modulePropertiesPath = hesperides.getPlatformInfo(app: applicationName, platform: platformName).modules[0].properties_path
            def props = hesperides.getModulePropertiesForPlatform(app: applicationName, platform: platformName, modulePropertiesPath: modulePropertiesPath)

        then:
            props['key_value_properties'] != null
            props['iterable_properties'] != null
    }

    def "Can update new properties"() {
        when:
            def jsonProperties = """
                {
                  "${moduleName}": {
                    "LCM_vha_test_property": 42,
                    "LCM_vha_test_builtin_property": "{{hesperides.platform.name}}",
                    "iterable_properties": {
                      "LCM_vha_test_iterableProps": [
                        {
                          "LCM_vha_test_iterableProp": "TOTO"
                        }
                      ]
                    }
                  },
                  "${moduleName}#${instanceName}": {
                    "LCM_vha_test_instance_property": "hello World !"
                  },
                  "path:#${logicGroupNameTwo}#${subLogicGroup}#${secondModuleName}": {
                    "propriete_commune_secondmodule": "Canon Garrick"
                  },
                  "${secondModuleName}": {
                     "propriete_commune_secondmodule": "Kamehameha"
                  },
                  "GLOBAL": {
                    "LCM_vha_test_global_property": "Over 9000 !"
                  }
                }
                """

            File jsonFile = new File('jsonProperties')
            jsonFile.text = jsonProperties
            hesperides.updateProperties(app: applicationName,
                                        platform: platformName,
                                        jsonPropertyUpdates: jsonFile.path,
                                        commitMsg: 'hesperides-jenkins-lib Spock tests')
            jsonFile.delete()

            def modulePropertiesPath = "#${logicGroupName}#${subLogicGroup}#${moduleName}#${moduleVersion}#WORKINGCOPY"
            def moduleTwoPropertiesPath = "#${logicGroupName}#${subLogicGroup}#${secondModuleName}#${moduleVersion}#WORKINGCOPY"
            def moduleTwoPropertiesPathTwo = "#${logicGroupNameTwo}#${subLogicGroup}#${secondModuleName}#${moduleVersion}#WORKINGCOPY"
            def platformProps = hesperides.getModulePropertiesForPlatform(app: applicationName, platform: platformName, modulePropertiesPath: modulePropertiesPath)
            def platformPropsModuleTwo = hesperides.getModulePropertiesForPlatform(app: applicationName, platform: platformName, modulePropertiesPath: moduleTwoPropertiesPath)
            def platformPropsModuleTwoOtherPath = hesperides.getModulePropertiesForPlatform(app: applicationName, platform: platformName, modulePropertiesPath: moduleTwoPropertiesPathTwo)
            def globalProps = hesperides.getModulePropertiesForPlatform(app: applicationName, platform: platformName, modulePropertiesPath: '#')
            def instanceProps = hesperides.getInstanceProperties(app: applicationName, platform: platformName, moduleName: moduleName, instance: instanceName)
            log("platform pppties:"+platformPropsModuleTwo)

        then:
            platformProps['key_value_properties'].find { it.name == 'LCM_vha_test_property' }
            platformProps['key_value_properties'].find { it.name == 'LCM_vha_test_property' }['value'] == '42'
            platformPropsModuleTwo['key_value_properties'].find { it.name == 'propriete_commune_secondmodule'}['value'] == 'Kamehameha'
            platformProps['key_value_properties'].find {
                it.name == 'LCM_vha_test_builtin_property'
            }['value'] == '{{hesperides.platform.name}}'

            platformProps['iterable_properties'].size() > 0
            platformProps['iterable_properties'].find {
                it.name == 'LCM_vha_test_iterableProps'
            }
            platformProps['iterable_properties'].find {
                it.name == 'LCM_vha_test_iterableProps'
            }['iterable_valorisation_items'].size() > 0
            platformProps['iterable_properties'].find {
                it.name == 'LCM_vha_test_iterableProps'
            }['iterable_valorisation_items'][0]['values'].find {
                it.name == 'LCM_vha_test_iterableProp'
            }['value'] == 'TOTO'

            globalProps['key_value_properties'].find {
                it.name == 'LCM_vha_test_global_property'
            }['value'] == 'Over 9000 !'
            instanceProps['key_values'].find { it.name == 'LCM_vha_test_instance_property' }['value'] == 'hello World !'
            platformPropsModuleTwoOtherPath['key_value_properties'].find { it.name == 'propriete_commune_secondmodule'}['value'] == 'Canon Garrick'
    }

    def "Can upgrade platform version"() {
        when:
            def currentVersion = hesperides.getPlatformInfo(app: applicationName, platform: platformName)['application_version']
            def newVersion = nextVersion(currentVersion)
            hesperides.setPlatformVersion(app: applicationName,
                    platform: platformName,
                    currentVersion: currentVersion,
                    newVersion: newVersion)
            def platformInfo = hesperides.getPlatformInfo(app: applicationName, platform: platformName)

        then:
            platformInfo['application_version'] == newVersion

        cleanup:
            hesperides.setPlatformVersion(app: applicationName,
                    platform: platformName,
                    currentVersion: newVersion,
                    newVersion: currentVersion)
    }

    def "Can upgrade modules versions on a platform"() {
        when:
            def currentVersion = hesperides.getPlatformInfo(app: applicationName, platform: platformName)['modules'][0]['version']
            def newVersion = nextVersion(currentVersion)
            hesperides.setPlatformModulesVersion(app: applicationName,
                    platform: platformName,
                    currentVersion: currentVersion,
                    newVersion: newVersion)
            def platformInfo = hesperides.getPlatformInfo(app: applicationName, platform: platformName)

        then:
            platformInfo['modules'].every { it['version'] == newVersion }

        cleanup:
            hesperides.setPlatformModulesVersion(app: applicationName,
                    platform: platformName,
                    currentVersion: newVersion,
                    newVersion: currentVersion)
    }

    def "Can upgrade module version on a platform"() {
        when:
        def platformInfo = hesperides.getPlatformInfo(app: applicationName, platform: platformName)
        def currentVersion = platformInfo['modules'][0]['version']
        def moduleName = platformInfo['modules'][0]['name']
        def newVersion = nextVersion(currentVersion)
        hesperides.setPlatformModuleVersion(app: applicationName,
                platform: platformName,
                newVersion: newVersion,
                moduleName:moduleName)
        def platformInfoUpdated = hesperides.getPlatformInfo(app: applicationName, platform: platformName)

        then:
        platformInfoUpdated['modules'].find { it['name'] == moduleName && it['version'] == newVersion }

        cleanup:
        hesperides.setPlatformModuleVersion(app: applicationName,
                platform: platformName,
                currentVersion: newVersion,
                newVersion: currentVersion,
                moduleName:moduleName)
    }

    def "Can create a new module in workingcopy from scratch"() {
        when:
            hesperides.createModule(moduleName: 'toto', version: '0.0')

        then:
            hesperides.getModule(moduleName: 'toto', version: '0.0', moduleType: 'workingcopy')

        cleanup:
            hesperides.deleteModule(moduleName: 'toto', version: '0.0', moduleType: 'workingcopy')
    }

    def "Can release a module in workingcopy with same version"() {
        when:
            hesperides.createModule(moduleName: 'toto', version: '0.0')
            hesperides.releaseModule(moduleName: 'toto', workingcopyVersion: '0.0', releaseVersion: '0.0')

        then:
            hesperides.getModule(moduleName: 'toto', version: '0.0', moduleType: 'release')

        cleanup:
            hesperides.deleteModule(moduleName: 'toto', version: '0.0', moduleType: 'release')
            hesperides.deleteModule(moduleName: 'toto', version: '0.0', moduleType: 'workingcopy')
    }

    def "Can release a module in workingcopy with a different version"() {
        when:
            hesperides.createModule(moduleName: 'toto', version: '0.0')
            hesperides.releaseModule(moduleName: 'toto', workingcopyVersion: '0.0', releaseVersion: '0.1')

        then:
            hesperides.getModule(moduleName: 'toto', version: '0.1', moduleType: 'release')

        cleanup:
            hesperides.deleteModule(moduleName: 'toto', version: '0.1', moduleType: 'release')
            hesperides.deleteModule(moduleName: 'toto', version: '0.0', moduleType: 'workingcopy')
    }

    def "Can create a new module in workingcopy from a module in workingcopy"() {
        when:
            hesperides.createModule(moduleName: 'toto', version: '0.0')
            hesperides.createModule(moduleName: 'toto', version: '0.1', fromModule: [name: 'toto', version: '0.0', isWorkingcopy: true])

        then:
            hesperides.getModule(moduleName: 'toto', version: '0.1', moduleType: 'workingcopy')

        cleanup:
            hesperides.deleteModule(moduleName: 'toto', version: '0.1', moduleType: 'workingcopy')
            hesperides.deleteModule(moduleName: 'toto', version: '0.0', moduleType: 'workingcopy')
    }

    def "Can create a new module in workingcopy from a released module"() {
        when:
            hesperides.createModule(moduleName: 'toto', version: '0.0')
            hesperides.releaseModule(moduleName: 'toto', workingcopyVersion: '0.0', releaseVersion: '0.0')
            hesperides.createModule(moduleName: 'toto', version: '0.1', fromModule: [name: 'toto', version: '0.0'])

        then:
            hesperides.getModule(moduleName: 'toto', version: '0.1', moduleType: 'workingcopy')

        cleanup:
            hesperides.deleteModule(moduleName: 'toto', version: '0.1', moduleType: 'workingcopy')
            hesperides.deleteModule(moduleName: 'toto', version: '0.0', moduleType: 'release')
            hesperides.deleteModule(moduleName: 'toto', version: '0.0', moduleType: 'workingcopy')
    }

    def "Can download all files of an instance"() {
        when:
            hesperides.createTemplate(moduleName: moduleName, moduleVersion: moduleVersion, location: '/etc', filename: 'titi', content: 'iam=titi')
            hesperides.createTemplate(moduleName: moduleName, moduleVersion: moduleVersion, location: '/etc', filename: 'toto', content: 'iam=toto')
            def files = hesperides.getInstanceFiles(app: applicationName, platform: platformName, moduleName: moduleName, instance: instanceName)
        then:
            files == [
                '/etc/titi': 'iam=titi',
                '/etc/toto': 'iam=toto',
            ]
    }

    def "Can delete an instance"() {
        setup:
            hesperides.createInstance(app: applicationName, platform: platformName, moduleName: moduleName, instance: 'TOTO')
            hesperides.getInstanceProperties(app: applicationName, platform: platformName, moduleName: moduleName, instance: 'TOTO') == []
        when:
            hesperides.deleteInstance(app: applicationName, platform: platformName, moduleName: moduleName, instance: 'TOTO')
        then:
            hesperides.getInstanceProperties(app: applicationName, platform: platformName, moduleName: moduleName, instance: 'TOTO') == null
    }

    def "Can delete all instances"() {
        setup:
            hesperides.createInstance(app: applicationName, platform: platformName, moduleName: moduleName, instance: 'TOTO')
            hesperides.getInstanceProperties(app: applicationName, platform: platformName, moduleName: moduleName, instance: 'TOTO') == []
        when:
            hesperides.deleteInstance(app: applicationName, platform: platformName, moduleName: moduleName, instance: '*')
        then:
            hesperides.getInstanceProperties(app: applicationName, platform: platformName, moduleName: moduleName, instance: instanceName) == null
            hesperides.getInstanceProperties(app: applicationName, platform: platformName, moduleName: moduleName, instance: 'TOTO') == null
    }

    def log(msg) {
        System.out.println msg
    }

}
