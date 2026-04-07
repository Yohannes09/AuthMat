package com.authmat.application.token;

import com.authmat.application.token.model.AccessToken;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface JwtSigner {
    CompletableFuture<AccessToken> sign(Map<String,Object> payload, Instant expiration);
}
