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
import com.vsct.dt.hesperides.jenkins.pipelines.http.SerializableURIBuilder


class SerializableURIBuilderSpec extends Specification {

    def "SerializableURIBuilder must be serializable"() {
        setup:
            def uriBuilder = new SerializableURIBuilder('https://hesperides')
            uriBuilder.path = '/rest/applications/CSC/platforms/USN1'

        when:
            def serialized = new ObjectOutputStream(new ByteArrayOutputStream())
            serialized.writeObject(uriBuilder)

        then:
            serialized != null
    }
}
