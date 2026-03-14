package com.authmat.application.token.exception;

public class TokenConstructionException extends RuntimeException {
    public TokenConstructionException(String msg, Throwable e) {
        super(msg, e);
    }
}
