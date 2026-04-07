package com.authmat.application.token.controller;

import com.authmat.application.token.service.LocalJwtSigner;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class JwksController {
    // TODO: JwtSigner (interface) needs a method exposing public key, not just LocalJwtSigner
    private final LocalJwtSigner localJwtSigner;

    public JwksController(LocalJwtSigner localJwtSigner) {
        this.localJwtSigner = localJwtSigner;
    }

    @GetMapping("/.well-known/jwks.json")
    public ResponseEntity<LocalJwtSigner.PublicKeyMetadata> jwks(){
        log.debug("New JWKS request received");
        return ResponseEntity.ok(localJwtSigner.getPublicKey());
    }
}
