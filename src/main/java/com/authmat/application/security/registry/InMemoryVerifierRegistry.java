package com.authmat.application.security.registry;

import java.security.Key;
import java.util.Map;


public class InMemoryVerifierRegistry implements VerifierRegistry {
    private final Map<String, Key> verifiers;

    public InMemoryVerifierRegistry(Map<String, Key> verifiers) {
        this.verifiers = verifiers;
    }


    public void put(String kid, Key key) {
        if (kid == null || kid.isBlank()) return;
        if (key == null) return;
        verifiers.putIfAbsent(kid, key);
    }

    public Key get(String kid){
        if (kid == null || kid.isBlank()) return null;
        return verifiers.getOrDefault(kid,  null);
    }

}