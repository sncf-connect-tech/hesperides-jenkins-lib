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

import spock.lang.Specification
import com.vsct.dt.hesperides.jenkins.pipelines.Hesperides
import com.vsct.dt.hesperides.jenkins.pipelines.http.HTTPBuilderRequester


class HesperidesSpec extends Specification {

    def hesperides = new Hesperides(httpRequester: new HTTPBuilderRequester(), auth: "dum:my")

    def "Hesperides must be serializable in order to be used in Jenkins pipeline"() {
        when:
            def serialized = new ObjectOutputStream(new ByteArrayOutputStream())
            serialized.writeObject(hesperides)

        then:
            serialized != null
    }

    def "updateProperties throws an exception if no commitMsg arg is provided"() {
        when: hesperides.updateProperties(app: 'KTN', platform: 'USN1', jsonPropertyUpdates: 'dummy.json')
        then: thrown IllegalArgumentException
    }

    def "createPlatform throws an exception if no app arg is provided"() {
        when: hesperides.createPlatform(platform: 'USN1', version: '0.0')
        then: thrown IllegalArgumentException
    }
    def "createPlatform throws an exception if no platform arg is provided"() {
        when: hesperides.createPlatform(app: 'ABC', version: '0.0')
        then: thrown IllegalArgumentException
    }
    def "createPlatform throws an exception if no version arg is provided"() {
        when: hesperides.createPlatform(app: 'ABC', platform: 'USN1')
        then: thrown IllegalArgumentException
    }
    def "handle new iterables prpoerties that already exists"() {
        when:
            def initial = [["name": "elasticsearch.indexes",
            "iterable_valorisation_items": [
                ["title": "title1",
                "values": [
                    [
                      "name": "alias_name",
                      "value": "catalog_{{hesperides.platform.name}}"
                    ],
                    [
                      "name": "els_hosts",
                      "value": "zangarella,padiglione,bologne"
                    ]
                ]],
                ["title": "",
                "values": [
                    [
                      "name": "alias_name",
                      "value": "catalog_INT6"
                    ],
                    [
                      "name": "els_hosts",
                      "value": "zangarella,padiglione,bologne"
                    ]
                ]]
            ]]]
            def change = ["elasticsearch.indexes": [
                [
                  "alias_name": "catalog_{{hesperides.platform.name}}",
                  "els_hosts": "sermaglia,barghe,monte",
                ],
                [
                  "alias_name": "catalog_INT6",
                  "els_hosts": "sermaglia,barghe,monte",
                ]
            ]]
            def merged = hesperides.handleIterableProperties(change, initial)
        then:
            assert initial == [["name": "elasticsearch.indexes",
            "iterable_valorisation_items": [
                ["title": "not used",
                "values": [
                    [
                      "name": "alias_name",
                      "value": "catalog_{{hesperides.platform.name}}"
                    ],
                    [
                      "name": "els_hosts",
                      "value": "sermaglia,barghe,monte"
                    ]
                ]],
                ["title": "not used",
                "values": [
                    [
                      "name": "alias_name",
                      "value": "catalog_INT6"
                    ],
                    [
                      "name": "els_hosts",
                      "value": "sermaglia,barghe,monte"
                    ]
                ]]
            ]]]
    }
}
