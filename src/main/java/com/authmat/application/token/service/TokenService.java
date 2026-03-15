package com.authmat.application.token.service;

import com.authmat.application.token.config.TokenProperties;
import com.authmat.application.token.exception.TokenException;
import com.authmat.application.token.model.AccessToken;
import com.authmat.application.token.model.RefreshTokenRecord;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.kms.KmsAsyncClient;
import software.amazon.awssdk.services.kms.model.SignRequest;
import software.amazon.awssdk.services.kms.model.SigningAlgorithmSpec;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * DOCS:
 * <a href="https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/java_kms_code_examples.html">...</a>*/
@Service
@Slf4j
@RequiredArgsConstructor
public class TokenService {
    private static final String BLACKLISTED_TOKEN_PREFIX = "blacklist:jti:";
    private static final String REFRESH_TOKEN_PREFIX = "refresh:token:";
    private static final Base64.Encoder B64URL = Base64.getUrlEncoder().withoutPadding();
    private static final SecureRandom SECURE_RANDOM =  new SecureRandom(); // create an instance of this to avoid reinitializing the RNG
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final RedisTemplate<String,String> redisTemplate;
    private final KmsAsyncClient kmsClient;
    private final TokenProperties tokenProperties;

    /*
     * NOTES:
     *  - UTF-8 is a character encoding standard used to represent text (letters, symbols, emojis)
     *    as binary data (bytes)
     *
     *
     * SHA-256 is used to produce a fixed-size digest of the signingInput(header+payload) before
     * cryptographic operation happens.
     *
     * ECDSA provides the asymmetric property. KMS uses ECC_NIST_P256 private key to sign the
     * SHA-256 digest
     *
     * FLOW:
     * header.payload  →  SHA-256 digest  →  ECDSA sign with private key  →  signature*/
    public CompletableFuture<AccessToken> generateAccessToken(String subject){
        Instant now = Instant.now();
        Instant expiresIn = now.plus(tokenProperties.accessTokenTtl());
        String jti = UUID.randomUUID().toString();

        String header = encodeJson(Map.of(
                "alg", "ES256", // could this be in tokenProperties?
                "typ", "JWT",
                "kid", tokenProperties.kmsKeyId()));

        String payload = encodeJson(Map.of(
                "sub", subject,
                "iss", tokenProperties.issuer(),
                "aud", tokenProperties.audience(),
                "iat", now.getEpochSecond(),
                "exp", expiresIn.getEpochSecond(),
                "jti", jti,
                "type", "ACCESS"));

        String signingInput = header + "." + payload;
        byte[] signingInputBytes = signingInput.getBytes(StandardCharsets.UTF_8);
        SdkBytes messageBytes = SdkBytes.fromByteArray(signingInputBytes);

        SignRequest signRequest = SignRequest.builder()
                .keyId(tokenProperties.kmsKeyId())
                .message(messageBytes)
                .signingAlgorithm(SigningAlgorithmSpec.ECDSA_SHA_256)
                .build();

        return kmsClient.sign(signRequest)
                .thenApply(response -> {
                    byte[] extractedDerBytes = response.signature().asByteArray();
                    byte[] joseEcdsaFormattedBytes = derToJoseEcdsa(extractedDerBytes);
                    String encodedJwtSignature = B64URL.encodeToString(joseEcdsaFormattedBytes);


                    return AccessToken.of(
                            signingInput + "." + encodedJwtSignature,
                            expiresIn.getEpochSecond()
                    );
                });
    }


    // getEpochSecond() is easier to parse on retrieval, i.e., Instant.ofEpochSecond(Long.parseLong(value))
    // And timezone unambiguous
    public RefreshTokenRecord generateRefreshToken(String subject){
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

        return new RefreshTokenRecord(token, subject, now);
    }

    public Optional<RefreshTokenRecord> rotateRefreshToken(String oldToken){
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

        RefreshTokenRecord newToken = generateRefreshToken(subject);
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


    // TODO: Most of what is below could be its own class
    private String encodeJson(Map<String,Object> claims){
        try {
            byte[] json = MAPPER.writeValueAsBytes(claims);
            return B64URL.encodeToString(json);
        } catch (JsonProcessingException e) {
            throw new TokenException("Failed to serialize JWT segment", e);
        }
    }

    /**
     * Converts a DER-encoded ECDSA signature (what KMS returns) to the
     * JOSE/JWT-required P1363 format (fixed-width R||S concatenation).
     *
     * KMS always returns DER. If you skip this conversion, your JWTs will
     * fail verification on every standard JWT library.
     */
    private byte[] derToJoseEcdsa(byte[] der) {
        int offset = 2;

        // handle long-form length
        if ((der[1] & 0xFF) > 0x80) {
            offset += (der[1] & 0x7F);
        }

        if (der[offset] != 0x02) {
            throw new IllegalArgumentException("Invalid DER: expected INTEGER tag for R");
        }
        int rLen = der[offset + 1] & 0xFF;
        byte[] r = Arrays.copyOfRange(der, offset + 2, offset + 2 + rLen);

        offset += 2 + rLen;

        if (der[offset] != 0x02) {
            throw new IllegalArgumentException("Invalid DER: expected INTEGER tag for S");
        }
        int sLen = der[offset + 1] & 0xFF;
        byte[] s = Arrays.copyOfRange(der, offset + 2, offset + 2 + sLen);

        byte[] result = new byte[64];
        copyToFixedWidth(r, result, 0);
        copyToFixedWidth(s, result, 32);
        return result;
    }

    private void copyToFixedWidth(byte[] src, byte[] dst, int dstOffset) {
        int srcOffset = (src.length > 32 && src[0] == 0x00) ? 1 : 0;
        int copyLen   = Math.min(src.length - srcOffset, 32);
        System.arraycopy(src, srcOffset, dst, dstOffset + (32 - copyLen), copyLen);
    }

    private Map<String, Object> parseAccessToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) throw new IllegalArgumentException("Invalid JWT structure");

            byte[] payloadBytes = Base64.getUrlDecoder().decode(parts[1]);
            return MAPPER.readValue(payloadBytes, new TypeReference<Map<String, Object>>() {});
        } catch (IOException e) {
            throw new TokenException("Failed to parse access token", e);
        }
    }

}