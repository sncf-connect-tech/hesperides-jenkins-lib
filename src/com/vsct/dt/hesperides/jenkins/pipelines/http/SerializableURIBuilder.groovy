package com.vsct.dt.hesperides.jenkins.pipelines.http

import org.apache.http.client.utils.URIBuilder


class SerializableURIBuilder extends URIBuilder implements Serializable {

    private static final long serialVersionUID = 13648552184566987952L

    SerializableURIBuilder(String string) {
        super(string)
    }
}
