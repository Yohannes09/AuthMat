package com.authmat.application.authentication.service;

import com.authmat.application.authentication.dto.AuthenticationResponse;
import com.authmat.application.authentication.dto.LoginRequest;
import com.authmat.application.authentication.dto.RegistrationRequest;

public interface AuthenticationService {
    AuthenticationResponse login(LoginRequest loginRequest);

    void register(RegistrationRequest registrationRequest);

    AuthenticationResponse refresh(Long id);

    void logout(String token);
}
