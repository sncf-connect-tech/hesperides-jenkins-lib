package com.vsct.dt.hesperides.jenkins.pipelines.http


import static groovy.json.JsonOutput.prettyPrint

import static com.vsct.dt.hesperides.jenkins.pipelines.LogUtils.*

import groovy.json.JsonException

import groovyx.net.http.ContentType
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.Method

import java.nio.channels.ReadableByteChannel
import java.nio.charset.StandardCharsets

import org.apache.http.client.utils.URLEncodedUtils


class HTTPBuilderRequester implements Serializable {

    private static final long serialVersionUID = 5649687894321302878L

    def performRequest(Map args) {
        def endpoint = "$args.uri.scheme://$args.uri.host"
        if (args.uri.port != -1) {
            endpoint += ":$args.uri.port"
        }
        def httpClient = new HTTPBuilder(endpoint)
        httpClient.ignoreSSLIssues() // src: https://github.com/jgritman/httpbuilder/blob/master/src/main/java/groovyx/net/http/HTTPBuilder.java#L929
        def parsedResponse
        httpClient.request(Method[args.method], ContentType[args.contentType]) { req ->
            uri.path = args.uri.path
            uri.query = queryStringAsMap(args.uri.query)
            headers.Accept = ContentType[args.accept]
            headers.Authorization = args.authHeader
            body = args.body
            response.success = { resp, content ->
                if (content != null && args.accept != 'JSON') {
                    parsedResponse = inputStreamAsString(content)
                } else {
                    parsedResponse = content
                }
            }
            response.failure = { resp ->
                log COLOR_RED + tryPrettyPrintJSON(responseAsString(resp)) + COLOR_END
                throw new HttpException(resp.statusLine.statusCode, resp.statusLine.reasonPhrase)
            }
        }
        parsedResponse
    }

    def log(msg) {
        System.err.println msg
    }

    private queryStringAsMap(String queryString) {
        def queryMap = [:]
        for (param in URLEncodedUtils.parse(queryString, StandardCharsets.UTF_8)) {
            queryMap[param.name] = param.value
        }
        queryMap
    }

    def inputStreamAsString(stream) {
        def scanner = new Scanner(stream).useDelimiter('\\A')
        scanner.hasNext() ? scanner.next() : ''
    }

    def responseAsString(response) {
        def outputStream = new ByteArrayOutputStream()
        response.entity.writeTo(outputStream)
        outputStream.toString('utf8')
    }

    def tryPrettyPrintJSON(obj) {
        try {
            prettyPrint obj
        } catch (JsonException) {
            obj.toString()
        }
    }
}
