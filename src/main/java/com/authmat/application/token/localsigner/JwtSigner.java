package com.authmat.application.token.localsigner;

import com.authmat.application.token.model.AccessToken;

import java.util.concurrent.CompletableFuture;

public interface JwtSigner {
    CompletableFuture<AccessToken> sign(String subject);
}
