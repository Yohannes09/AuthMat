package com.authmat.application.authentication.token.config;

import com.authmat.application.authentication.token.builder.SigningKeyManager;
import com.authmat.tool.events.PublicKeyRotationEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@Slf4j
public class TokenKeyRotationConfig {
    public static final int BEAN_CREATION_DELAY_MS = 10000;

    private final SigningKeyManager accessTokenKeyManager;
    private final SigningKeyManager refreshTokenKeyManager;
    private final KafkaTemplate<String,Object> kafkaTemplate;

    public TokenKeyRotationConfig(
            @Qualifier(TokenSigningConfig.ACCESS_KEY_MANAGER_BEAN_NAME)
            SigningKeyManager accessTokenKeyManager,

            @Qualifier(TokenSigningConfig.REFRESH_KEY_MANAGER_BEAN_NAME)
            SigningKeyManager refreshTokenKeyManager,

            KafkaTemplate<String,Object> kafkaTemplate) {
        this.accessTokenKeyManager = accessTokenKeyManager;
        this.refreshTokenKeyManager = refreshTokenKeyManager;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(
            fixedRateString = "#{${token.access.key-rotation-rate:10}*60*1000}",
            initialDelay = BEAN_CREATION_DELAY_MS)
    public void rotateAccessTokenSigningKey(){
        log.info("Rotating Access Token signing key.");

        accessTokenKeyManager.rotateSigningKey();

        Object id = accessTokenKeyManager.getCurrentKeyMetaData().getId();
        String key = accessTokenKeyManager.getCurrentKeyMetaData().getEncodedPublicKey();
        String issuer = "authmat";

        kafkaTemplate.send(
                "token-rotation-event",
                new PublicKeyRotationEvent(issuer, key, id.toString()));
    }

    @Scheduled(
            fixedRateString = "#{${token.refresh.key-rotation-rate:10}*60*1000}",
            initialDelay = BEAN_CREATION_DELAY_MS
    )
    public void rotateRefreshTokenSigningKey(){
        log.info("Rotating Refresh Token signing key.");
        refreshTokenKeyManager.rotateSigningKey();
    }
}
