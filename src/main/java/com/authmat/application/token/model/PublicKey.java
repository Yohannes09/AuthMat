package com.authmat.application.token.model;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public record PublicKey(
        String kid,
        String publicKey,
        String keyAlgorithm,
        String signatureAlgorithm,
        String curve
){
    public static CompletableFuture<PublicKey> of(
            String publicKey,
            String keyAlgorithm,
            String signatureAlgorithm,
            String curve){
        return new PublicKey(
                UUID.randomUUID().toString(),
                publicKey,
                keyAlgorithm,
                signatureAlgorithm,
                curve
        );
    }
}
