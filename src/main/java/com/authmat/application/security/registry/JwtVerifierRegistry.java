package com.authmat.application.security.registry;

import com.authmat.application.security.exception.NoVerifierForKidException;
import com.authmat.application.token.model.PublicKey;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class JwtVerifierRegistry {
    private final Map<String, PublicKey> verifiers;

    public JwtVerifierRegistry(List<PublicKey> publicKeys) {
        this.verifiers = publicKeys.stream().collect(Collectors.toMap(PublicKey::kid, Function.identity()));
    }

    public PublicKey getVerifier(String kid) {
        PublicKey publicKey = verifiers.get(kid);
        if (publicKey == null) {
            throw new NoVerifierForKidException(kid);
        }
        return publicKey;
    }
}
