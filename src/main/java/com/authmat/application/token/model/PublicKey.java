package com.authmat.application.token.model;

import java.time.Instant;
import java.util.UUID;

public record PublicKey(
        String kid,
        String publicKey,
        String keyAlgorithm,
        String signatureAlgorithm,
        String curve,
        Instant createdAt
){
    public static PublicKey of(
            String publicKey,
            String keyAlgorithm,
            String signatureAlgorithm,
            String curve){
        return new PublicKey(
                UUID.randomUUID().toString(),
                publicKey,
                keyAlgorithm,
                signatureAlgorithm,
                curve,
                Instant.now()
        );
    }
}
