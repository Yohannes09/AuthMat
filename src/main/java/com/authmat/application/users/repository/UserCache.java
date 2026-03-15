package com.authmat.application.users.repository;

import com.authmat.application.users.model.UserDto;
import com.authmat.application.users.model.User;
import com.authmat.application.users.UserMapper;
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
    private static final UserDto NOT_FOUND = UserDto.of(-1L, "__NOT_FOUND__");
    private static final Duration NOT_FOUND_TTL = Duration.ofMinutes(2);

    private final UserMapper userMapper;
    private final UserRepository userRepository;
    private final RedisTemplate<String,UserDto> redisTemplate;


    public UserCache(
            UserMapper userMapper,
            UserRepository userRepository,
            @Qualifier("userCacheRedisTemplate") RedisTemplate<String,UserDto> redisTemplate) {
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


    /**
     * Persists to DB first, then assuming all goes well, cache the user.*/
    public User save(User user) {
        try {
            User savedUser =  userRepository.save(user);
            cacheUser(userMapper.entityToDto(savedUser));
            return savedUser;
        } catch (RedisException e) {
            log.warn("Redis unavailable during save - cache skipped, persisting to db");
            return userRepository.save(user);
        }
    }


    public boolean existsByUsernameOrEmail(String username, String email){
        return existsByUsername(username) || existsByEmail(email);
    }


    public boolean existsByUsername(String username){
        return existsByIdentifier(
                username,
                buildKey(USERNAME_KEY, username),
                userRepository::existsByUsernameIgnoreCase
        );
    }


    public boolean existsByEmail(String email){
        return existsByIdentifier(
                email,
                buildKey(EMAIL_KEY, email),
                userRepository::existsByEmailIgnoreCase
        );
    }


    private void cacheUser(UserDto user){
        Duration ttlWithJitter = BASE_TTL_MINUTES.plusSeconds(
                ThreadLocalRandom.current().nextInt(30));

        try{
            redisTemplate.opsForValue().set(
                    buildKey(USERNAME_KEY, user.username()),
                    user,
                    ttlWithJitter);

            redisTemplate.opsForValue().set(
                    buildKey(EMAIL_KEY, user.email()),
                    user,
                    ttlWithJitter);

            redisTemplate.opsForValue().set(
                    buildKey(ID_KEY, user.id().toString()),
                    user,
                    ttlWithJitter);
        }catch (RedisException e){
            log.warn("Redis unavailable - cache skipped", e);
        }
    }


    private <I> UserDto findByIdentifier(
            String key,
            I identifier,
            Function<I, Optional<User>> identifierFunction
    ){
        if(key == null) throw new IllegalStateException("Cache key must not be null");

        try {
            UserDto cached = redisTemplate.opsForValue().get(key);
            //TODO: Ensure sentinel will work
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


    /**
     * redisTemplate.hasKey() returns Boolean not boolean,
     * unboxing a null will throw a NPE*/
    private <I> boolean existsByIdentifier(
            I identifier, String key, Predicate<I> repositoryFunction){
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            log.warn("Redis unavailable during existence check - falling back to DB");
        }
        return repositoryFunction.test(identifier);
    }


}
