package com.authmat.authentication.controller;

import com.authmat.authentication.component.RoleProvider;
import com.authmat.authentication.entity.Role;
import io.sentry.Sentry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// As the name suggests, not meant for production use, just quick tests.

@RestController
@Slf4j
@RequestMapping("/dummy")
@RequiredArgsConstructor
public class DummyController {
    private final RoleProvider roleProvider;


    @GetMapping("/{role}")
    public ResponseEntity<Role> getRole(@PathVariable String role){
        log.info("fetching role...");
        return ResponseEntity.ok(roleProvider.findRole(role));
    }

    @GetMapping("/boom")
    public ResponseEntity<String> boom() {
        try {
            throw new RuntimeException("Simulated failure");
        }catch (Exception e){
            Sentry.captureException(e);
        }
        return ResponseEntity.ok("hello");
    }

}
