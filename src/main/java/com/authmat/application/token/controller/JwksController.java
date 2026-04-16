package com.authmat.application.token.controller;

import com.authmat.application.token.model.PublicKey;
import com.authmat.application.token.service.TokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;

@RestController
@Slf4j
public class JwksController {
    private final TokenService tokenService;

    public JwksController(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @GetMapping("/.well-known/jwks.json")
    public ResponseEntity<CompletableFuture<PublicKey>> jwks(){
        log.debug("New JWKS request received");
        return ResponseEntity.ok(tokenService.getPublicKey());
    }
}
