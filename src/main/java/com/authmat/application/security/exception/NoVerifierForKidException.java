package com.authmat.application.security.exception;

// TODO: deal with this exception
public class NoVerifierForKidException extends RuntimeException {
    public NoVerifierForKidException(String kid) {
        super(kid);
    }
}
