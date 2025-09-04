package com.authmat.application.users.repository;

import com.authmat.application.users.model.User;
import com.authmat.application.users.model.UserDto;
import com.authmat.application.util.UserMapper;
import com.authmat.tool.exception.UserNotFoundException;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

@Slf4j
@Component
@RequiredArgsConstructor
public class CachedUserRepository {
    private static final String USERNAME_OR_EMAIL_KEY = "users:usernameOrEmail:";
    private static final String ID_KEY = "users:currentPassword";

    private final UserMapper userMapper;
    private final UserRepository userRepository;
    private final RedisTemplate<String,Object> redisTemplate;
    private final EntityManager entityManager;


    public Optional<UserDto> findByUsernameOrEmail(String usernameOrEmail){
        return findUser(
                usernameOrEmail,
                USERNAME_OR_EMAIL_KEY + usernameOrEmail,
                identifier -> userRepository
                        .findByUsernameOrEmail(identifier)
                        .orElseThrow(() -> new UserNotFoundException("User not found " + usernameOrEmail)));
    }

    public Optional<UserDto> findById(Long id){
        return findUser(
                id,
                ID_KEY + id,
                identifier ->
                        userRepository
                                .findById(identifier)
                                .orElseThrow(() -> new UserNotFoundException("User not found " + id)));
    }

    public <V> Optional<V> findByUsernameOrEmail(String usernameOrEmail, Function<User,V> mapperFunction){
        return Optional.ofNullable(findUser(
                usernameOrEmail,
                USERNAME_OR_EMAIL_KEY + usernameOrEmail,
                identifier ->
                        userRepository.findByUsernameOrEmail(identifier)
                                .orElseThrow(() -> new UserNotFoundException("User not found " + usernameOrEmail)),
                mapperFunction));
    }

    public <V> V findById(Long id, Function<User,V> mapperFunction){
        return findUser(
                id,
                ID_KEY + id,
                identifier ->
                        userRepository.findById(identifier)
                                .orElseThrow(() -> new UserNotFoundException("User not found " + id)),
                mapperFunction);
    }

    public User save(User user){
        try {
            User savedUser = userRepository.save(user);
            redisTemplate.opsForValue().set(
                    ID_KEY + savedUser.getId(),
                    userMapper.entityToDto(savedUser));

            return savedUser;
        } catch (Exception e) {
            log.error("USER CACHE ERROR: {}", e.getMessage());
            throw new RuntimeException(e);
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

    private <I> boolean existsByIdentifier(
            I identifier, String key, Predicate<I> repositoryFunction){

        return redisTemplate.hasKey(key + identifier.toString()) &&
                repositoryFunction.test(identifier);
    }

    private <I,V> V findUser(
            I identifier,
            String key,
            Function<I, User> userRepositoryFunction,
            Function<User,V> mapperFunction){

        Object object = redisTemplate.opsForValue().get(key);

        if(object instanceof UserDto userDto){
            return mapperFunction.apply(entityManager.getReference(User.class, userDto.getId()));
        }
        final User user = userRepositoryFunction.apply(identifier);
        redisTemplate.opsForValue().set(key, userMapper.entityToDto(user));

        return mapperFunction.apply(user);
    }

    private  <I> Optional<UserDto> findUser(
            I identifier,
            String key,
            Function<I,User> repositoryFunction){
        Object object = redisTemplate.opsForValue().get(key);

        if(object instanceof UserDto userDto){
            return Optional.ofNullable(userDto);
        }

        Optional<UserDto> user = Optional.ofNullable(repositoryFunction.apply(identifier))
                .map(userMapper::entityToDto);

        redisTemplate.opsForValue().set(key, user);
        return user;
    }

}
