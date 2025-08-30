package com.authmat.application.authentication.token.builder;

import com.authmat.application.authentication.token.model.PublicKeyMetaData;

import java.security.PrivateKey;

public interface SigningKeyManager {
    PrivateKey getActiveSigningKey();
    void rotateSigningKey();
    PublicKeyMetaData getCurrentKeyMetaData();
}
