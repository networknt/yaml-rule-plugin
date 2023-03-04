package com.networknt.rule.soap.exception;

public class InvalidSoapBodyException extends RuntimeException {

    public InvalidSoapBodyException() {
        super("Invalid Soap Request");
    }

    public InvalidSoapBodyException(String msg) {
           super(msg);
    }
}
