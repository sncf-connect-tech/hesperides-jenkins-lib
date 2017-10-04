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

import com.vsct.dt.hesperides.jenkins.pipelines.Hesperides
import com.vsct.dt.hesperides.jenkins.pipelines.http.JenkinsHTTRequester


/******************************************************************************

                            APPLICATIONS & PLATFORMS

******************************************************************************/
def getAppInfo(Map args) {
    new Hesperides(apiRootUrl: args.apiRootUrl,
                   auth: args.auth,
                   httpRequester: new JenkinsHTTRequester(this.steps),
                   steps: this.steps).getAppInfo(args.app)
}

def createPlatform(Map args) {
    new Hesperides(apiRootUrl: args.apiRootUrl,
                   auth: args.auth,
                   httpRequester: new JenkinsHTTRequester(this.steps),
                   steps: this.steps).createPlatform(args)
}

def getPlatformInfo(Map args) {
    new Hesperides(apiRootUrl: args.apiRootUrl,
                   auth: args.auth,
                   httpRequester: new JenkinsHTTRequester(this.steps),
                   steps: this.steps).getPlatformInfo(args)
}

def deletePlatform(Map args) {
    new Hesperides(apiRootUrl: args.apiRootUrl,
                   auth: args.auth,
                   httpRequester: new JenkinsHTTRequester(this.steps),
                   steps: this.steps).deletePlatform(args)
}

def setPlatformVersion(Map args) {
    new Hesperides(apiRootUrl: args.apiRootUrl,
                   auth: args.auth,
                   httpRequester: new JenkinsHTTRequester(this.steps),
                   steps: this.steps).setPlatformVersion(args)
}


/******************************************************************************

                                    MODULES

******************************************************************************/
def getModule(Map args) {
    new Hesperides(apiRootUrl: args.apiRootUrl,
                   auth: args.auth,
                   httpRequester: new JenkinsHTTRequester(this.steps),
                   steps: this.steps).getModule(args)
}

def createModule(Map args) {
    new Hesperides(apiRootUrl: args.apiRootUrl,
                   auth: args.auth,
                   httpRequester: new JenkinsHTTRequester(this.steps),
                   steps: this.steps).createModule(args)
}

def releaseModule(Map args) {
    new Hesperides(apiRootUrl: args.apiRootUrl,
                   auth: args.auth,
                   httpRequester: new JenkinsHTTRequester(this.steps),
                   steps: this.steps).releaseModule(args)
}

def deleteModule(Map args) {
    new Hesperides(apiRootUrl: args.apiRootUrl,
                   auth: args.auth,
                   httpRequester: new JenkinsHTTRequester(this.steps),
                   steps: this.steps).deleteModule(args)
}

def putModuleOnPlatform(Map args) {
    new Hesperides(apiRootUrl: args.apiRootUrl,
                   auth: args.auth,
                   httpRequester: new JenkinsHTTRequester(this.steps),
                   steps: this.steps).putModuleOnPlatform(args)
}

def setPlatformModuleVersion(Map args) {
    new Hesperides(apiRootUrl: args.apiRootUrl,
                   auth: args.auth,
                   httpRequester: new JenkinsHTTRequester(this.steps),
                   steps: this.steps).setPlatformModuleVersion(args)
}

def setPlatformModulesVersion(Map args) {
    new Hesperides(apiRootUrl: args.apiRootUrl,
                   auth: args.auth,
                   httpRequester: new JenkinsHTTRequester(this.steps),
                   steps: this.steps).setPlatformModulesVersion(args)
}

def doesWorkingcopyExistForModuleVersion(Map args) {
    new Hesperides(apiRootUrl: args.apiRootUrl,
                   auth: args.auth,
                   httpRequester: new JenkinsHTTRequester(this.steps),
                   steps: this.steps).doesWorkingcopyExistForModuleVersion(args)
}

def doesReleaseExistForModuleVersion(Map args) {
    new Hesperides(apiRootUrl: args.apiRootUrl,
                   auth: args.auth,
                   httpRequester: new JenkinsHTTRequester(this.steps),
                   steps: this.steps).doesReleaseExistForModuleVersion(args)
}


/******************************************************************************

                            TEMPLATES & PROPERTIES

******************************************************************************/
def updateProperties(Map args) {
    new Hesperides(apiRootUrl: args.apiRootUrl,
                   auth: args.auth,
                   httpRequester: new JenkinsHTTRequester(this.steps),
                   steps: this.steps).updateProperties(args)
}

def createTemplate(Map args) {
    new Hesperides(apiRootUrl: args.apiRootUrl,
                   auth: args.auth,
                   httpRequester: new JenkinsHTTRequester(this.steps),
                   steps: this.steps).createTemplate(args)
}

def getModuleTemplateProperties(Map args) {
    new Hesperides(apiRootUrl: args.apiRootUrl,
                   auth: args.auth,
                   httpRequester: new JenkinsHTTRequester(this.steps),
                   steps: this.steps).getModuleTemplateProperties(args)
}

def getModulePropertiesForPlatform(Map args) {
    new Hesperides(apiRootUrl: args.apiRootUrl,
                   auth: args.auth,
                   httpRequester: new JenkinsHTTRequester(this.steps),
                   steps: this.steps).getModulePropertiesForPlatform(args)
}


/******************************************************************************

                                    INSTANCES

******************************************************************************/
def createInstance(Map args) {
    new Hesperides(apiRootUrl: args.apiRootUrl,
                   auth: args.auth,
                   httpRequester: new JenkinsHTTRequester(this.steps),
                   steps: this.steps).createInstance(args)
}

def deleteInstance(Map args) {
    new Hesperides(apiRootUrl: args.apiRootUrl,
                   auth: args.auth,
                   httpRequester: new JenkinsHTTRequester(this.steps),
                   steps: this.steps).deleteInstance(args)
}

def getInstanceProperties(Map args) {
    new Hesperides(apiRootUrl: args.apiRootUrl,
                   auth: args.auth,
                   httpRequester: new JenkinsHTTRequester(this.steps),
                   steps: this.steps).getInstanceProperties(args)
}

def getInstanceFiles(Map args) {
    new Hesperides(apiRootUrl: args.apiRootUrl,
                   auth: args.auth,
                   httpRequester: new JenkinsHTTRequester(this.steps),
                   steps: this.steps).getInstanceFiles(args)
}
