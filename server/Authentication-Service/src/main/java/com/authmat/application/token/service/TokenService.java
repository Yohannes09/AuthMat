package com.authmat.application.token.service;

import com.authmat.application.token.config.TokenSigningConfig;
import com.authmat.application.token.constant.TokenType;
import com.authmat.application.token.builder.TokenFactory;
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
            @Qualifier(TokenSigningConfig.ACCESS_TOKEN_FACTORY_BEAN_NAME) TokenFactory accessTokenFactory,
            @Qualifier(TokenSigningConfig.REFRESH_TOKEN_FACTORY_BEAN_NAME) TokenFactory refreshTokenFactory
    ) {
        this.accessTokenFactory = accessTokenFactory;
        this.refreshTokenFactory = refreshTokenFactory;
    }

    public String generateAccessToken(String subject, Set<String> permissions){
        Map<String, Object> extraClaims = new HashMap<>(defaultClaims(subject, TokenType.ACCESS.name()));
        extraClaims.put("scope", permissions);

        return accessTokenFactory.generateNewToken(
            extraClaims,
            defaultHeaderParams()
        );
    }

    public String generateRefreshToken(String subject){

        return refreshTokenFactory.generateNewToken(
                defaultClaims(subject, TokenType.REFRESH.name()),
                defaultHeaderParams()
        );

    }

    private Map<String, Object> defaultClaims(String subject, String tokenType){
        return Map.of(
                "sub", subject,
                "aud", Set.of("authmat", "dockeep"),
                "iss", "authmat",
                "type", tokenType
        );
    }

    private Map<String, Object> defaultHeaderParams(){
        return Map.of(
                "alg", "",
                "kid", "",
                "type", "JWT"
        );
    }

}
