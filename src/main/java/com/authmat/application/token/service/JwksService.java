package com.authmat.application.token.service;

import com.authmat.application.token.model.PublicKey;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.CompletableFuture;

public class JwksService {
    private static final String PUBLIC_KEY_PREFIX = "jwks:publickey:";

    // TODO: need a RedisTemplate bean of this type
    private final RedisTemplate<String, PublicKey> redisTemplate;
    private final TokenService tokenService;

    public JwksService(RedisTemplate<String, PublicKey> redisTemplate, TokenService tokenService) {
        this.redisTemplate = redisTemplate;
        this.tokenService = tokenService;
    }

    // TODO:
    public CompletableFuture<PublicKey> getPublicKey(){
        return tokenService.getPublicKey();
    }
}
