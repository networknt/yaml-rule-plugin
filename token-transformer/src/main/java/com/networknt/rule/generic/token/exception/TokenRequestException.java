package com.networknt.rule.generic.token.exception;

import java.net.http.HttpRequest;

public class TokenRequestException extends RuntimeException {
    public TokenRequestException(final HttpRequest request) {
        super("Request failed trying to send a request to: " + request.uri().toString());
    }
}
