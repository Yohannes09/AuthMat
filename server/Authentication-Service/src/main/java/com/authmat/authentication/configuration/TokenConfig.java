package com.authmat.authentication.configuration;

import com.payme.token.management.TokenConfigurationProperties;
import com.payme.token.management.secured.SigningKeyManager;
import com.payme.token.management.secured.TokenFactory;
import com.payme.token.model.PublicKeyRecordJpa;
import com.payme.token.persistence.PublicKeyHistory;
import com.payme.token.persistence.PublicKeyHistoryImp;
import com.payme.token.persistence.PublicKeyStore;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.util.concurrent.ConcurrentLinkedDeque;

@Configuration
@EnableJpaRepositories(basePackages = {
        "com.payme.authentication.repository",
        "com.payme.token.persistence"
})
@EntityScan(basePackages = {
        "com.payme.authentication.entity",
        "com.payme.token.model"
})
public class TokenConfig {

    @Bean
    public SigningKeyManager<PublicKeyRecordJpa> signingKeyManager(
            PublicKeyStore<PublicKeyRecordJpa> publicKeyStore,
            TokenConfigurationProperties tokenConfigurationProperties,
            PublicKeyHistory publicKeyHistory,
            SigningKeyManager.KeyFactory<PublicKeyRecordJpa> publicKeyFactory
    ){
        return new SigningKeyManager<>(
                publicKeyStore,
                tokenConfigurationProperties,
                publicKeyHistory,
                publicKeyFactory
        );

    }

    @Bean
    public TokenConfigurationProperties tokenConfigurationProperties(){
        return new TokenConfigurationProperties();
    }

    @Bean
    public PublicKeyHistory publicKeyHistory(){
        return new PublicKeyHistoryImp(20, new ConcurrentLinkedDeque<>());
    }

    @Bean
    public TokenFactory<PublicKeyRecordJpa> tokenFactory(
            SigningKeyManager<PublicKeyRecordJpa> signingKeyManager,
            TokenConfigurationProperties tokenConfigurationProperties
    ){
        return new TokenFactory<>(signingKeyManager, tokenConfigurationProperties);
    }

    @Bean
    public SigningKeyManager.KeyFactory<PublicKeyRecordJpa> publicKeyFactory(){
        return (publicKey, algorithm, issuedAt, expiresAt) ->
                PublicKeyRecordJpa.builder()
                        .publicKey(publicKey)
                        .signatureAlgorithm(algorithm)
                        .createdAt(issuedAt)
                        .expiresAt(expiresAt)
                        .build();
    }

}
