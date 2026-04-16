package com.authmat.application.security.registry;

import java.security.Key;

public interface VerifierRegistry {
    void put(String kid, Key key);
    Key get(String kid);
}
