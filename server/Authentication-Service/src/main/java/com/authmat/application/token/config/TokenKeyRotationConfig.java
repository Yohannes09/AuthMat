package com.authmat.application.token.config;

import com.authmat.application.token.builder.SigningKeyManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@Slf4j
public class TokenKeyRotationConfig {
    private final SigningKeyManager accessTokenKeyManager;
    private final SigningKeyManager refreshTokenKeyManager;

    public TokenKeyRotationConfig(
            @Qualifier(TokenSigningConfig.ACCESS_KEY_MANAGER_BEAN_NAME)
            SigningKeyManager accessTokenKeyManager,

            @Qualifier(TokenSigningConfig.REFRESH_KEY_MANAGER_BEAN_NAME)
            SigningKeyManager refreshTokenKeyManager) {
        this.accessTokenKeyManager = accessTokenKeyManager;
        this.refreshTokenKeyManager = refreshTokenKeyManager;
    }

    @Scheduled(fixedRateString = "${token.access.key-rotation-rate:10}")
    public void rotateAccessTokenSigningKey(){

        log.info("Rotating Access Token signing key.");
        accessTokenKeyManager.rotateSigningKey();
    }

    @Scheduled(fixedRateString = "${token.refresh.key-rotation-rate:10}")
    public void rotateRefreshTokenSigningKey(){
        log.info("Rotating Refresh Token signing key.");
        refreshTokenKeyManager.rotateSigningKey();
    }
}
