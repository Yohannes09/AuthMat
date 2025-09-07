package com.authmat.application.config;

import com.authmat.application.token.builder.SigningKeyManager;
import com.authmat.application.token.builder.SigningKeyManagerImp;
import com.authmat.application.token.builder.TokenFactory;
import com.authmat.application.token.history.PublicKeyHistory;
import com.authmat.application.token.history.PublicKeyHistoryImp;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.repository.cdi.Eager;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.concurrent.ConcurrentLinkedDeque;

@Configuration
@EnableScheduling
@Eager
@Slf4j
public class TokenSigningConfig {
    public static final String ACCESS_KEY_MANAGER_BEAN_NAME = "accessKeyManager";
    public static final String ACCESS_KEY_HISTORY_BEAN_NAME = "accessKeyHistory";
    public static final String ACCESS_TOKEN_FACTORY_BEAN_NAME = "accessTokenFactory";

    public static final String REFRESH_KEY_MANAGER_BEAN_NAME = "refreshKeyManager";
    public static final String REFRESH_KEY_HISTORY_BEAN_NAME = "refreshKeyHistory";
    public static final String REFRESH_TOKEN_FACTORY_BEAN_NAME = "refreshTokenFactory";


    @Bean(ACCESS_KEY_MANAGER_BEAN_NAME)
    public SigningKeyManager accessTokenSigningKeyManager(
            @Qualifier(ACCESS_KEY_HISTORY_BEAN_NAME)PublicKeyHistory publicKeyHistory,
            @Value("${token.access.key-size:2048}") int keySize,
            @Value("${token.access.key-algorithm:RSA}") String keyAlgorithm,
            @Value("${token.access.jwt-algorithm:RS256}") String jwtAlgorithm){
        return new SigningKeyManagerImp(
                publicKeyHistory,
                keyAlgorithm,
                jwtAlgorithm,
                keySize);
    }

    @Bean(REFRESH_KEY_MANAGER_BEAN_NAME)
    public SigningKeyManager refreshTokenSigningKeyManager(
            @Qualifier(REFRESH_KEY_HISTORY_BEAN_NAME)PublicKeyHistory publicKeyHistory,
            @Value("${token.refresh.key-size:2048}") int keySize,
            @Value("${token.refresh.key-algorithm:RSA}") String keyAlgorithm,
            @Value("${token.refresh.jwt-algorithm:RS256}") String jwtAlgorithm){
        return new SigningKeyManagerImp(
                publicKeyHistory,
                keyAlgorithm,
                jwtAlgorithm,
                keySize);
    }

    @Bean(ACCESS_KEY_HISTORY_BEAN_NAME)
    public PublicKeyHistory accessTokenPublicKeyHistory(
            @Value("${token.access.key-history-trace:10}") Integer maxKeysTraced){
        return new PublicKeyHistoryImp(
                maxKeysTraced,
                new ConcurrentLinkedDeque<>());
    }

    @Bean(REFRESH_KEY_HISTORY_BEAN_NAME)
    public PublicKeyHistory refreshTokenPublicKeyHistory(
            @Value("${token.refresh.key-history-trace:10}") Integer maxKeysTraced){
        return new PublicKeyHistoryImp(
                maxKeysTraced,
                new ConcurrentLinkedDeque<>());
    }

    @Bean(ACCESS_TOKEN_FACTORY_BEAN_NAME)
    public TokenFactory accessTokenFactory(
            @Qualifier(ACCESS_KEY_MANAGER_BEAN_NAME) SigningKeyManager signingKeyManager,
            @Value("${token.access.token-validity-minutes}") Integer accessTokenValidityMinutes){
        return new TokenFactory(
                signingKeyManager,
                accessTokenValidityMinutes);
    }

    @Bean(REFRESH_TOKEN_FACTORY_BEAN_NAME)
    public TokenFactory refreshTokenFactory(
            @Qualifier(REFRESH_KEY_MANAGER_BEAN_NAME) SigningKeyManager signingKeyManager,
            @Value("${token.refresh.token-validity-minutes}") Integer refreshTokenValidityMinutes){
        return new TokenFactory(
                signingKeyManager,
                refreshTokenValidityMinutes);
    }

}
