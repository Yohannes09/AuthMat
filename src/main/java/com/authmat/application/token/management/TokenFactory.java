package com.authmat.application.token.management;

import com.authmat.model.publickey.PublicKeyMetadata;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;

import java.security.PrivateKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

//@Component
@RequiredArgsConstructor
public class TokenFactory {
    private final SigningKeyManager signingKeyManager;
    private final Integer tokenValidityMinutes;

    public String generateNewToken(
            String subject,
            String audience,
            Map<String, Object> claims,
            Map<String, Object> headerParams){

        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(Duration.ofMinutes(tokenValidityMinutes));
        PrivateKey privateKey = signingKeyManager.getActiveSigningKey();

        return Jwts.builder()
                .setSubject(subject)
                .setAudience(audience)
                .setHeaderParams(headerParams)
                .setClaims(claims)
                .setIssuedAt(Date.from(issuedAt))
                .setExpiration(Date.from(expiresAt))
                .signWith(privateKey)
                .compact();
    }

    public String generateNewToken(
            Map<String, Object> claims,
            Map<String, Object> headerParams
    ){
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(Duration.ofMinutes(tokenValidityMinutes));
        PrivateKey privateKey = signingKeyManager.getActiveSigningKey();

        return Jwts.builder()
                .setHeaderParams(headerParams)
                .setClaims(claims)
                .setId(UUID.randomUUID().toString())
                .setIssuedAt(Date.from(issuedAt))
                .setExpiration(Date.from(expiresAt))
                .signWith(privateKey)
                .compact();
    }

    public Optional<PublicKeyMetadata> currentKeyMetaData(){
        return Optional.of(signingKeyManager.getCurrentKeyMetaData());
    }

}
