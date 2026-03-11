package com.authmat.application.authentication.service;

import com.authmat.application.authentication.response.AuthenticationResponse;
import com.authmat.application.authentication.request.LoginRequest;
import com.authmat.application.authentication.request.RegistrationRequest;
import com.authmat.application.authentication.models.UserPrincipal;
import com.authmat.application.authentication.response.RegistrationResponse;

public interface AuthenticationService {
    AuthenticationResponse login(LoginRequest loginRequest);

    RegistrationResponse register(RegistrationRequest registrationRequest);

    AuthenticationResponse refresh(UserPrincipal userPrincipal);

    void logout(String token);
}
