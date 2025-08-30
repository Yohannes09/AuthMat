package com.authmat.application.authentication.service;

import com.authmat.application.authentication.dto.AuthenticationResponse;
import com.authmat.application.authentication.dto.LoginRequest;
import com.authmat.application.authentication.dto.RegistrationRequest;
import com.authmat.application.users.model.UserPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;

public interface AuthenticationService {
    AuthenticationResponse login(LoginRequest loginRequest);

    void register(RegistrationRequest registrationRequest, PasswordEncoder passwordEncoder);

    AuthenticationResponse refresh(UserPrincipal userPrincipal);

    void logout(String token);
}
