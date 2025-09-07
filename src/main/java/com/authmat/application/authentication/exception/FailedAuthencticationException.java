package com.authmat.application.authentication.exception;

public class FailedAuthencticationException extends RuntimeException {
    public FailedAuthencticationException(String message) {
        super(message);
    }
}
