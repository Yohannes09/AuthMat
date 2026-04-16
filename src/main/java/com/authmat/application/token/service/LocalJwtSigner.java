package com.authmat.application.token.service;

import com.authmat.application.token.exception.KeyInitializationException;
import com.authmat.application.token.model.AccessToken;
import com.authmat.application.token.model.PublicKey;
import com.authmat.application.token.properties.TokenProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.spec.ECGenParameterSpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Component
@Slf4j
@ConditionalOnProperty(name = "authmat.token.signer", havingValue = "local")
public final class LocalJwtSigner implements JwtSigner{
    private final KeyPair keyPair;
    private final CompletableFuture<PublicKey> publicKey;
    private final TokenProperties tokenProperties;


    public LocalJwtSigner(TokenProperties tokenProperties){
        try {
            this.tokenProperties = tokenProperties;

            KeyPairGenerator keyPairGenerator =
                    KeyPairGenerator.getInstance(tokenProperties.algorithm().keyAlgorithm());
            keyPairGenerator.initialize(new ECGenParameterSpec("secp256r1"));
            keyPair = keyPairGenerator.generateKeyPair();

            String publicKey = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
            this.publicKey = CompletableFuture.completedFuture(
                    PublicKey.of(
                            publicKey,
                            tokenProperties.algorithm().keyAlgorithm(),
                            tokenProperties.algorithm().signatureAlgorithm(),
                            tokenProperties.algorithm().curve()));

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
                            // TODO: CODE REVIEW THIS
                            .setHeaderParam("keyId", publicKey.thenApply(PublicKey::kid))
                            .setHeaderParam("typ", "JWT")
                            .setClaims(payload)
                            .signWith(keyPair.getPrivate(), SignatureAlgorithm.ES256)
                            .compact();
                    return AccessToken.of(token, expiration.getEpochSecond());
                }
        );
    }

    public CompletableFuture<PublicKey> getPublicKey(){
        return publicKey;
    }
}
