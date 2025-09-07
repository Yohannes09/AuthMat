package com.authmat.application.authentication.service;

import com.authmat.application.authentication.dto.AuthenticationResponse;
import com.authmat.application.authentication.dto.LoginRequest;
import com.authmat.application.authentication.dto.RegistrationRequest;
import com.authmat.application.authentication.models.UserPrincipal;

public interface AuthenticationService {
    AuthenticationResponse login(LoginRequest loginRequest);

    boolean register(RegistrationRequest registrationRequest);

    AuthenticationResponse refresh(UserPrincipal userPrincipal);

    void logout(String token);
}
