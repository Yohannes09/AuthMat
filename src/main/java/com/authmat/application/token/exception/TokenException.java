package com.authmat.application.token.exception;

public class TokenException extends RuntimeException {
    public TokenException(String msg, Throwable e) {
        super(msg, e);
    }

    public TokenException(String msg) {
        super(msg);
    }
}
