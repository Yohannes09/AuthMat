package com.authmat.application.token.service;

import com.authmat.application.exception.UnknownServiceIdentityException;
import com.authmat.application.security.properties.ServiceProperties;
import com.authmat.application.token.constant.TokenType;
import com.authmat.application.token.exception.TokenException;
import com.authmat.application.token.model.AccessToken;
import com.authmat.application.token.model.PublicKey;
import com.authmat.application.token.model.RefreshToken;
import com.authmat.application.token.TokenProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class TokenService {
    private static final String BLACKLISTED_TOKEN_PREFIX = "blacklist:jti:";
    private static final String REFRESH_TOKEN_PREFIX = "refresh:token:";
    private static final Base64.Encoder B64URL = Base64.getUrlEncoder().withoutPadding();
    private static final SecureRandom SECURE_RANDOM =  new SecureRandom(); // create an instance of this to avoid reinitializing the RNG
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final RedisTemplate<String,String> redisTemplate;
    private final JwtSigner jwtSigner;
    private final TokenProperties tokenProperties;
    private final ServiceProperties serviceProperties;

    public TokenService(
            @Qualifier("strRedisTemplate") RedisTemplate<String, String> redisTemplate,
            JwtSigner jwtSigner,
            TokenProperties tokenProperties,
            ServiceProperties serviceProperties) {
        this.redisTemplate = redisTemplate;
        this.jwtSigner = jwtSigner;
        this.tokenProperties = tokenProperties;
        this.serviceProperties = serviceProperties;
    }

    public CompletableFuture<AccessToken> generateAccessToken(String subject){
        Instant now = Instant.now();
        Instant expiresIn = now.plus(tokenProperties.accessTokenTtl());
        String jti = UUID.randomUUID().toString();

        Map<String,Object> payload = Map.of(
                "sub", subject,
                "iss", tokenProperties.issuer(),
                "aud", tokenProperties.audience(),
                "iat", now.getEpochSecond(),
                "exp", expiresIn.getEpochSecond(),
                "jti", jti,
                "type", TokenType.ACCESS.name());

        return jwtSigner.sign(payload, expiresIn);
    }

    public CompletableFuture<AccessToken> generateServiceToken(String spiffeId){
        Instant now = Instant.now();
        Instant expiresIn = now.plus(tokenProperties.accessTokenTtl());
        String jti = UUID.randomUUID().toString();

        ServiceProperties.ServiceDefinition definition = serviceProperties.services().get(spiffeId);
        if(definition == null){ throw new UnknownServiceIdentityException("spiffeId not found"); }

        Map<String,Object> payload = Map.of(
                "sub", spiffeId,
                "iss", tokenProperties.issuer(),
                "aud", tokenProperties.audience(),
                "iat", now.getEpochSecond(),
                "exp", expiresIn.getEpochSecond(),
                "jti", jti,
                "type", TokenType.SERVICE.name(),
                "scope", definition.scopes());

        return jwtSigner.sign(payload, expiresIn);
    }

    public CompletableFuture<PublicKey> getPublicKey(){
        return jwtSigner.getPublicKey();
    }

    // getEpochSecond() is easier to parse on retrieval, i.e., Instant.ofEpochSecond(Long.parseLong(value))
    // And timezone unambiguous
    public RefreshToken generateRefreshToken(String subject){
        byte[] raw = new byte[32];
        SECURE_RANDOM.nextBytes(raw);

        String token = B64URL.encodeToString(raw);
        Instant now = Instant.now();
        Instant expiresAt = now.plus(tokenProperties.refreshTokenTtl());

        String key = REFRESH_TOKEN_PREFIX + token;
        Map<String,Object> payload = Map.of(
                "subject",subject,
                "issuedAt", String.valueOf(now.getEpochSecond()),
                "expiresAt",String.valueOf(expiresAt.getEpochSecond()),
                "rotationCount","0"
        );

        redisTemplate.opsForHash().putAll(key, payload);
        redisTemplate.expire(key, tokenProperties.refreshTokenTtl());

        return new RefreshToken(token, subject, now);
    }

    public Optional<RefreshToken> rotateRefreshToken(String oldToken){
        String key = REFRESH_TOKEN_PREFIX + oldToken;

        Map<Object,Object> data = redisTemplate.opsForHash().entries(key);
        if(data == null || data.isEmpty()){
            log.warn("Refresh token not found or rotated - possible replay attack");
            return Optional.empty();
        }

        String subject = (String) data.get("subject");
        int rotationCount = Integer.parseInt(
                (String) data.getOrDefault("rotationCount", "0")
        );

        redisTemplate.delete(key);

        RefreshToken newToken = generateRefreshToken(subject);
        redisTemplate.opsForHash().put(
                REFRESH_TOKEN_PREFIX + newToken,
                "rotationCount",
                String.valueOf(rotationCount + 1)
        );

        return Optional.of(newToken);
    }

    public void blackListToken(String token){
        Map<String,Object> tokenParts = parseAccessToken(token);

        String jti = (String) tokenParts.get("jti");
        // Jackson deserializes JSON numbers into Integer when the value fits in an integer range,
        // and Long when it doesn't — and you don't control which one it picks.
        // If you cast directly to Long you get a ClassCastException at runtime.
        // Number is the common parent of both Integer and Long in Java, and .longValue() is defined on Number
        Instant tokenExpiration = Instant.ofEpochSecond(
                ((Number) tokenParts.get("exp")).longValue()
        );

        Duration remainingBeforeExpiration = Duration.between(Instant.now(), tokenExpiration);

        if(remainingBeforeExpiration.isNegative() || remainingBeforeExpiration.isZero()){
            return;
        }

        redisTemplate.opsForValue().set(
                BLACKLISTED_TOKEN_PREFIX + jti,
                "1",
                remainingBeforeExpiration
        );
    }

    /**
     * Fail-closed: if Redis is down, treat the token as blacklisted.
     */
    public boolean isBlacklisted(String jti) {
        try {
            return redisTemplate.hasKey(BLACKLISTED_TOKEN_PREFIX + jti);
        } catch (Exception e) {
            log.error("Redis unavailable during blacklist check — failing closed", e);
            return true;
        }
    }

    private Map<String, Object> parseAccessToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) throw new IllegalArgumentException("Invalid JWT structure");

            byte[] payloadBytes = Base64.getUrlDecoder().decode(parts[1]);
            return MAPPER.readValue(payloadBytes, new TypeReference<>() {});
        } catch (IOException e) {
            throw new TokenException("Failed to parse access token", e);
        }
    }

}