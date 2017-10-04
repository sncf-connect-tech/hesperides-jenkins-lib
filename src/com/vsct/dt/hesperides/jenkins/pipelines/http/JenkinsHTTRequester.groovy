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
                                              contentType: args.contentType == 'JSON' ? 'APPLICATION_JSON' : 'NOT_SET',
                                              customHeaders: headers,
                                              ignoreSslErrors: true,
                                              validResponseCodes: '100:600'

        if (!(response.status in [200, 201])) {
            log COLOR_RED + tryPrettyPrintJSON(tryParseJSON(response.content)) + COLOR_END
            throw new HttpException(response.status, 'HTTP error')
        }

        if (args.accept == 'JSON') {
            tryParseJSON response.content
        } else {
            response.content
        }
    }

    def tryParseJSON(text) {
        try {
            new JsonSlurperClassic().parseText(text)
        } catch (JsonException) {
            text
        } catch (IllegalArgumentException) {
            text
        }
    }

    def tryPrettyPrintJSON(obj) {
        try {
            prettyPrint obj
        } catch (JsonException) {
            obj.toString()
        }
    }

}
