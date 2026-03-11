package com.authmat.application.authentication.service;

import com.authmat.application.authentication.LoginAttemptManager;
import com.authmat.application.authentication.response.AuthenticationResponse;
import com.authmat.application.authentication.request.LoginRequest;
import com.authmat.application.authentication.request.RegistrationRequest;
import com.authmat.application.authentication.exception.FailedAuthencticationException;
import com.authmat.application.authentication.models.UserPrincipal;
import com.authmat.application.authentication.response.RegistrationResponse;
import com.authmat.application.token.service.TokenService;
import com.authmat.application.users.UserService;
import com.authmat.application.users.model.UserDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
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
    public RegistrationResponse register(RegistrationRequest registrationRequest){
        //TODO: hardcoded provider strings should be from a constant or config
        UserDto user = userService.registerUser(
                registrationRequest.username(),
                registrationRequest.email(),
                registrationRequest.password(),
                "authmat_service",
                "authmat");

        log.info("User registered successfully: externalId={}, username={}",
                user.getExternalId(), user.getUsername());
        return new RegistrationResponse(user.getExternalId(), user.getUsername(), user.getEmail());
    }


    @Override
    public AuthenticationResponse login(LoginRequest loginRequest) {
        String identifier = loginRequest.usernameOrEmail();

        if(loginAttemptManager.isBlocked(identifier)){
            log.warn("User {} temporarily locked for many failed login attempts.", identifier);
            throw new LockedException("User account temporarily locked due to multiple failed login attempts.");
        }

        try {
            UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(
                            loginRequest.usernameOrEmail(), loginRequest.password());

            Authentication authentication = authenticationManager.authenticate(token);
            Object object = authentication.getPrincipal();

            if(object instanceof UserPrincipal principal) {
                loginAttemptManager.loginSucceeded(identifier);
                principal.validateAccount();

                log.debug("Successful login: {}", principal.getExternalId());
                return generateAuthenticationResponse(principal.getExternalId());
            }

            throw new IllegalStateException("Incompatible type mapping during authentication.");

        } catch (AuthenticationException e) {
            loginAttemptManager.loginFailed(identifier);
            throw e;
        } catch (Exception e) {
            log.debug("""
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
//        try {
//            userPrincipal.validateAccount();
//            log.info("Access Token refresh: {}", userPrincipal.getId());
//            return generateAuthenticationResponse(
//                    userPrincipal.getId().toString(),
//                    userPrincipal.getAuthoritiesStr());
//        } catch (Exception e) {
//            log.info("Failed token refresh: {}", e.getMessage());
//            throw new FailedAuthencticationException("Could not reauthencticate user.");
//        }
        return null;
    }

    @Override
    public void logout(String token){
        // No logic here yet.
        tokenService.blackListToken(token);
    }

    public AuthenticationResponse generateAuthenticationResponse(String subject){
        String accessToken = tokenService.generateAccessToken(subject);
        String refreshToken = tokenService.generateRefreshToken(subject);

        return new AuthenticationResponse(accessToken, refreshToken);
    }

}

