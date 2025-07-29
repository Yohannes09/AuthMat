package com.authmat.application.authorization;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Provides access to application roles and ensures their presence at startup.
 * <p>
 * On initialization, this class loads all predefined {@link DefaultRoles} into the database
 * (if missing) and caches them for fast retrieval.
 * </p>
 *
 * <p>Use {@link #findRole(String)} to fetch a role from the in-memory cache.</p>
 *
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class RoleProvider {
    private final RoleRepository roleRepository;


    @PostConstruct
    private void addAllRoles(){
        Set<String> savedRoles = roleRepository.findAll().stream()
                .map(Role::getRole)
                .collect(Collectors.toSet());

        List<Role> unsavedRoles = DefaultRoles.getAll().stream()
                .filter(role -> !savedRoles.contains(role.getRole()))
                .map(Role::new)
                .toList();

        if(!unsavedRoles.isEmpty()){
            roleRepository.saveAll(unsavedRoles);
            log.info("Saved all default roles. ");
        }

    }

    @Cacheable(cacheNames = "role", key = "#role")
    public Role findRole(String role){
        return roleRepository.findByRoleIgnoreCase(role)
                .orElseThrow(() -> new RoleNotFoundException("Could not find role: " + role));
    }

}
