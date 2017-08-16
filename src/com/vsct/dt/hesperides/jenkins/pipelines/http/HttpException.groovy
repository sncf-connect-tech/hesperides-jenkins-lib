package com.vsct.dt.hesperides.jenkins.pipelines.http


class HttpException extends RuntimeException {

    int statusCode

    HttpException(int statusCode, String msg) {
        super("$msg : $statusCode")
        this.statusCode = statusCode
    }

}
