package com.authmat.application.token;

import com.authmat.application.token.properties.TokenProperties;
import com.authmat.application.token.exception.KeyInitializationException;
import com.authmat.application.token.model.AccessToken;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.spec.ECGenParameterSpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Component
@Profile("dev")
@Slf4j
public final class LocalJwtSigner implements JwtSigner{
    private static final String KEY_ALGORITHM = "EC";
    private static final String SIGNATURE_ALGORITHM = "ES256";
    private static final String CURVE = "P-256";

    private final KeyPair keyPair;
    private final PublicKeyMetadata keyMetadata;


    public LocalJwtSigner(TokenProperties tokenProperties){
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(KEY_ALGORITHM);
            keyPairGenerator.initialize(new ECGenParameterSpec("secp256r1"));
            keyPair = keyPairGenerator.generateKeyPair();

            String publicKey = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
            keyMetadata = PublicKeyMetadata.of(publicKey);

            log.info("Key Pair initialized successfully");
        } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException e) {
            log.error("Invalid key parameter - Failed to generate Key Pair", e);
            throw new KeyInitializationException("Invalid key parameter - Failed to generate Key Pair");
        } catch (Exception e){
            log.error("Unknown error occurred while generating Key Pair", e);
            throw new KeyInitializationException("Unknown error occurred while generating Key Pair");
        }
    }

    @Override
    public CompletableFuture<AccessToken> sign(Map<String,Object> payload, Instant expiration) {
        return CompletableFuture.supplyAsync(() -> {
                    String token = Jwts.builder()
                            .setHeaderParam("kid", keyMetadata.kid())
                            .setHeaderParam("typ", "JWT")
                            .setClaims(payload)
                            .signWith(keyPair.getPrivate(), SignatureAlgorithm.ES256)
                            .compact();
                    return AccessToken.of(token, expiration.getEpochSecond());
                }
        );
    }

    public PublicKeyMetadata getKeyMetadata(){
        return keyMetadata;
    }

    public record PublicKeyMetadata(
            String kid,
            String publicKey,
            String keyAlgorithm,
            String signatureAlgorithm,
            String curve,
            Instant createdAt
    ){
        public static PublicKeyMetadata of(String publicKey){
            return new PublicKeyMetadata(
                    UUID.randomUUID().toString(),
                    publicKey,
                    KEY_ALGORITHM,
                    SIGNATURE_ALGORITHM,
                    CURVE,
                    Instant.now()
            );
        }
    }
}
