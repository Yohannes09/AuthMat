package com.authmat.application.users.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class CredentialUpdateException extends RuntimeException{
    public CredentialUpdateException(String message){
        super(message);
    }
}
