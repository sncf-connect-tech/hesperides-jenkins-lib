[![](https://travis-ci.org/voyages-sncf-technologies/hesperides-jenkins-lib.svg?branch=master)](https://travis-ci.org/voyages-sncf-technologies/hesperides-jenkins-lib) [![](https://circleci.com/gh/voyages-sncf-technologies/hesperides-jenkins-lib.svg?style=shield&circle-token=0d3df4d3ea31cbfb310f718d969926af6ef7a6bf)](https://circleci.com/gh/voyages-sncf-technologies/hesperides-jenkins-lib)

![](jenkins-hesperides-apple.png)

This is a [Groovy shared lib](https://jenkins.io/doc/book/pipeline/shared-libraries/) for Jenkins 2 pipelines, but also useable as a Groovy command-line script.

It lets you interact with [Hesperides](https://voyages-sncf-technologies.github.io/hesperides-gui/) to perform various tasks:

- platform and module creation
- module release
- update of platform version / modules versions
- update of properties per platform/instance of a platform/module of a platform from a JSON file


# Summary
1. [Installation](#installation)
1. [Usage](#usage)
1. [Tests](#tests)
1. [Development](#development)


# Installation

In "Manage Jenkins > Configure System" aka `/jenkins/configure`, adds the `git@...` URL to this repo in **Global Pipeline Libraries**, and **ticks the "load implicitly" checkbox**. You can use either the `master` branch to use the latest version, or specify a tag to ensure more stability.

You will also need to install the [http_request Jenkins plugin](https://wiki.jenkins-ci.org/display/JENKINS/HTTP+Request+Plugin) from [its .hpi](http://updates.jenkins-ci.org/latest/http_request.hpi).


# Usage

## Jenkins pipeline

cf. [Jenkinsfile](Jenkinsfile) & `vars/*.txt` documentation files for examples.

**Note:** to check your Jenkinsfiles syntax, use a linter ! _cf._ https://github.com/Lucas-C/pre-commit-hooks#other-useful-local-hooks

## CLI script for the standard Groovy interpreter

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
        def hesperides = new Hesperides(apiRootUrl: options.apiRootUrl, httpRequester: new HTTPBuilderRequester())
        def platformInfo = hesperides.getPlatformInfo(auth: options.auth, app: options.app, platform: options.platform)
        System.out.println prettyPrint(toJson(platformInfo))
    }


# Tests

The tests require the `$HESPERIDES_HOST` environment variable to be set, including the protocol.
An optional `$HESPERIDES_PORT` can also be specified,
along with `$HESPERIDES_AUTH` as "<USERNAME>:<PASSWORD>".

    gradle test

To run a single test:

    gradle -Dtest.single=HesperidesIntegrationSpec test

**!WARNING!** -> Integration tests perform modifications on the target Hesperides instance

The test report is generated in `build/reports/tests/test/index.html`.

## With docker-compose

Integration tests use a dockerized Hesperides instance.

    docker-compose build
    docker-compose run gradle-test

If you want to only use Docker to launch an Hesperides instance:

    docker-compose up -d hesperides
    gradle test


# Development

## HTTPRequester

Because we want this library to be usable both with the standard Groovy interpreter and the jenkins groovy-cps plugin,
we faced the challenge of making HTTP requests in both contexts:

- in Jenkins pipelines, the recommended solution is to use the non-builtin [http_request](https://jenkins.io/doc/pipeline/steps/http_request/) plugin.
Another, more hacky approach, would be to use the [sh step](https://jenkins.io/doc/pipeline/steps/workflow-durable-task-step/#code-sh-code-shell-script) + `curl`.

- with the Groovy standard interpreter, [groovyx.net.http.HTTPBuilder](https://github.com/jgritman/httpbuilder/wiki) is a very common library to make HTTP calls

**Both are based on [org.apache.httpcomponents.httpclient](https://hc.apache.org/httpcomponents-client-ga/index.html)**.

In order to use either one dependeing on the execution context, we created the `com.vsct.dt.hesperides.jenkins.pipelines.http` package to abstract this into an `HTTPRequester` interface:

<!-- To generate the .png from the .txt file with PlantUML:
java -jar plantuml.jar -tpng HTTPRequester.txt
-->
![](HTTPRequester.png)

## Release & upload to Nexus

1. `git tag` & `git push --tags`
2. modifify the `build.gradle` according to this tag
3. set the `NEXUS_URL` / `NEXUS_USER` / `NEXUS_PASSWORD` environment variables
4. `gradle upload`

## Coding style

Use the CodeNarc Groovy linter:

    gradle check

- following [Robert C. Martin "Clean Code" recommendations](https://image.slidesharecdn.com/cleancode-vortrag-03-2009-pdf-121006112415-phpapp02/95/clean-code-pdf-version-16-728.jpg?cb=1349523162), we avoid methods with too many parameters. We use named-parameter with `Map args`, and validate necesseray parameters with `_required`.
- do NOT `@Grab` in source files under `src/`, it makes the code non-testable


## Known issues with Jenkins Pipeline

:bangbang: :bangbang: :bangbang:
```
+-----------------------------------------------------------------------------------------------------------+
| Use of `.each`, `.find`, `.collect`, etc. builtin Groovy methods is prohibited OUTSIDE @NonCPS functions    |
| cf. https://github.com/cloudbees/groovy-cps/issues/9 & https://issues.jenkins-ci.org/browse/JENKINS-26481 |
|                                                                                                           |
| Moreover, while UnsupportedOperationExceptions will be raised if used directly in Jenkinsfiles,           |
| it will fail silently if used inside a shared library.                                                    |
| cf. https://issues.jenkins-ci.org/browse/JENKINS-42024                                                    |
+-----------------------------------------------------------------------------------------------------------+
```
:bangbang: :bangbang: :bangbang:

- it does not support tuples : https://issues.jenkins-ci.org/browse/JENKINS-38846
- iterators are not supported : https://issues.jenkins-ci.org/browse/JENKINS-27421
- `abstract` classes & `traits` do not work : cf. https://issues.jenkins-ci.org/browse/JENKINS-39329 & https://issues.jenkins-ci.org/browse/JENKINS-46145
- invoking CPS-transformed methods from constructors : https://issues.jenkins-ci.org/browse/JENKINS-26313
- static nested classes limitations : https://issues.jenkins-ci.org/browse/JENKINS-41896
- use `JsonSlurperClassic` instead of `JsonSlurper` : http://stackoverflow.com/a/38439681/636849
- assignment in `if` statements : https://issues.jenkins-ci.org/browse/JENKINS-41422


## Other known issuess

`javax.net.ssl.SSLException: java.lang.RuntimeException: Could not generate DH keypair` exception when using [http_request](https://wiki.jenkins-ci.org/display/JENKINS/HTTP+Request+Plugin) plugin : you need to run Jenkins with Java 8, a bug with Java 7 will prevent you from making HTTPS requests.

cf. http://vboard.vsct.fr/vblog/?p=561

## Debugging Grapes resolution

Deleting an artifact in both Groovy & Maven caches, to test re-downloading (yes, Groovy will use Maven cache by default !) :

    rm -r %HOME%\.groovy\grapes\com.cloudbees\groovy-cps %HOME%\.m2\repository\com\cloudbees\groovy-cps

Rerunning a script with increased logging:

    set CLASSPATH=src
    groovy -Dgroovy.grape.report.downloads=true -Divy.message.logger.level=3 vars/getHesperidesPlatformInfo.groovy --app CSC --platform USN1

Look for strings like "downloading https://jcenter.bintray.com/com/cloudbees/groovy-cps/1.12/groovy-cps-1.12.jar".

To disable default groovy resolvers (like jcenter), you need to create a [`~/.groovy/grapeConfig.xml`](http://docs.groovy-lang.org/latest/html/documentation/grape.html#Grape-CustomizeIvysettings) file based on [the default one](https://github.com/apache/groovy/blob/master/src/resources/groovy/grape/defaultGrapeConfig.xml), then remove the resolver entries you don't want.
