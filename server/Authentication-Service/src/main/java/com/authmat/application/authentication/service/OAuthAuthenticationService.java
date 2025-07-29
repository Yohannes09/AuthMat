package com.authmat.application.authentication.service;

import com.authmat.application.authentication.dto.AuthenticationResponse;
import com.authmat.application.authentication.dto.LoginRequest;
import com.authmat.application.authentication.dto.RegistrationRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
@Qualifier("OAuthAuthenticationService")
public class OAuthAuthenticationService implements AuthenticationService {
    @Override
    public AuthenticationResponse login(LoginRequest loginRequest) {
        return null;
    }

    @Override
    public void register(RegistrationRequest registrationRequest) {

    }

    @Override
    public void logout(String token) {

    }

    @Override
    public AuthenticationResponse refresh(Long id) {
        return null;
    }
}
