package com.authmat.application.authentication.controller;

import com.authmat.application.authentication.response.AuthenticationResponse;
import com.authmat.application.authentication.service.IdentityProviderService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;

@RestController
public class IdentityProviderController {
    private final IdentityProviderService identityProvider;

    public IdentityProviderController(IdentityProviderService identityProvider) {
        this.identityProvider = identityProvider;
    }

    @GetMapping("/auth/service/token")
    public ResponseEntity<CompletableFuture<AuthenticationResponse>> authenticateService(HttpServletRequest request) {
        String spiffeId = (String) request.getAttribute("mtls.client.spiffe");

        return ResponseEntity.ok(identityProvider.authenticate(spiffeId));
    }

}
