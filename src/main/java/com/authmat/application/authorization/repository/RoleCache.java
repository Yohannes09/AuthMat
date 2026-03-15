package com.authmat.application.authorization.repository;

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
public class RoleCache {
    private final RoleRepository roleRepository;
    private final RoleMapper roleMapper;
    private final EntityManager entityManager;
    private final RedisTemplate<String,RoleDto> redisTemplate;

    private static final String SENTINEL_NAME = "__NOT_FOUND__";
    private static final RoleDto ROLE_SENTINEL = RoleDto.of(1L, SENTINEL_NAME);
    private static final String ROLE_KEY = "auth:role:";
    private static final Duration BASE_TTL_MINUTES = Duration.ofMinutes(15);
    private static final Duration NEGATIVE_TTL_MINUTES = Duration.ofMinutes(2);


    public Optional<RoleDto> findRoleByName(String roleName){
        return Optional.ofNullable(
                findRoleByIdentifier(
                        buildKey(roleName),
                        roleName,
                        roleRepository::findByName
                )
        );
    }

    public Optional<RoleDto> findRoleById(Long id){
        return Optional.ofNullable(
                findRoleByIdentifier(
                        buildKey(id.toString()),
                        id,
                        roleRepository::findById
                )
        );
    }

    // could be its own class that fetches roles?
    public Optional<Role> findRoleProxyByName(String roleName){
        RoleDto dto  = findRoleByIdentifier(
                buildKey(roleName),
                roleName,
                roleRepository::findByName
        );

        if(dto == null) return Optional.empty();
        return Optional.ofNullable(entityManager.getReference(Role.class, dto.id()));
    }

    // Same logic used across cache, could be a 1 method class
    private <I> RoleDto findRoleByIdentifier(
            String key, I identifier, Function<I,Optional<Role>> identifierFunction
    ){
        if(key == null) throw new IllegalStateException("Cache key can't be null");
        try{
            RoleDto cached = redisTemplate.opsForValue().get(key);

            if(cached != null) return SENTINEL_NAME.equals(cached.name()) ? null : cached;

            RoleDto dbFetched = identifierFunction.apply(identifier)
                    .map(roleMapper::entityToDto)
                    .orElse(null);

            if(dbFetched == null){
                log.debug("Cache and DB miss - Falling back to sentinel");
                redisTemplate.opsForValue().set(key, ROLE_SENTINEL, NEGATIVE_TTL_MINUTES);
                return null;
            }

            cacheAuthority(dbFetched);
            return dbFetched;
        }catch (RedisException e){
            log.warn("Redis unavailable - falling back to DB");
            return identifierFunction.apply(identifier)
                    .map(roleMapper::entityToDto)
                    .orElse(null);
        }
    }

    private void cacheAuthority(RoleDto role){
        Duration ttlWithJitter = BASE_TTL_MINUTES.plusSeconds(
                ThreadLocalRandom.current().nextInt(30));
        try {
            redisTemplate.opsForValue().set(
                    buildKey(role.name()),
                    role,
                    ttlWithJitter);

            redisTemplate.opsForValue().set(
                    buildKey(role.id().toString()),
                    role,
                    ttlWithJitter);
        }catch (RedisException e){
            log.warn("Could not write to cache - Redis unavailable");
        }
    }


    private String buildKey(String identifier){
        return ROLE_KEY + identifier.trim().toLowerCase(Locale.ROOT);
    }

}
