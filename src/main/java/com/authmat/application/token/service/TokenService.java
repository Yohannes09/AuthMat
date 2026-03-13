package com.authmat.application.token.service;

import com.authmat.application.token.config.TokenProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.kms.KmsAsyncClient;
import software.amazon.awssdk.services.kms.model.SignRequest;
import software.amazon.awssdk.services.kms.model.SigningAlgorithmSpec;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * DOCS:
 * <a href="https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/java_kms_code_examples.html">...</a>*/
@Service
@RequiredArgsConstructor
public class TokenService {
    private static final String BLACKLISTED_TOKEN_KEY_PREFIX = "blacklist:jti:";
    private static final String REFRESH_TOKEN_KEY_PREFRIX = "refresh:token:";
    private static final Base64.Encoder B64URL = Base64.getUrlEncoder().withoutPadding();
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
    public CompletableFuture<String> generateAccessToken(String subject){
        Instant now = Instant.now();
        String jti = UUID.randomUUID().toString();

        //todo: replace alg value with the actual algorithm being used
        String header = encodeJson(Map.of(
                "alg", "RS256",
                "typ", "JWT",
                "kid", tokenProperties.kmsKeyId()));

        String payload = encodeJson(Map.of(
                "sub", subject,
                "iss", tokenProperties.issuer(),
                "aud", tokenProperties.audience(),
                "iat", now.getEpochSecond(),
                "exp", now.plus(tokenProperties.accessTokenTtl()).getEpochSecond(),
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
                    return signingInput + "." + encodedJwtSignature;
                });
    }

    // Refresh token will no longer be a signed JWT and will be stored in Redis
    public String generateRefreshToken(String subject){
        Map<String,Object> payload = Map.of(
                "userId","usr_a1b2c3",
                "issuedAt","",
                "expiresAt","",
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


    private String encodeJson(Map<String,Object> claims){
        try {
            byte[] json = MAPPER.writeValueAsBytes(claims);
            return B64URL.encodeToString(json);
        } catch (JsonProcessingException e) {
            // TODO: create and throw a custom runtime exception
            throw new RuntimeException(e);
        }
    }

    // TODO: understand everything below, jwt signing abstraction (use interface with method sign()), move below methods into an aws kms specific signer
    /**
     * Converts a DER-encoded ECDSA signature (what KMS returns) to the
     * JOSE/JWT-required P1363 format (fixed-width R||S concatenation).
     *
     * KMS always returns DER. If you skip this conversion, your JWTs will
     * fail verification on every standard JWT library.
     */
    private byte[] derToJoseEcdsa(byte[] der) {
        // DER structure: 0x30 <len> 0x02 <rLen> <r> 0x02 <sLen> <s>
        int offset = 2; // skip 0x30 and total length byte(s)
        if ((der[1] & 0xFF) > 0x80) offset += (der[1] & 0x7F); // handle long-form length

        // Parse R
        if (der[offset] != 0x02) throw new IllegalArgumentException("Invalid DER: expected INTEGER tag for R");
        int rLen = der[offset + 1] & 0xFF;
        byte[] r = Arrays.copyOfRange(der, offset + 2, offset + 2 + rLen);
        offset += 2 + rLen;

        // Parse S
        if (der[offset] != 0x02) throw new IllegalArgumentException("Invalid DER: expected INTEGER tag for S");
        int sLen = der[offset + 1] & 0xFF;
        byte[] s = Arrays.copyOfRange(der, offset + 2, offset + 2 + sLen);

        // Pad/trim to 32 bytes each (P-256 coordinate size)
        byte[] result = new byte[64];
        copyToFixedWidth(r, result, 0,  32);
        copyToFixedWidth(s, result, 32, 32);
        return result;
    }

    private void copyToFixedWidth(byte[] src, byte[] dst, int dstOffset, int width) {
        // DER integers are signed — a leading 0x00 may be present if high bit is set
        int srcOffset = (src.length > width && src[0] == 0x00) ? 1 : 0;
        int copyLen   = Math.min(src.length - srcOffset, width);
        System.arraycopy(src, srcOffset, dst, dstOffset + (width - copyLen), copyLen);
    }

}
