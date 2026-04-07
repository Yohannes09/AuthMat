package com.authmat.application.token.service;

import com.authmat.application.token.model.PublicKey;
import com.authmat.application.token.properties.TokenProperties;
import org.springframework.data.redis.core.RedisTemplate;

public class JwksService {
    private static final String PUBLIC_KEY_PREFIX = "jwks:publickey:";

    private final RedisTemplate<String,byte[]>  redisTemplate;
    private final JwtSigner jwtSigner;
    private final TokenProperties tokenProperties;

    public JwksService(RedisTemplate<String, byte[]> redisTemplate, JwtSigner jwtSigner, TokenProperties tokenProperties) {
        this.redisTemplate = redisTemplate;
        this.jwtSigner = jwtSigner;
        this.tokenProperties = tokenProperties;
    }


    // TODO:
    public PublicKey getPublicKey(){
        String kid = tokenProperties.kmsKeyId() != null ?
                tokenProperties.kmsKeyId() : jwtSigner.getPublicKey().kid();

        return  jwtSigner.getPublicKey();
    }
}
