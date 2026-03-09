package com.authmat.application.util;

import com.authmat.application.authorization.constant.DefaultRole;
import com.authmat.application.authorization.entity.Role;
import com.authmat.application.authorization.repository.RoleRepository;
import com.authmat.application.users.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

//@Configuration
//@ConditionalOnProperty(
//        prefix = "authmat.system.credentials",
//        name = {"username", "password"})
//@RequiredArgsConstructor
//@Slf4j
//public class UserGenerator {
//    private final RoleRepository roleRepository;
//    private final UserService userService;
//
//    @Value("${authmat.system.credentials.username:}")
//    private String username;
//
//    @Value("${authmat.system.credentials.password:}")
//    private String password;
//
//
//    @Bean
//    public CommandLineRunner createDefaultUsers() {
//        return (args) -> {
//            List<Role> fetchedRoles = roleRepository
//                    .findAllByNameIgnoreCase(DefaultRole.getSystemRoles());
//
//            userService.createUser(
//                            username,
//                            null,
//                            password,
//                            fetchedRoles,
//                            null,
//                            null,
//                            null)
//            .orElseThrow(() ->
//                    new IllegalStateException("Failed to create System User."));
//
//            log.info("Super Admin {} created", username);
//        };
//    }
//
//}
