package com.authmat.application.util;

import com.authmat.application.authorization.entity.Role;
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
    private final DefaultAuthoritiesInitializer defaultAuthoritiesInitializer;


    @GetMapping("/{role}")
    public ResponseEntity<Role> getRole(@PathVariable String role){
        log.info("fetching role...");
        return null;//ResponseEntity.ok(defaultRolesAndPermissionsInitializer.findRole(role));
    }

    @GetMapping("/boom")
    public ResponseEntity<String> boom() {
//        try {
//            throw new RuntimeException("Simulated failure");
//        }catch (Exception e){
//            Sentry.captureException(e);
//        }4
        return ResponseEntity.ok("hello");
    }

}
