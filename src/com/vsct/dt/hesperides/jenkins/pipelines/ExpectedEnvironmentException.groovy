package com.vsct.dt.hesperides.jenkins.pipelines


class ExpectedEnvironmentException extends RuntimeException {

    ExpectedEnvironmentException(String msg) {
        super(msg)
    }

}
