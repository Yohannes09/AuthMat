package com.authmat.application.authentication;

public class DuplicateCredentialException extends RuntimeException{
    public DuplicateCredentialException(String message){
        super(message);
    }

}
