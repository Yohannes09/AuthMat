package com.authmat.application.util;

import com.authmat.application.authentication.dto.AuthenticationResponse;
import com.authmat.application.authentication.service.InternalAuthenticationService;
import com.authmat.application.authorization.constant.DefaultRole;
import com.authmat.application.config.TokenSigningConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Configuration
@DependsOn({"tokenSigningConfig", TokenSigningConfig.ACCESS_KEY_MANAGER_BEAN_NAME,  TokenSigningConfig.REFRESH_KEY_MANAGER_BEAN_NAME})
@Profile("dev")
@RequiredArgsConstructor
@Slf4j(topic = "DEFAULT_USER_GENERATOR")
public class DefaultUserGenerator {
    private final InternalAuthenticationService internalAuthenticationService;


    @Scheduled(initialDelay = 10000)
    public void createDefaultUsers(){
        log.info("Creating default Admin and User accounts.");
        try {
            String tempAdminUsername = UUID.randomUUID().toString();
            String tempUserUsername = UUID.randomUUID().toString();

            Set<String> adminAuthorities = Set.of(
                    DefaultRole.SUPER_ADMIN,
                    DefaultRole.ADMIN,
                    DefaultRole.ELEVATED)
                    .stream()
                    .flatMap(defaultRole -> defaultRole.getAuthorities().stream())
                    .collect(Collectors.toSet());

            Set<String> userRoles = Set.of(DefaultRole.BASIC).stream()
                    .flatMap(defaultRole -> defaultRole.getAuthorities().stream())
                    .collect(Collectors.toSet());

            AuthenticationResponse adminResponse = internalAuthenticationService
                    .generateAuthenticationResponse(tempAdminUsername, adminAuthorities);

            AuthenticationResponse userResponse = internalAuthenticationService
                    .generateAuthenticationResponse(tempUserUsername, userRoles);

            log.info("""
                    Admin
                        username: {}
                        access: {}
                        refresh: {}
                    user
                        username: {}
                        access: {}
                        refresh: {}
                    """,
                    tempAdminUsername, adminResponse.accessToken(), adminResponse.refreshToken(),
                    tempUserUsername, userResponse.accessToken(), userResponse.refreshToken());

        } catch (Exception e) {
            log.error("FAILED TO CREATE TOKENS FOR DEFAULT USERS");
            log.error("Cause: {} Exception: {}", e.getMessage(), e.toString());
        }

    }

}
