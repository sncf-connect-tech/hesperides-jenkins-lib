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
