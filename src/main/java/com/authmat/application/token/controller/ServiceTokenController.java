package com.authmat.application.token.controller;

import com.authmat.application.token.model.AccessToken;
import com.authmat.application.token.service.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;

@RestController
public class ServiceTokenController {
    private final TokenService tokenService;

    public ServiceTokenController(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @GetMapping("/auth/service/token")
    public ResponseEntity<CompletableFuture<AccessToken>> authenticateService(HttpServletRequest request) {
        String spiffeId = (String) request.getAttribute("mtls.client.spiffe");

        return ResponseEntity.ok(tokenService.generateServiceToken(spiffeId));
    }

}
