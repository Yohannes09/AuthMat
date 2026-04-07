package com.authmat.application.token.service;

import com.authmat.application.token.exception.TokenException;
import com.authmat.application.token.model.AccessToken;
import com.authmat.application.token.model.PublicKey;
import com.authmat.application.token.properties.TokenProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.kms.KmsAsyncClient;
import software.amazon.awssdk.services.kms.model.SignRequest;
import software.amazon.awssdk.services.kms.model.SigningAlgorithmSpec;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * DOCS:
 * <a href="https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/java_kms_code_examples.html">...</a>*/
@Component
@Slf4j
@ConditionalOnProperty(name = "authmat.token.signer", havingValue = "kms")
public final class KmsJwtSigner implements JwtSigner {
    private static final Base64.Encoder B64URL = Base64.getUrlEncoder().withoutPadding();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final KmsAsyncClient kmsClient;
    private final TokenProperties tokenProperties;

    public KmsJwtSigner(KmsAsyncClient kmsClient, TokenProperties tokenProperties) {
        this.kmsClient = kmsClient;
        this.tokenProperties = tokenProperties;
    }

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
    public CompletableFuture<AccessToken> sign(Map<String,Object> payload, Instant expiration) {
        String header = encodeJson(Map.of(
                "alg", "ES256",
                "typ", "JWT",
                "kid", tokenProperties.kmsKeyId()));

        String signingInput = header + "." + encodeJson(payload);
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

                    String token = signingInput + "." + encodedJwtSignature;
                    return AccessToken.of(token, expiration.getEpochSecond());
                });
    }

    @Override
    public PublicKey getPublicKey() {
        // TODO: Fetch Public Key from KMS and cache (TokenService)
        return null;
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

}
