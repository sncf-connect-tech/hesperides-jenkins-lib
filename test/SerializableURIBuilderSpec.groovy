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
