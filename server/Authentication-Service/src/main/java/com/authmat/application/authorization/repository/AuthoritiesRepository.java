package com.authmat.application.authorization.repository;

import com.authmat.application.authorization.entity.Permission;
import com.authmat.application.authorization.exception.PermissionNotFoundException;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.function.Function;

@Component
@Slf4j
@RequiredArgsConstructor
public class AuthoritiesRepository {
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String PERMISSION_PREFIX = "auth:permission:";
    private static final String ROLE_PREFIX = "auth:role:";


//    public <V> Optional<V> findRoleByName(@NonNull String name, Function<User, V> userMapperFunction){
//        String key = PERMISSION_PREFIX + name;
//        Object cachedRole = redisTemplate.opsForValue().get(key);
//
//        if(cachedRole instanceof Role role){
//            return Optional.of(role);
//        }
//
//        Role dbRole = roleRepository.findByName(name)
//                .orElseThrow(() -> new RoleNotFoundException("Role not found in DB. "));
//        log.info("Fetched Role from database.");
//        redisTemplate.opsForValue().set(key, dbRole);
//
//        return Optional.of(userMapperFunction.apply(dbRole));
//    }

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

    public <V> V repositoryFunction(Function<String,V> repositorySearchFunction, String name){
        return repositorySearchFunction.apply(name);
    }

}
