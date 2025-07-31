package com.authmat.application.authorization.config;

import com.authmat.application.authorization.constant.DefaultPermissions;
import com.authmat.application.authorization.constant.DefaultRoles;
import com.authmat.application.authorization.entity.Permission;
import com.authmat.application.authorization.entity.Role;
import com.authmat.application.authorization.persistence.PermissionRepository;
import com.authmat.application.authorization.persistence.RoleRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class DefaultRolesAndPermissionsInitializer {
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    @PostConstruct
    private void addAllRoles(){
        try {
            log.info("Initializing default roles and permissions.");

            Map<String, Permission> permissionEntities = DefaultPermissions
                    .getAll()
                    .stream()
                    .map(permission ->
                        permissionRepository
                            .findByName(permission.getName())
                                .orElse(permissionRepository.save(permissionMapper(permission)))
                    )
                    .collect(Collectors.toMap(Permission::getName, permission -> permission));


            Map<String, Set<Permission>> defaultRolePermissions = DefaultRoles
                    .getAll()
                    .stream()
                    .collect(
                            Collectors.toMap(
                                DefaultRoles::getName ,

                                defaultRoles ->
                                        defaultRoles
                                        .getPermissions()
                                                .stream()
                                                .map(DefaultPermissions::getName)
                                                .map(permissionEntities::get)
                                                .collect(Collectors.toSet())
                            )
                    );

            List<Role> defaultRoles = DefaultRoles
                    .getAll()
                    .stream()
                    .map(role -> roleMapper(role, defaultRolePermissions.get(role.getName())))
                    .toList();

            roleRepository.saveAll(defaultRoles);
            log.info("Default roles and permissions successfully initialized.");
        } catch (Exception e) {
            log.error("Error initializing default roles and permissions.");
            throw new IllegalStateException(e);
        }
    }


    private Permission permissionMapper(DefaultPermissions defaultPermission){
        return Permission.builder()
                .name(defaultPermission.getName())
                .description(defaultPermission.getDescription())
                .build();
    }

    private Role roleMapper(DefaultRoles defaultRole, Set<Permission> permissions){
        return Role.builder()
                .name(defaultRole.getName())
                .description(defaultRole.getDescription())
                .permissions(permissions)
                .build();
    }

}
