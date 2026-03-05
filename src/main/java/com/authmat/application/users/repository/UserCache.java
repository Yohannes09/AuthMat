package com.authmat.application.users.repository;

import com.authmat.application.users.model.UserDto;
import com.authmat.application.users.model.User;
import com.authmat.application.util.UserMapper;
import io.lettuce.core.RedisException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.function.Predicate;

@Slf4j
@Component
public class UserCache {
    private static final String USERNAME_KEY = "users:username:";
    private static final String EMAIL_KEY = "users:email:";
    private static final String ID_KEY = "users:id:";
    private static final Duration BASE_TTL_MINUTES = Duration.ofMinutes(30);

    // Prevents DB storms on repeated cache/db misses
    private static final UserDto NOT_FOUND = new UserDto(-1L);
    private static final Duration NOT_FOUND_TTL = Duration.ofMinutes(2);

    private final UserMapper userMapper;
    private final UserRepository userRepository;
    private final RedisTemplate<String,UserDto> redisTemplate;


    public UserCache(
            UserMapper userMapper,
            UserRepository userRepository,
            @Qualifier("userCacheRedisTemplate") RedisTemplate<String,UserDto> redisTemplate
    ) {
        this.userMapper = userMapper;
        this.userRepository = userRepository;
        this.redisTemplate = redisTemplate;
    }


    public Optional<UserDto> findByEmail(String email){
        return Optional.ofNullable(
                findByIdentifier(
                        buildKey(EMAIL_KEY, email),
                        email,
                        userRepository::findByUsernameOrEmail
                )
        );
    }

    public Optional<UserDto> findByUsername(String username){
        return Optional.ofNullable(
                findByIdentifier(
                        buildKey(USERNAME_KEY, username),
                        username,
                        userRepository::findByUsernameOrEmail
                )
        );
    }

    public Optional<UserDto> findById(Long id){
        return Optional.ofNullable(
                findByIdentifier(
                        buildKey(ID_KEY, id.toString()),
                        id,
                        userRepository::findById
                )
        );
    }


    public void cacheUser(UserDto user){
        Duration ttlWithJitter = BASE_TTL_MINUTES.plusSeconds(
                ThreadLocalRandom.current().nextInt(30));

        try{
            redisTemplate.opsForValue().set(
                    buildKey(USERNAME_KEY, user.getUsername()),
                    user,
                    ttlWithJitter);

            redisTemplate.opsForValue().set(
                    buildKey(EMAIL_KEY, user.getEmail()),
                    user,
                    ttlWithJitter);

            redisTemplate.opsForValue().set(
                    buildKey(ID_KEY, user.getId().toString()),
                    user,
                    ttlWithJitter);
        }catch (RedisException e){
            log.warn("Failed to cache user", e);
        }
    }


    public boolean existsByUsernameOrEmail(String username, String email){
        return existsByIdentifier(
                username,
                USERNAME_KEY + username,
                identifier -> userRepository.existsByUsernameOrEmail(identifier, email)
        );
    }

    public boolean existsByUsername(String username){
        return existsByIdentifier(
                username,
                USERNAME_KEY + username,
                userRepository::existsByUsernameIgnoreCase
        );
    }


    public boolean existsByEmail(String email){
        return existsByIdentifier(
                email,
                USERNAME_KEY + email,
                userRepository::existsByEmailIgnoreCase
        );
    }


    private <I> UserDto findByIdentifier(
            String key,
            I identifier,
            Function<I, Optional<User>> identifierFunction
    ){
        if(key == null) throw new IllegalStateException("Cache key must not be null");

        try {
            UserDto cached = redisTemplate.opsForValue().get(key);
            if(cached != null) {
                return cached == NOT_FOUND ?
                        null : cached;
            }

            UserDto dbFetched = identifierFunction.apply(identifier)
                    .map(userMapper::entityToDto)
                    .orElse(null);

            if(dbFetched == null) {
                redisTemplate.opsForValue().set(key, NOT_FOUND, NOT_FOUND_TTL);
                return null;
            }

            cacheUser(dbFetched);
            return dbFetched;
        } catch (RedisException e) {
            log.warn("Redis unavailable - fetching from DB");
            return identifierFunction.apply(identifier)
                    .map(userMapper::entityToDto)
                    .orElse(null);
        }
    }


    private String buildKey(String keyPrefix, String identifier){
        return keyPrefix + identifier.trim().toLowerCase(Locale.ROOT);
    }

    private <I> boolean existsByIdentifier(
            I identifier, String key, Predicate<I> repositoryFunction){
        return redisTemplate.hasKey(key + identifier.toString()) ||
                repositoryFunction.test(identifier);
    }


}
