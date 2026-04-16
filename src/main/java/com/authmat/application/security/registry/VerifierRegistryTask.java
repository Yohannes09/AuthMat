package com.authmat.application.security.registry;

import com.authmat.application.token.service.TokenService;
import com.authmat.validation.TokenResolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.concurrent.CompletionException;

@Slf4j
@Component
public class VerifierRegistryTask {
    private static final TokenResolver RESOLVER = new TokenResolver();

    private final VerifierRegistry verifierRegistry;
    private final TokenService tokenService;

    public VerifierRegistryTask(VerifierRegistry verifierRegistry, TokenService tokenService) {
        this.verifierRegistry = verifierRegistry;
        this.tokenService = tokenService;
    }


    @Scheduled(fixedRate = 1000 * 60 * 5)
    public void run(){
        tokenService.getPublicKey()
                .thenAccept(pk -> {
                    try {
                        if (pk.kid() == null || pk.kid().isBlank()) {
                            throw new CompletionException(new IllegalStateException("kid provided was either null or blank"));
                        }

                        if(verifierRegistry.get(pk.kid()) == null) {
                            Key key = RESOLVER.buildKey(pk.publicKey(), pk.keyAlgorithm());
                            verifierRegistry.put(pk.kid(), key);
                        }

                    } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                        throw new CompletionException("Failed to load Public Key", e);
                    }
                })
                .exceptionally(e -> {
                    log.error("VerifierRegistryTask failed to add key to registry.", e);
                    return null;
                });
    }

}
