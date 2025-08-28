package com.authmat.application.authorization.config;

import com.authmat.application.authorization.constant.DefaultPermission;
import com.authmat.application.authorization.constant.DefaultRoles;
import com.authmat.application.authorization.entity.Permission;
import com.authmat.application.authorization.entity.Role;
import com.authmat.application.authorization.repository.PermissionRepository;
import com.authmat.application.authorization.repository.RoleRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

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
        matchIfMissing = true
)
@RequiredArgsConstructor
public class DefaultRolesAndPermissionsInitializer {
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    @PostConstruct
    public void init(){
        try {
            List<Permission> permissions = DefaultPermission
                    .getAll()
                    .stream()
                    .map(Permission::new)
                    .toList();

            Map<String, Permission> permissionMap = permissionRepository
                    .saveAll(permissions)
                    .stream()
                    .collect(Collectors.toMap(Permission::getName, permission -> permission));

            List<Role> roles = DefaultRoles
                    .getAll()
                    .stream()
                    .map(defaultRole -> {

                        Set<Permission> rolePermissions =
                                    defaultRole.getPermissions()
                                            .stream()
                                            .map(defaultPermission ->
                                                    permissionMap.get(defaultPermission.getName())
                                            )
                                            .collect(Collectors.toSet());

                        return Role.builder()
                                .name(defaultRole.getName())
                                .description(defaultRole.getDescription())
                                .permissions(rolePermissions)
                                .build();

                    })
                    .toList();

            roleRepository.saveAll(roles);
            log.info("Default Roles and Permissions initialized.");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}