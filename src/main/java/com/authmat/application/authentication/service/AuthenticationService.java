package com.authmat.application.authentication.service;

import com.authmat.application.authentication.request.LoginRequest;
import com.authmat.application.authentication.request.RegistrationRequest;
import com.authmat.application.authentication.response.AuthenticationResponse;
import com.authmat.application.authentication.response.RegistrationResponse;

import java.util.concurrent.CompletableFuture;

public interface AuthenticationService {
    CompletableFuture<AuthenticationResponse> login(LoginRequest loginRequest);

    RegistrationResponse register(RegistrationRequest registrationRequest);

    CompletableFuture<AuthenticationResponse> refresh(String refreshToken);

    void logout(String token);

    CompletableFuture<AuthenticationResponse> generateAuthenticationResponse(String id);
}
