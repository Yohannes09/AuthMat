package com.authmat.application.authorization;

import com.authmat.application.authorization.constant.DefaultRoles;
import com.authmat.application.authorization.entity.Role;
import com.authmat.application.authorization.persistence.RoleRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Provides access to application roles and ensures their presence at startup.
 * <p>
 * On initialization, this class loads all predefined {@link DefaultRoles} into the database
 * (if missing) and caches them for fast retrieval.
 * </p>
 *
 *
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class DefaultRoleInitializer {
    private final RoleRepository roleRepository;


    @PostConstruct
    private void addAllRoles(){
        Set<DefaultRoles> allDefaultRoles = DefaultRoles.getAll();
        if(allDefaultRoles.isEmpty()){
            throw new IllegalStateException("Default roles not present.");
        }

        List<String> defaultRoleNames = allDefaultRoles
                .stream()
                .map(DefaultRoles::getName)
                .toList();

        Set<String> defaultRolesDb = roleRepository
                .findAllByNameIgnoreCase(defaultRoleNames)
                .stream()
                .map(Role::getRole)
                .collect(Collectors.toSet());

        Set<Role> uninitializedRoles = allDefaultRoles
                .stream()
                .filter(role -> !defaultRolesDb.contains(role.getName()))
                .map(Role::new)
                .collect(Collectors.toSet());

        roleRepository.saveAll(uninitializedRoles);
    }

//    @Cacheable(cacheNames = "role", key = "#role")
//    public Role findRole(String role){
//        return roleRepository.findByRoleIgnoreCase(role)
//                .orElseThrow(() -> new RoleNotFoundException("Could not find role: " + role));
//    }

}
