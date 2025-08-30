package com.authmat.application.authentication.token.model;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@AllArgsConstructor
@Getter
@Builder
public class PublicKeyMetaDataImp implements PublicKeyMetaData{
    @NotNull
    private UUID id;

    @NotEmpty
    @Size(min = 2048, message = "Encoded public key must be at least 2048 characters long.")
    private String encodedPublicKey;

    @NotEmpty
    private String keyAlgorithm;

    @NotEmpty
    private String jwtAlgorithm;

    @NotNull
    private LocalDateTime createdAt;

    private LocalDateTime expiresAt;

    private LocalDateTime revokedAt;

    public PublicKeyMetaDataImp(String encodedPublicKey, String keyAlgorithm, String jwtAlgorithm){
        this.id = UUID.randomUUID();
        this.encodedPublicKey = encodedPublicKey;
        this.keyAlgorithm = keyAlgorithm;
        this.jwtAlgorithm = jwtAlgorithm;
        this.createdAt = LocalDateTime.now();
    }

}
