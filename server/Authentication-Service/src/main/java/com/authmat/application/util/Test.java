package com.authmat.application.util;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class Test {
    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleOauthClientId;
    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String googleOauth2Secret;

    @PostConstruct
    public void init(){
        log.info("********* Client-Id: {}, Client Secret: {} *********", googleOauthClientId, googleOauth2Secret);
    }
}
