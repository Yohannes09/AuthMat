package com.authmat.application.config;

import com.authmat.application.token.builder.SigningKeyManager;
import com.authmat.events.PublicKeyRotationEvent;
import com.authmat.model.publickey.PublicKeyMetadata;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.ZoneId;

@Configuration
@Slf4j
public class SigningKeysRotationAndPublisherConfig {
    public static final int BEAN_CREATION_DELAY_MS = 10000;

    private final SigningKeyManager accessTokenKeyManager;
    private final SigningKeyManager refreshTokenKeyManager;
    private final KafkaTemplate<String,Object> kafkaTemplate;

    public SigningKeysRotationAndPublisherConfig(
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
            fixedRateString = "#{${token.access.signing-key-rotation-rate:60}*60*1000}",
            initialDelay = BEAN_CREATION_DELAY_MS)
    public void rotateAccessTokenSigningKey(){
        log.info("Rotating Access Token signing key.");

        accessTokenKeyManager.rotateSigningKey();

        PublicKeyMetadata metadata = accessTokenKeyManager.getCurrentKeyMetaData();
        log.info(metadata.getEncodedPublicKey());

        kafkaTemplate.send(
                "token-rotation-event",
                PublicKeyRotationEvent.builder()
                        .kid(metadata.getId().toString())
                        .publicKey(metadata.getEncodedPublicKey())
                        .signingKeyAlgorithm(metadata.getKeyAlgorithm())
                        .jwtAlgorithm(metadata.getJwtAlgorithm())
                        .issuedAt(metadata.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant())
                        .issuer("authmat")
                        .build());
    }

    @Scheduled(
            fixedRateString = "#{${token.refresh.signing-key-rotation-rate:180}*60*1000}",
            initialDelay = BEAN_CREATION_DELAY_MS)
    public void rotateRefreshTokenSigningKey(){
        log.info("Rotating Refresh Token signing key.");
        refreshTokenKeyManager.rotateSigningKey();
    }
}
