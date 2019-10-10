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
package com.vsct.dt.hesperides.jenkins.pipelines.http

import static groovy.json.JsonOutput.prettyPrint

import static com.vsct.dt.hesperides.jenkins.pipelines.LogUtils.*

import groovy.json.JsonSlurperClassic


class JenkinsHTTRequester implements Serializable {

    private static final long serialVersionUID = 1654684321891394621L

    Object steps

    JenkinsHTTRequester(steps) {
        this.steps = steps
    }

    def performRequest(Map args) {
        // cf. https://jenkins.io/doc/pipeline/steps/http_request/ & https://github.com/jenkinsci/http-request-plugin
        def headers = args.authHeader != null ? [[name: 'Authorization', value: args.authHeader]] : []

        def response = this.steps.httpRequest url: args.uri.toString(),
                                              httpMode: args.method,
                                              requestBody:args.body,
                                              acceptType: args.accept == 'JSON' ? 'APPLICATION_JSON' : 'NOT_SET',
                                              contentType: 'APPLICATION_JSON',  // Beware ! Default is "application/x-www-form-urlencoded", cf. https://issues.jenkins-ci.org/browse/JENKINS-47356
                                              customHeaders: headers,
                                              ignoreSslErrors: true,
                                              validResponseCodes: '100:600'

        steps.echo 'Response header Set-Cookie = ' + tryPrettyPrintJSON(response.headers['Set-Cookie'])
        ['Deprecation', 'Sunset', 'Link'].findAll { header -> response.headers.getAt(header) }
                                         .each { header ->
            logWarn header + ': ' + response.headers.getAt(header)
        }

        if (!(response.status in [200, 201, 202, 203, 204, 205, 206])) {
            logWarn tryPrettyPrintJSON(tryParseJSON(response.content))
            throw new HttpException(response.status, 'HTTP error')
        }

        if (args.accept == 'JSON') {
            tryParseJSON response.content
        } else {
            response.content
        }
    }

    def logWarn(String msg) {
        if (this.steps.sh(script:'echo $TERM', returnStdout: true).trim()) { // means ansiColor plugin is enabled
            this.steps.echo COLOR_RED + msg + COLOR_END
        } else {
            this.steps.echo msg
        }
    }

    def tryParseJSON(String text) {
        try {
            new JsonSlurperClassic().parseText(text)
        } catch (JsonException) {
            text
        } catch (IllegalArgumentException) {
            text
        }
    }

    def tryPrettyPrintJSON(Object obj) {
        try {
            prettyPrint obj
        } catch (JsonException) {
            obj.toString()
        }
    }

}
