package com.authmat.application.authentication.service;

import com.authmat.application.authentication.LoginAttemptManager;
import com.authmat.application.authentication.config.registrationProperties;
import com.authmat.application.authentication.models.UserPrincipal;
import com.authmat.application.authentication.request.LoginRequest;
import com.authmat.application.authentication.request.RegistrationRequest;
import com.authmat.application.authentication.response.AuthenticationResponse;
import com.authmat.application.authentication.response.RegistrationResponse;
import com.authmat.application.token.exception.TokenException;
import com.authmat.application.token.model.AccessToken;
import com.authmat.application.token.model.RefreshTokenRecord;
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

import java.util.concurrent.CompletableFuture;

@Service("authenticationServiceImpl")
@Slf4j
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {
    private final UserService userService;
    private final TokenService tokenService;
    private final AuthenticationManager authenticationManager;
    private final LoginAttemptManager loginAttemptManager;
    private final registrationProperties registrationProperties;

    @Override
    public RegistrationResponse register(RegistrationRequest registrationRequest){
        UserDto user = userService.registerUser(
                registrationRequest.username(),
                registrationRequest.email(),
                registrationRequest.password(),
                registrationProperties.provider(),
                registrationProperties.providerId());

        log.info("User registered successfully: externalId={}", user.externalId());
        return new RegistrationResponse(user.externalId(), user.username(), user.email());
    }


    @Override
    public CompletableFuture<AuthenticationResponse> login(LoginRequest loginRequest) {
        String identifier = loginRequest.usernameOrEmail();

        return CompletableFuture.supplyAsync(() -> {
            if(loginAttemptManager.isBlocked(identifier)){
                log.warn("User account temporarily locked after many failed login attempts");
                throw new LockedException("User account temporarily locked after many failed login attempts");
            }

            try {
                Authentication authentication = authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(identifier, loginRequest.password())
                );

                if(!(authentication.getPrincipal() instanceof UserPrincipal principal)){
                    throw new IllegalStateException(
                            "Expected UserPrincipal but got: " + authentication.getPrincipal().getClass().getName()
                    );
                }

                principal.validateAccount();

                loginAttemptManager.loginSucceeded(identifier);
                log.debug("Successful login: {}", principal.getExternalId());

                return principal;
            }catch (AuthenticationException e){
                loginAttemptManager.loginFailed(identifier);
                throw e;
            }
        }).thenCompose(principal -> generateAuthenticationResponse(principal.getExternalId()));

    }

    // TODO:
    //  Right now exceptions from either stage will propagate as CompletionException,
    //  need to handle or exceptionally somewhere up the chain — either here or at the controller level —
    //  to map those into proper HTTP responses before they hit exception handler as unwrapped noise.
    @Override
    public CompletableFuture<AuthenticationResponse> refresh(String refreshToken){
        CompletableFuture<RefreshTokenRecord> newRefreshToken = CompletableFuture.supplyAsync(() ->
            tokenService.rotateRefreshToken(refreshToken)
                    .orElseThrow(() -> new TokenException("User must reauthenticate"))
        );

        return newRefreshToken
                .thenCompose(refreshTokenRecord ->
                        tokenService.generateAccessToken(refreshTokenRecord.externalId())
                                .thenApply(newAccessToken ->
                                        new AuthenticationResponse(newAccessToken, refreshTokenRecord.newRefreshToken())
                                )
                );
    }

    @Override
    public void logout(String token){
        // No logic here yet.
        tokenService.blackListToken(token);
    }

    private CompletableFuture<AuthenticationResponse> generateAuthenticationResponse(String subject){
        CompletableFuture<AccessToken> accessToken = tokenService.generateAccessToken(subject);
        CompletableFuture<String> refreshToken = CompletableFuture.supplyAsync(
                () -> tokenService.generateRefreshToken(subject).newRefreshToken()
        );

        return accessToken.thenCombine(refreshToken, AuthenticationResponse::new);
    }

}

