package com.authmat.application.util;

import com.authmat.application.authorization.constant.DefaultRole;
import com.authmat.application.authorization.entity.Role;
import com.authmat.application.authorization.repository.RoleRepository;
import com.authmat.application.constant.ValidationConstants;
import com.authmat.application.users.entity.User;
import com.authmat.application.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashSet;
import java.util.List;

@Configuration
@ConditionalOnProperty(
        prefix = "authmat.system.credentials",
        name = {"username", "password"})
@RequiredArgsConstructor
@Slf4j
public class UserGenerator {
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;

    @Value("${authmat.system.credentials.username:}")
    private String username;

    @Value("${authmat.system.credentials.password:}")
    private String password;


    @Bean
    public CommandLineRunner createDefaultUsers() {
        return (args) -> {
            validateCredentials(username, password);

            if (userRepository.existsByUsernameIgnoreCase(username)) {
                log.info("System user {} already exists. Skipping creation.", username);
                return;
            }

            List<Role> fetchedRoles = roleRepository
                    .findAllByNameIgnoreCase(DefaultRole.getSystemRoles());

            User authmatSuperAdmin = User.builder()
                    .username(username)
                    .password(password)
                    .roles(new HashSet<>(fetchedRoles))
                    .build();

            userRepository.save(authmatSuperAdmin);
            log.info("Super Admin {} created", username);
        };
    }

    private void validateCredentials(String username, String password){
        if (username.isBlank() || password.isBlank()) {
            throw new IllegalStateException("Username and password must be provided together.");
        }

        if (!username.matches(ValidationConstants.USERNAME_PATTERN)) {
            throw new IllegalArgumentException(ValidationConstants.USERNAME_VALIDATION_MESSAGE);
        }

        if (!password.matches(ValidationConstants.PASSWORD_PATTERN)) {
            throw new IllegalArgumentException(ValidationConstants.PASSWORD_VALIDATION_MESSAGE);
        }

    }

}
