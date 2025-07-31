package com.authmat.application.authorization.persistence;

import com.authmat.application.authorization.exception.PermissionNotFoundException;
import com.authmat.application.authorization.exception.RoleNotFoundException;
import com.authmat.application.authorization.entity.Permission;
import com.authmat.application.authorization.entity.Role;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
@Slf4j
@RequiredArgsConstructor
public class RolePermissionRepository {
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String PERMISSION_PREFIX = "auth:permission:";
    private static final String ROLE_PREFIX = "auth:role:";


    public Optional<Role> findRoleById(@NonNull Long id){
        String key = PERMISSION_PREFIX + id;
        Object cachedRole = redisTemplate.opsForValue().get(key);

        if(cachedRole instanceof Role role){
            return Optional.of(role);
        }

        Role dbRole = roleRepository.findById(id)
                .orElseThrow(() -> new RoleNotFoundException("Role not found in DB. "));
        redisTemplate.opsForValue().set(key, dbRole);

        return Optional.of(dbRole);
    }

    public Set<Role> findRolesById(List<Long> ids){
        Set<Role> roles = new HashSet<>();

        ids.forEach(id -> {
            try{
                findRoleById(id).ifPresent(roles::add);
            }catch (NullPointerException nullPointerException){
                log.error("Null Role-ID fetch attempt.");
            }
        });

        return roles;
    }



    public Optional<Permission> findPermissionById(@NonNull Long id){
        String key = ROLE_PREFIX + id;
        Object cachedRole = redisTemplate.opsForValue().get(key);

        if(cachedRole instanceof Permission permission){
            return Optional.of(permission);
        }

        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new PermissionNotFoundException("Role not found in DB. "));
        redisTemplate.opsForValue().set(key, permission);

        return Optional.of(permission);
    }

    public Set<Permission> findPermissionsById(List<Long> ids){
        Set<Permission> roles = new HashSet<>();

        ids.forEach(id -> {
            try{
                findPermissionById(id).ifPresent(roles::add);
            }catch (NullPointerException nullPointerException){
                log.error("Null Permission-ID fetch attempt.");
            }
        });

        return roles;
    }




}
