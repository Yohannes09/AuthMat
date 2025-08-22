package com.authmat.application.token.builder;

import java.security.PrivateKey;

public interface SigningKeyManager {
    PrivateKey getActiveSigningKey();
    void rotateSigningKey();
}
