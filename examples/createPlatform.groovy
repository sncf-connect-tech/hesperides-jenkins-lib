#!/usr/bin/env groovy
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

@Grab(group='com.cloudbees', module='groovy-cps', version='1.31')
@Grab(group='org.codehaus.groovy.modules.http-builder', module='http-builder', version='0.7.2')

import static groovy.json.JsonOutput.*
import com.vsct.dt.hesperides.jenkins.pipelines.Hesperides
import com.vsct.dt.hesperides.jenkins.pipelines.http.HTTPBuilderRequester


def cli = new CliBuilder()
cli.help('')
cli.apiUrl(args:1, required:true, '')
cli.auth(args:1, required:true, 'Format: user:password')
cli.app(args:1, required:true, '')
cli.platform(args:1, required:true, '')
cli.version(args:1, required:true, '')
cli.isProduction(args:1, '')
cli.fromApplication(args:1, '')
cli.fromPlatform(args:1, '')
def options = cli.parse(args)
if (options) {
    def hesperides = new Hesperides(apiRootUrl: options.apiUrl, auth: options.auth, httpRequester: new HTTPBuilderRequester())
    def platformInfo = hesperides.createPlatform(app: options.app,
                                                 platform: options.platform,
                                                 version: options.version,
                                                 isProduction: options.isProduction,
                                                 fromApplication: options.fromApplication ?: '',
                                                 fromPlatform: options.fromPlatform ?: '')
    System.out.println prettyPrint(toJson(platformInfo))
}
