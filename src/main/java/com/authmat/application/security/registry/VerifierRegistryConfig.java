package com.authmat.application.security.registry;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class VerifierRegistryConfig {
    @Bean
    public VerifierRegistry getVerifierRegistry() {
        return new InMemoryVerifierRegistry(new ConcurrentHashMap<>(10));
    }
}
