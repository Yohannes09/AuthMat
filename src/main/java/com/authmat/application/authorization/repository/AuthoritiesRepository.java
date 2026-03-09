package com.authmat.application.authorization.repository;

import com.authmat.application.authorization.RoleMapper;
import com.authmat.application.authorization.dto.RoleDto;
import com.authmat.application.authorization.entity.Role;
import io.lettuce.core.RedisException;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;

@Component
@Slf4j
@RequiredArgsConstructor
public class AuthoritiesRepository {
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RoleMapper roleMapper;
    private final EntityManager entityManager;
    private final RedisTemplate<String,RoleDto> redisTemplate;

    private static final String PERMISSION_KEY = "auth:permission:";
    private static final String ROLE_KEY = "auth:role:";
    private static final Duration BASE_TTL_MINUTES = Duration.ofMinutes(15);
    private static final RoleDto ROLE_NOT_FOUND = RoleDto.of(1L, "ROLE_NOT_FOUND");
    private static final Duration NOT_FOUND_DURATION_MINUTES = Duration.ofMinutes(2);

    public Optional<RoleDto> findRoleByName(String roleName){
        return Optional.ofNullable(null);
    }

    public Optional<Role> findRoleProxyByName(String roleName){
        RoleDto dto  = findRoleByIdentifier(
                buildKey(ROLE_KEY, roleName),
                roleName,
                roleRepository::findByName);

        if(dto == null) return Optional.empty();
        return Optional.ofNullable(entityManager.getReference(Role.class, dto.id()));
    }

    private <I> RoleDto findRoleByIdentifier(
            String key,
            I identifier,
            Function<I,Optional<Role>> identifierFunction
    ){
        if(key == null) throw new IllegalStateException("Cache key can't be null");
        try{
            RoleDto cached = redisTemplate.opsForValue().get(key);

            if(cached != null) {
                return cached != ROLE_NOT_FOUND ?
                        cached : null;
            }

            RoleDto dbFetched = identifierFunction.apply(identifier)
                    .map(roleMapper::entityToDto)
                    .orElse(null);

            if(dbFetched == null){
                redisTemplate.opsForValue().set(key, ROLE_NOT_FOUND, NOT_FOUND_DURATION_MINUTES);
                return null;
            }
            cacheAuthority(dbFetched);
            return dbFetched;
        }catch (RedisException e){
            // TODO
            return null;
        }
    }

    private void cacheAuthority(RoleDto role){
        Duration ttlWithJitter = BASE_TTL_MINUTES.plusSeconds(
                ThreadLocalRandom.current().nextInt(30));

        try {
            redisTemplate.opsForValue().set(
                    buildKey(ROLE_KEY, role.name()),
                    role,
                    ttlWithJitter);

            //Not sure if I should cache by ID as well
//            redisTemplate.opsForValue().set(
//                    buildKey("", ""),
//                    role,
//                    ttlWithJitter);
        }catch (RedisException e){
            // TODO
        }
    }

//    public Optional<Permission> findPermissionById(@NonNull Long id){
//        String key = ROLE_KEY + id;
//        Object cachedRole = redisTemplate.opsForValue().get(key);
//
//        if(cachedRole instanceof Permission permission){
//            return Optional.of(permission);
//        }
//
//        Permission permission = permissionRepository.findById(id)
//                .orElseThrow(() -> new PermissionNotFoundException("Role not found in DB. "));
//        redisTemplate.opsForValue().set(key, permission);
//
//        return Optional.of(permission);
//    }

    public <V> V repositoryFunction(Function<String,V> repositorySearchFunction, String name){
        return repositorySearchFunction.apply(name);
    }


    private String buildKey(String keyPrefix, String identifier){
        return keyPrefix + identifier.trim().toLowerCase(Locale.ROOT);
    }

}
