package com.authmat.application.token.builder;

import com.authmat.application.token.model.PublicKeyMetaData;

import java.security.PrivateKey;

public interface SigningKeyManager {
    PrivateKey getActiveSigningKey();
    void rotateSigningKey();
    PublicKeyMetaData getCurrentKeyMetaData();
}
