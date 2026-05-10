package com.authmat.application.authentication.service;

import com.authmat.application.authentication.response.AuthenticationResponse;
import com.authmat.application.security.properties.ServiceProperties;
import com.authmat.application.token.TokenService;
import com.authmat.application.authentication.exception.UnknownServiceIdentityException;
import com.authmat.application.token.model.AccessToken;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class IdentityProviderService {
    private final ServiceProperties properties;
    private final TokenService tokenService;

    public IdentityProviderService(ServiceProperties properties, TokenService tokenService) {
        this.properties = properties;
        this.tokenService = tokenService;
    }

    public CompletableFuture<AuthenticationResponse> authenticate(String spiffeId) {
        ServiceProperties.ServiceDefinition definition = properties.services().get(spiffeId);
        if(definition == null){ throw new UnknownServiceIdentityException("spiffeId not found"); }

        CompletableFuture<AccessToken> accessToken = tokenService.generateAccessToken(spiffeId);
        CompletableFuture<String> refreshToken = CompletableFuture.supplyAsync(() -> tokenService
                .generateRefreshToken(spiffeId).newRefreshToken());

        return accessToken.thenCombine(refreshToken, AuthenticationResponse::new);
    }
}
