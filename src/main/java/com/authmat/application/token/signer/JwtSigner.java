package com.authmat.application.token.signer;

import com.authmat.application.token.model.AccessToken;
import com.authmat.application.token.jwks.PublicKey;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface JwtSigner {
    CompletableFuture<AccessToken> sign(Map<String,Object> payload, Instant expiration);
    CompletableFuture<PublicKey> getPublicKey();
}
