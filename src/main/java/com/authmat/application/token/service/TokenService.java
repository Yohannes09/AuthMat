package com.authmat.application.token.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;


@Service
@RequiredArgsConstructor
public class TokenService {
    private static final String BLACKLISTED_TOKEN_KEY_PREFIX = "blacklist:token:";
    private static final String REFRESH_TOKEN_KEY_PREFRIX = "refresh:token:";

    private final RedisTemplate<String,String> redisTemplate;


    public String generateAccessToken(String subject){
        Map<String,Object> claims = Map.of(
                "sub", subject,
                "iss", "authmat",
                "aud", "authmat-platform", // eventually ill figure this out
                "iat", "",
                "exp", "",
                "jti", "",
                "type", ""
        );

        Map<String,Object> header = Map.of(
                "alg", "",
                "kid", "",
                "type", "JWT"
        );

        return null;
    }

    // Refresh token will no longer be a signed JWT and will be stored in Redis
    public String generateRefreshToken(String subject){
        Map<String,Object> payload = Map.of(
                "userId","usr_a1b2c3",
                "issuedAt","...",
                "expiresAt","...",
                "rotationCount",""
        );
        return null;
    }

    public void blackListToken(String token){
        redisTemplate.opsForValue().set(BLACKLISTED_TOKEN_KEY_PREFIX + token, token);
    }

    public boolean isBlacklisted(String token){
        return redisTemplate.hasKey(BLACKLISTED_TOKEN_KEY_PREFIX + token);
    }

}
