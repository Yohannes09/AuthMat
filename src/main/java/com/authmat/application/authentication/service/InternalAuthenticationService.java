package com.authmat.application.authentication.service;

import com.authmat.application.authentication.LoginAttemptManager;
import com.authmat.application.authentication.dto.AuthenticationResponse;
import com.authmat.application.authentication.dto.LoginRequest;
import com.authmat.application.authentication.dto.RegistrationRequest;
import com.authmat.application.authentication.exception.FailedAuthencticationException;
import com.authmat.application.token.service.TokenService;
import com.authmat.application.users.model.User;
import com.authmat.application.authentication.models.UserPrincipal;
import com.authmat.application.users.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;

@Service("internalAuthenticationService")
@Slf4j
@RequiredArgsConstructor
public class InternalAuthenticationService implements AuthenticationService {
    private final UserService userService;
    private final TokenService tokenService;
    private final AuthenticationManager authenticationManager;
    private final LoginAttemptManager loginAttemptManager;


    @Override
    @Transactional
    public boolean register(RegistrationRequest registrationRequest){
        boolean isUserCreated = userService.createAndPublishUser(
                User.builder()
                        .username(registrationRequest.username())
                        .email(registrationRequest.email())
                        .password(registrationRequest.password())
                        .build());

        log.info("Successful registration: {}", registrationRequest.username());
        return isUserCreated;
    }


    @Override
    public AuthenticationResponse login(LoginRequest loginRequest) {
        String identifier = Optional.of(loginRequest.usernameOrEmail()).orElseThrow();

        if(loginAttemptManager.isBlocked(identifier)){
            log.info("User {} temporarily locked for many failed login attempts.", identifier);
            throw new LockedException("User account temporarily locked due to multiple failed login attempts.");
        }

        try {
            UsernamePasswordAuthenticationToken token =
                    new UsernamePasswordAuthenticationToken(loginRequest.usernameOrEmail(), loginRequest.password());

            Authentication authentication = authenticationManager.authenticate(token);
            Object object = authentication.getPrincipal();

            if(object instanceof UserPrincipal principal) {
                loginAttemptManager.loginSucceeded(identifier);
                principal.validateAccount();

                log.info("Successful login: {}", principal.getId());
                return generateAuthenticationResponse(
                        principal.getId().toString(),
                        principal.getAuthoritiesStr());
            }

            throw new IllegalStateException("Incompatible type mapping during authentication.");

        } catch (AuthenticationException e) {
            loginAttemptManager.loginFailed(identifier);
            throw e;
        } catch (Exception e) {
            log.error("""
                    
                    Unforeseen Exception thrown during login
                    Cause: {}
                    Message: {}
                    Trace: {}
                    
                    """, e.getCause(), e.getMessage(), e.getStackTrace());
            throw e;
        }

    }

    // need to blacklist their current refresh token
    @Override                                                       //,String refreshToken
    public AuthenticationResponse refresh(UserPrincipal userPrincipal){
        try {
            userPrincipal.validateAccount();

            log.info("Access Token refresh: {}", userPrincipal.getId());
            return generateAuthenticationResponse(
                    userPrincipal.getId().toString(),
                    userPrincipal.getAuthoritiesStr());
        } catch (Exception e) {
            log.info("Failed token refresh: {}", e.getMessage());
            throw new FailedAuthencticationException("Could not reauthencticate user.");
        }
    }

    @Override
    public void logout(String token){
        // No logic here yet.
        tokenService.blackListToken(token);
    }

    public AuthenticationResponse generateAuthenticationResponse(
            String subject, Set<String> authorities){
        String accessToken = tokenService.generateAccessToken(subject, authorities);
        String refreshToken = tokenService.generateRefreshToken(subject);

        return AuthenticationResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

}

