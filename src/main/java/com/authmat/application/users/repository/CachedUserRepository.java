package com.authmat.application.users.repository;

import com.authmat.application.users.entity.User;
import com.authmat.application.users.dto.UserDto;
import com.authmat.application.util.UserMapper;
import com.authmat.tool.exception.UserNotFoundException;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

@Slf4j
@Component
@RequiredArgsConstructor
public class CachedUserRepository {
    public static final String USERNAME_OR_EMAIL_KEY = "users:usernameOrEmail:";
    public static final String ID_KEY = "users:id:";

    private static final String DATA_REFERENCE_KEY = "users:data:";
    private static final Duration TTL_MINUTES = Duration.ofMinutes(30);

    private final UserMapper userMapper;
    private final UserRepository userRepository;
    private final RedisTemplate<String,String> redisTemplate;
    private final EntityManager entityManager;


    public <I,V> V findUser(
        String key,
        boolean useProxy,
        I identifier,
        Function<I, Optional<User>> userRepositoryFunction,
        Function<User,V> mapperFunction){

        return findCachedUser(key, useProxy)
                .map(mapperFunction)
                .orElseGet(()-> findAndCacheUser(
                        key, identifier, userRepositoryFunction, mapperFunction));
    }

    @Transactional
    public Optional<User> save(User user){
        try {
            User savedUser = userRepository.save(user);

            cacheUser(savedUser);
            return Optional.of(savedUser);
        } catch (Exception e) {
            log.warn("Failed to persist data for user: {}", e.getMessage());
            log.debug("Cause: ", e.getMessage());
            return Optional.empty();
        }

    }

    public boolean existsByUsernameOrEmail(String username, String email){
        return existsByIdentifier(
                username,
                USERNAME_OR_EMAIL_KEY + username,
                identifier -> userRepository.existsByUsernameOrEmail(identifier, email));
    }

    public boolean existsByUsername(String username){
        return existsByIdentifier(
                username,
                USERNAME_OR_EMAIL_KEY + username,
                userRepository::existsByUsernameIgnoreCase);
    }

    public boolean existsByEmail(String email){
        return existsByIdentifier(
                email,
                USERNAME_OR_EMAIL_KEY + email,
                userRepository::existsByEmailIgnoreCase);
    }

    public UserRepository userRepository(){
        return userRepository;
    }



    private <I> boolean existsByIdentifier(
            I identifier, String key, Predicate<I> repositoryFunction){
        return redisTemplate.hasKey(key + identifier.toString()) ||
                repositoryFunction.test(identifier);
    }

    private Optional<User> findCachedUser(String key, boolean useProxy){
        try{
            String userDataKey = (String) redisTemplate.opsForValue().get(key);
            if(Objects.isNull(userDataKey) || userDataKey.isBlank()) {
                return Optional.empty();
            }

            Object cached = redisTemplate.opsForValue().get(userDataKey);
            if(cached instanceof UserDto dto && dto.getId() != null){
                log.debug("Cache hit for key: {}", key);
                return useProxy ?
                        Optional.of(entityManager.getReference(User.class, dto.getId())) :
                        Optional.of(entityManager.find(User.class, dto.getId()));
            }
        } catch (Exception e) {
            log.warn("Cache read failed for key: {}", key, e);
        }
        return Optional.empty();
    }

    private <I,V> V findAndCacheUser(
            String key,
            I identifier,
            Function<I,Optional<User>> userRepositoryFunction,
            Function<User,V> mapperFunction){
        log.debug("Cache miss for key: {}", key);
        return userRepositoryFunction.apply(identifier)
                .map(user -> {
                    cacheUser(user);
                    return mapperFunction.apply(user);
                })
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }


    private void cacheUser(User user){
        try{
            UserDto dto = userMapper.entityToDto(user);
            String dataKey = DATA_REFERENCE_KEY + dto.getId();

            redisTemplate.opsForValue().set(
                    dataKey,
                    dto,
                    TTL_MINUTES);

            redisTemplate.opsForValue().set(
                    USERNAME_OR_EMAIL_KEY + dto.getUsername(),
                    dataKey,
                    TTL_MINUTES);

            redisTemplate.opsForValue().set(
                    ID_KEY + dto.getId(),
                    dataKey,
                    TTL_MINUTES);
        } catch (Exception e) {
            log.warn("Failed to cache user");
            log.debug("User cache failure cause: {}", e.getMessage(), e);
        }
    }

}
