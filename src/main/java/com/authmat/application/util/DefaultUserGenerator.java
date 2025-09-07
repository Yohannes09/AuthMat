package com.authmat.application.util;

import com.authmat.application.authentication.dto.AuthenticationResponse;
import com.authmat.application.authentication.service.InternalAuthenticationService;
import com.authmat.application.config.TokenSigningConfig;
import com.authmat.application.authorization.constant.DefaultRole;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.lang.reflect.InvocationTargetException;
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


    @PostConstruct
    public void createDefaultUsers()
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        log.info("Creating default Admin and User accounts.");

        try {
            String tempAdminUsername = UUID.randomUUID().toString();
            String tempUserUsername = UUID.randomUUID().toString();

            Set<GrantedAuthority> superAdminRoles = Set.of(
                    DefaultRole.SUPER_ADMIN,
                    DefaultRole.ADMIN,
                    DefaultRole.ELEVATED)
                        .stream()
                        .map(DefaultRole::getName)
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toSet());

            Set<GrantedAuthority> userRoles = Set.of(DefaultRole.BASIC).stream()
                    .map(DefaultRole::getName)
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toSet());

            AuthenticationResponse adminResponse = internalAuthenticationService
                    .generateAuthenticationResponse(
                            tempAdminUsername,
                            superAdminRoles);

            AuthenticationResponse userResponse = internalAuthenticationService
                    .generateAuthenticationResponse(
                            tempUserUsername,
                            userRoles);

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
            //throw e;
        }

    }

}
