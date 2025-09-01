package com.authmat.application.authentication.token.service;

import com.authmat.application.authentication.token.builder.TokenFactory;
import com.authmat.application.authentication.token.config.TokenSigningConfig;
import com.authmat.application.authentication.token.constant.TokenType;
import com.authmat.application.authentication.token.exception.KeyInitializationException;
import com.authmat.application.authorization.constant.DefaultRole;
import com.authmat.model.publickey.PublicKeyMetadata;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;


@Service
public class TokenService {
    private final TokenFactory accessTokenFactory;
    private final TokenFactory refreshTokenFactory;


    public TokenService(
            @Qualifier(TokenSigningConfig.ACCESS_TOKEN_FACTORY_BEAN_NAME)
            TokenFactory accessTokenFactory,

            @Qualifier(TokenSigningConfig.REFRESH_TOKEN_FACTORY_BEAN_NAME)
            TokenFactory refreshTokenFactory) {
        this.accessTokenFactory = accessTokenFactory;
        this.refreshTokenFactory = refreshTokenFactory;
    }

    public String generateAccessToken(String subject, Set<String> authorities){
        Map<String, Object> extraClaims = new HashMap<>(buildClaims(subject, TokenType.ACCESS.name()));
        authorities.add(DefaultRole.BASIC.getName());
        extraClaims.put("scope", authorities);

        return accessTokenFactory.generateNewToken(
            extraClaims,
            defaultHeaderParams(accessTokenFactory
                    .currentKeyMetaData()
                    .orElseThrow(() ->
                            new KeyInitializationException("Failed to retrieve Access Token Signing Key MetaData, key was not initialized. "))));
    }

    public String generateRefreshToken(String subject){
        return refreshTokenFactory.generateNewToken(
                buildClaims(subject, TokenType.REFRESH.name()),
                defaultHeaderParams(
                        refreshTokenFactory
                                .currentKeyMetaData()
                                .orElseThrow(() -> new KeyInitializationException("Failed to retrieve Refresh Token Signing Key MetaData, key was not initialized. "))));
    }

    private Map<String, Object> buildClaims(String subject, String tokenType){
        return Map.of(
                "sub", subject,
                "aud", Set.of("authmat", "dockeep"),
                "iss", "authmat",
                "type", tokenType);
    }

    private Map<String, Object> defaultHeaderParams(PublicKeyMetadata publicKeyMetadata){
        return Map.of(
                "alg", publicKeyMetadata.getJwtAlgorithm(),
                "kid", publicKeyMetadata.getId(),
                "type", "JWT");
    }

}
