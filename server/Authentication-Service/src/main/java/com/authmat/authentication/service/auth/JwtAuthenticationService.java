package com.authmat.authentication.service.auth;

import com.authmat.authentication.component.LoginAttemptManager;
import com.authmat.authentication.component.TokenProvider;
import com.authmat.authentication.util.Mapper;
import com.authmat.authentication.dto.UserDto;
import com.authmat.authentication.dto.authentication.LoginRequest;
import com.authmat.authentication.dto.authentication.RegistrationRequest;
import com.authmat.authentication.component.UserAccountManager;
import com.authmat.authentication.dto.authentication.AuthenticationResponse;
import com.authmat.authentication.entity.model.UserPrincipal;
import com.payme.internal.security.constant.TokenRecipient;
import com.payme.internal.security.model.UserTokenSubject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service("jwtAuthenticationService")
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationService implements AuthenticationService {
    private final UserAccountManager userAccountManager;
    private final TokenProvider tokenProvider;
    private final AuthenticationManager authenticationManager;
    private final LoginAttemptManager loginAttemptManager;

    @Override
    @Transactional
    public void register(RegistrationRequest registrationRequest) {
        userAccountManager.createNewUser(
                registrationRequest.username(),
                registrationRequest.email(),
                registrationRequest.password()
        );

        log.info("Successful registration: {}", registrationRequest.username());
    }


    @Override
    public AuthenticationResponse login(LoginRequest loginRequest) {
        String identifier = loginRequest.usernameOrEmail();
        if(loginAttemptManager.isBlocked(identifier)){
            throw new LockedException("User account temporarily locked due to multiple failed login attempts.");
        }

        try {
            UserPrincipal user;
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.usernameOrEmail(), loginRequest.password()
                    )
            );

            Object principal = authentication.getPrincipal();
            if(principal instanceof UserPrincipal u)
                user =  u;
            else
                throw new IllegalStateException("Error - Incompatible types\n -Expected: User\n -Returned: " + principal.getClass());

            log.info("Successful login: {}", user.getId());
            loginAttemptManager.loginSucceeded(identifier);
            return generateAuthenticationResponse(Mapper.principalToDto(user));
        } catch (AuthenticationException e) {
            loginAttemptManager.loginFailed(identifier);
            throw e;
        }

    }


    @Override
    public AuthenticationResponse refresh(Long id){
        UserDto user = userAccountManager.findById(id);
        validateUserAccount(user);

        log.info("Token refresh: {}", user.getId());
        return generateAuthenticationResponse(user);
    }


    @Override
    public void logout(String token){
        // No logic here yet.
    }


    private AuthenticationResponse generateAuthenticationResponse(UserDto user){
        Long id = user.getId();
        Set<String> roles = user.getRoles();

        String accessToken = tokenProvider.generateAccessToken(
                new UserTokenSubject(id.toString(), roles), TokenRecipient.USER
        );

        String refreshToken = tokenProvider.generateRefreshToken(
                new UserTokenSubject(id.toString(), roles), TokenRecipient.USER
        );

        return AuthenticationResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .usernameOrEmail(user.getUsername())
                .userId(id)
                .build();
    }


    private void validateUserAccount(UserDto user) {
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

