package com.authmat.authentication.configuration.notprod;

import com.authmat.authentication.component.RoleProvider;
import com.authmat.authentication.component.UserAccountManager;

import com.authmat.authentication.entity.Role;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.Set;

@Configuration
@Profile("dev")
@Slf4j(topic = "DEFAULT_USER_GENERATOR")
public class DefaultUserGenerator {

    @Bean
    public CommandLineRunner createDefaultUsers(
            UserAccountManager userAccountManager, RoleProvider roleProvider
    ){
        return args -> {
            log.info("Creating default Admin and User accounts.");

            Role adminRole = roleProvider.findRole("ADMIN");
            Role userRole = roleProvider.findRole("USER");

            userAccountManager.createNewUser("admin12", "admin@example.com","Admin12@", Set.of(adminRole));
            userAccountManager.createNewUser("user12", "user@example.com", "User123@", Set.of(userRole));

            log.info("Default Admin and User accounts created");
        };
    }

}
