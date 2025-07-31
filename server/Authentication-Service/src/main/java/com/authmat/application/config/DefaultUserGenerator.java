package com.authmat.application.config;

import com.authmat.application.authorization.config.DefaultRolesAndPermissionsInitializer;
import com.authmat.application.users.UserAccountManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("dev")
@Slf4j(topic = "DEFAULT_USER_GENERATOR")
public class DefaultUserGenerator {

    @Bean
    public CommandLineRunner createDefaultUsers(
            UserAccountManager userAccountManager, DefaultRolesAndPermissionsInitializer defaultRolesAndPermissionsInitializer
    ){
        return args -> {
            log.info("Creating default Admin and User accounts.");

//            Role adminRole = defaultRolesAndPermissionsInitializer.findRole("ADMIN");
//            Role userRole = defaultRolesAndPermissionsInitializer.findRole("USER");
//
//            userAccountManager.createNewUser("admin12", "admin@example.com","Admin12@", Set.of(adminRole));
//            userAccountManager.createNewUser("user12", "user@example.com", "User123@", Set.of(userRole));

            log.info("Default Admin and User accounts created");
        };
    }

}
