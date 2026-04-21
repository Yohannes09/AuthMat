package com.authmat.application.exception;

public class UnknownServiceIdentityException extends RuntimeException {
    public UnknownServiceIdentityException(String message) {
        super(message);
    }
}
