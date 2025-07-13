package com.authmat.authentication.service.auth;

import com.authmat.authentication.dto.authentication.AuthenticationResponse;
import com.authmat.authentication.dto.authentication.LoginRequest;
import com.authmat.authentication.dto.authentication.RegistrationRequest;

public interface AuthenticationService {
    AuthenticationResponse login(LoginRequest loginRequest);

    void register(RegistrationRequest registrationRequest);

    AuthenticationResponse refresh(Long id);

    void logout(String token);
}
