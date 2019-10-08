[![](https://travis-ci.org/voyages-sncf-technologies/hesperides-jenkins-lib.svg?branch=master)](https://travis-ci.org/voyages-sncf-technologies/hesperides-jenkins-lib) [![](https://circleci.com/gh/voyages-sncf-technologies/hesperides-jenkins-lib.svg?style=shield&circle-token=0d3df4d3ea31cbfb310f718d969926af6ef7a6bf)](https://circleci.com/gh/voyages-sncf-technologies/hesperides-jenkins-lib)

[![License: GPL v3](https://img.shields.io/badge/License-GPL%20v3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)

![](jenkins-hesperides-apple.png)

This is a [Groovy shared lib](https://jenkins.io/doc/book/pipeline/shared-libraries/) for Jenkins 2 pipelines, but also useable as a Groovy command-line script.

It lets you interact with [Hesperides](https://voyages-sncf-technologies.github.io/hesperides-gui/).
It provides some _high level_ features over the bare HTTP REST API:

- `setPlatformModulesVersion`: change the version of all modules on a plateform
- `updateProperties`: change instance/module/global properties on a given Hesperides platform, from a JSON file listing deltas
- `upsertFromDescriptor`: upsert (create or update) one or several workingcopy module(s) and their templates from a file descriptor

This project is actively maintained.
A detailed releases changelog is available here: [CHANGELOG.md](CHANGELOG.md)

A documentation for the exposed Groovy functions is available here: <https://voyages-sncf-technologies.github.io/hesperides-jenkins-lib/>

# Summary
<!-- To update this ToC: markdown-toc --indent "    " -i README.md -->

<!-- toc -->

- [Installation](#installation)
- [Usage](#usage)
    * [Jenkins pipeline](#jenkins-pipeline)
    * [CLI script for the standard Groovy interpreter](#cli-script-for-the-standard-groovy-interpreter)
- [Contributing / Development](#contributing--development)

<!-- tocstop -->

# Installation

In "Manage Jenkins > Configure System" aka `/jenkins/configure`, adds the `git@...` URL to this repo in **Global Pipeline Libraries**, and **ticks the "load implicitly" checkbox**. You can use either the `master` branch to use the latest version, or specify a tag to ensure more stability.

You will also need to install the [http_request Jenkins plugin](https://wiki.jenkins-ci.org/display/JENKINS/HTTP+Request+Plugin) from [its .hpi](http://updates.jenkins-ci.org/latest/http_request.hpi).


# Usage

## Jenkins pipeline

cf. [examples/Jenkinsfile](examples/Jenkinsfile) & `vars/*.txt` documentation files for examples.

**Note:** to check your Jenkinsfiles syntax, use a linter ! _cf._ https://github.com/Lucas-C/pre-commit-hooks#other-useful-local-hooks

## CLI script for the standard Groovy interpreter
```groovy
@GrabResolver(name='nexus', root='http://nexus.mycompany.com/content/repositories/jenkins-ci/repo.jenkins-ci.org/public')
@Grab(group='com.cloudbees', module='groovy-cps', version='1.12')
@Grab(group='org.codehaus.groovy.modules.http-builder', module='http-builder', version='0.7.2')

import static groovy.json.JsonOutput.*
import com.vsct.dt.hesperides.jenkins.pipelines.Hesperides
import com.vsct.dt.hesperides.jenkins.pipelines.http.HTTPBuilderRequester

def cli = new CliBuilder()
cli.apiRootUrl(args:1, argName:'endpoint', 'Default: https://hesperides.mycompany.com')
cli.auth(args:1, required:true, argName:'auth', 'user:password')
cli.app(args:1, required:true, argName:'trigram', '')
cli.platform(args:1, required:true, argName:'instance', '')
def options = cli.parse(args)
if (options) {
    def hesperides = new Hesperides(apiRootUrl: options.apiRootUrl, auth: options.auth, httpRequester: new HTTPBuilderRequester())
    def platformInfo = hesperides.getPlatformInfo(app: options.app, platform: options.platform)
    System.out.println prettyPrint(toJson(platformInfo))
}
```


# Contributing / Development
_cf._ [CONTRIBUTING.md](CONTRIBUTING.md)
