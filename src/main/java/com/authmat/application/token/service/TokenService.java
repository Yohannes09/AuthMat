package com.authmat.application.token.service;

import com.authmat.application.token.config.TokenProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.kms.KmsAsyncClient;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;


@Service
@RequiredArgsConstructor
public class TokenService {
    private static final String BLACKLISTED_TOKEN_KEY_PREFIX = "blacklist:jti:";
    private static final String REFRESH_TOKEN_KEY_PREFRIX = "refresh:token:";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final RedisTemplate<String,String> redisTemplate;
    private final KmsAsyncClient kmsClient;
    private final TokenProperties tokenProperties;

    public String generateAccessToken(String subject){
        Instant now = Instant.now();
        String jti = UUID.randomUUID().toString();

        Map<String,Object> header = Map.of(
                "alg", "RS256",
                "type", "JWT",
                "kid", tokenProperties.kmsKeyId()
        );
        Map<String,Object> claims = (Map.of(
                "sub", subject,
                "iss", tokenProperties.issuer(),
                "aud", tokenProperties.audience(),
                "iat", now.getEpochSecond(),
                "exp", now.plus(tokenProperties.accessTokenTtl()).getEpochSecond(),
                "jti", jti,
                "type", "ACCESS"
        ));



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
