package com.authmat.application.config;

import com.authmat.application.authorization.config.DefaultAuthoritiesInitializer;
import com.authmat.application.authorization.constant.DefaultRole;
import com.authmat.application.authorization.entity.Role;
import com.authmat.application.authorization.repository.RoleRepository;
import com.authmat.application.users.service.UserService;
import com.authmat.tool.exception.FailedToInitializeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Profile;

import java.util.Set;

@Configuration
@DependsOn("defaultAuthoritiesInitializer")
@Profile("dev")
@RequiredArgsConstructor
@Slf4j(topic = "DEFAULT_USER_GENERATOR")
public class DefaultUserGenerator {
    private final RoleRepository roleRepository;
    private final UserService userService;

    @Bean
    public CommandLineRunner createDefaultUsers(
            UserService userService,
            DefaultAuthoritiesInitializer defaultAuthoritiesInitializer
    ){
        return args -> {
            log.info("Creating default Admin and User accounts.");

            Role superAdminRole = roleRepository
                    .findByName(DefaultRole.SUPER_ADMIN.getName())
                    .orElseThrow(() -> new FailedToInitializeException("Could not initialize SUPER_ADMIN_ROLE role."));

            Role userRole = roleRepository
                    .findByName(DefaultRole.BASIC.getName())
                    .orElseThrow(() -> new FailedToInitializeException("Could not initialize USER_ROLE role."));

            userService.createUser(
                    "admin123", "admin@example.com","Admin12@", Set.of(superAdminRole, userRole)
            );

            userService.createUser(
                    "user1234", "user@example.com", "User123@", Set.of(userRole)
            );

            log.info("Default Admin and User accounts created");
        };
    }

}
