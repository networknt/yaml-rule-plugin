package com.networknt.rule.generic.token.exception;

import java.net.http.HttpRequest;

public class TokenRequestInterruptedException extends RuntimeException {
    public TokenRequestInterruptedException(final HttpRequest request) {
        super("Request was interrupted when sending a request to: " + request.uri().toString());
    }
}
