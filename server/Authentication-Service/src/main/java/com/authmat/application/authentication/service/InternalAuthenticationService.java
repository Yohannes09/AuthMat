package com.authmat.application.authentication.service;

import com.authmat.application.authentication.component.LoginAttemptManager;
import com.authmat.application.authentication.dto.AuthenticationResponse;
import com.authmat.application.authentication.dto.LoginRequest;
import com.authmat.application.authentication.dto.RegistrationRequest;
import com.authmat.application.authentication.token.service.TokenService;
import com.authmat.application.users.model.UserPrincipal;
import com.authmat.application.users.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service("jwtAuthenticationService")
@Slf4j
@RequiredArgsConstructor
public class InternalAuthenticationService implements AuthenticationService {
    private final UserService userService;
    private final TokenService tokenService;
    private final AuthenticationManager authenticationManager;
    private final LoginAttemptManager loginAttemptManager;


    @Override
    @Transactional
    public void register(
            RegistrationRequest registrationRequest, PasswordEncoder passwordEncoder){

        userService.createAndPublishUser(
                registrationRequest.username(),
                registrationRequest.email(),
                passwordEncoder.encode(registrationRequest.password()),
                Set.of());

        log.info("Successful registration: {}", registrationRequest.username());
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
                validateUserAccount(principal);

                log.info("Successful login: {}", principal.getId());
                return generateAuthenticationResponse(
                        principal.getId().toString(), new HashSet<>(principal.getAuthorities()));
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

    @Override
    public AuthenticationResponse refresh(UserPrincipal userPrincipal){
        validateUserAccount(userPrincipal);

        log.info("Access Token refresh: {}", userPrincipal.getId());
        return generateAuthenticationResponse(
                userPrincipal.getId().toString(), new HashSet<>(userPrincipal.getAuthorities()));
    }


    @Override
    public void logout(String token){
        // No logic here yet.
    }


    private AuthenticationResponse generateAuthenticationResponse(
            String subject, Set<GrantedAuthority> grantedAuthorities){

        Set<String> authorities = grantedAuthorities
                .stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        String accessToken = tokenService.generateAccessToken(subject, authorities);
        String refreshToken = tokenService.generateRefreshToken(subject);

        return AuthenticationResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }


    private void validateUserAccount(UserDetails user) {
        if (!user.isAccountNonLocked()) {
            throw new LockedException("Account is locked");
        }
        if (!user.isEnabled()) {
            throw new DisabledException("Account is disabled");
        }
        if (!user.isAccountNonExpired()) {
            throw new AccountExpiredException("Account has expired");
        }
        if (!user.isCredentialsNonExpired()) {
            throw new CredentialsExpiredException("Credentials have expired");
        }

    }

}

