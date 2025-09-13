package com.authmat.application.util;

import com.authmat.application.authorization.constant.DefaultPermission;
import com.authmat.application.authorization.constant.DefaultRole;
import com.authmat.application.authorization.entity.Permission;
import com.authmat.application.authorization.entity.Role;
import com.authmat.application.authorization.repository.PermissionRepository;
import com.authmat.application.authorization.repository.RoleRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Configuration
@Slf4j
// Unless told otherwise externally, the bean gets created.
@ConditionalOnProperty(
        name = "feature.default-role-permission.initializer",
        havingValue = "true",
        matchIfMissing = true)
@RequiredArgsConstructor
public class DefaultAuthoritiesInitializer {
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    @PostConstruct
    public void init(){
        log.info("Beginning Authorities setup.");

        List<Permission> permissions = DefaultPermission.getAll()
                .stream()
                .map(defaultPermission -> {
                    try {
                        return permissionRepository.save(new Permission(defaultPermission));
                    } catch (DataIntegrityViolationException e) {
                        return permissionRepository.findByName(defaultPermission.getName())
                                .orElseThrow(() -> new IllegalStateException("Failed to initialize default permissions."));
                    }
                })
                .toList();

        Map<String, Permission> permissionMap = permissions.stream()
                .collect(Collectors.toMap(
                        Permission::getName,
                        permission -> permission));

        List<Role> roles = DefaultRole.getAll()
                .stream()
                .map(defaultRole -> {
                    try {
                        Set<Permission> rolePermissions = defaultRole.getPermissions()
                                            .stream()
                                            .map(defaultPermission ->
                                                    permissionMap.get(defaultPermission.getName())
                                            )
                                            .collect(Collectors.toSet());

                        return roleRepository.save(
                                Role.builder()
                                    .name(defaultRole.getName())
                                    .description(defaultRole.getDescription())
                                    .permissions(rolePermissions)
                                    .build());
                    } catch (DataIntegrityViolationException e) {
                        return roleRepository.findByName(defaultRole.getName())
                                .orElseThrow(() -> new IllegalStateException("Failed to initialize default roles."));
                    }
                })
                .toList();

        log.info("Default Roles and Permissions initialized.");

    }

}