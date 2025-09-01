package com.authmat.application.authentication.exception;

public class DuplicateCredentialException extends RuntimeException{
    public DuplicateCredentialException(String message){
        super(message);
    }

}
