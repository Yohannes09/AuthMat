package com.authmat.application.security.exception;

public class InvalidPublicKeyException extends RuntimeException {
    public InvalidPublicKeyException(String message, Exception cause) {
        super(message, cause);
    }
}
