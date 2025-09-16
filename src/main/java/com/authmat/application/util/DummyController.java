package com.authmat.application.util;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DummyController {
    @GetMapping("/ping")
    public String ping(){
        return "pong";
    }

    @GetMapping("/secured")
    public String secured(){
        return "Hello authenticated user";
    }

    @PreAuthorize("hasAuthority('ADMIN', 'SUPER_ADMIN')")
    @GetMapping("/isAdmin")
    public String isAdmin(){
        return "Hello Admin";
    }
}
