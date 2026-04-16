package com.authmat.application.security.exception;

public class InvalidPrincipalException extends RuntimeException {
    public InvalidPrincipalException(String message) {
        super(message);
    }
}
