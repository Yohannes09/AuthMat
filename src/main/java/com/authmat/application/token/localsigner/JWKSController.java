package com.authmat.application.token.localsigner;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("dev")
@RequestMapping("/jwks")
@Slf4j
public class JWKSController {
    private final LocalJwtSigner localJwtSigner;

    public JWKSController(LocalJwtSigner localJwtSigner) {
        this.localJwtSigner = localJwtSigner;
    }

    @GetMapping
    public ResponseEntity<LocalJwtSigner.PublicKeyMetadata> publicKey(){
        log.info("New JWKS request received");
        return ResponseEntity.ok(localJwtSigner.getKeyMetadata());
    }
}
