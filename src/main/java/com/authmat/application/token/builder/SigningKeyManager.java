package com.authmat.application.token.builder;

import com.authmat.model.publickey.PublicKeyMetadata;

import java.security.PrivateKey;

public interface SigningKeyManager {
    PrivateKey getActiveSigningKey();
    void rotateSigningKey();
    PublicKeyMetadata getCurrentKeyMetaData();
}
