package com.networknt.rule.soap.exception;

public class InvalidJsonBodyException extends RuntimeException {

    public InvalidJsonBodyException() {
        super("invalid json body");
    }

    public InvalidJsonBodyException(String msg) {
        super(msg);
    }
}
