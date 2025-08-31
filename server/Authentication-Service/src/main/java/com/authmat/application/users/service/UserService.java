package com.authmat.application.users.service;

import com.authmat.application.authentication.DuplicateCredentialException;
import com.authmat.application.authorization.entity.Role;
import com.authmat.application.users.UserRepository;
import com.authmat.application.users.model.User;
import com.authmat.application.users.model.UserDto;
import com.authmat.application.users.util.UserMapper;
import com.authmat.events.NewUserEvent;
import com.authmat.tool.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.function.Function;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {
    //TO-DO remove cache logic and have it in its own class

    private static final String USERNAME_OR_EMAIL_LOOKUP = "usernameOrEmail:lookup:";
    private static final String ID_LOOKUP = "id:lookup:";
    private static final String USER_DATA_LOOKUP = "userData:lookup:";

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final KafkaTemplate<String,Object> kafkaTemplate;


    /**
     * Creates and persists a new {@link User} with the provided credentials and roles.
     * @param username
     * @param email
     * @param encodedPassword does not
     * @param roles CAN be null or an Empty Collection.
     * @throws DuplicateCredentialException if the username or email already exists.
     */
    public void createUser(String username, String email, String encodedPassword, Collection<Role> roles){
        createAndSaveUser(username, email, encodedPassword, roles);
    }

    public void createAndPublishUser(String username, String email, String encodedPassword, Collection<Role> roles){
        User user = createAndSaveUser(username, email, encodedPassword, roles);
        kafkaTemplate.send("user-created-events", new NewUserEvent(user.getId(), username, email, Instant.now()));
    }


    public User findEntityById(Long userId){
        return findUser(userId, userRepository::findById, ID_LOOKUP + userId)
                .orElseThrow(()-> new UserNotFoundException("User not found: " + userId));
    }

    public UserDto findById(Long userId){
        return findUser(userId, userRepository::findById, USERNAME_OR_EMAIL_LOOKUP + userId)
                .map(userMapper::entityToDto)
                .orElseThrow(()-> new UserNotFoundException("User not found: " + userId));
    }

    public UserDto findByUsernameOrEmail(String usernameOrEmail){
        return findUser(usernameOrEmail, userRepository::findByUsernameOrEmail, USERNAME_OR_EMAIL_LOOKUP + usernameOrEmail)
                .map(userMapper::entityToDto)
                .orElseThrow();
    }

    private <T> Optional<User> findUser(
            T identifier, Function<T, Optional<User>> userRepositoryFunction, String key
    ){
        Optional<User> cachedUser = (Optional<User>) redisTemplate.opsForValue().get(key);

        if(cachedUser.isEmpty()) cachedUser = userRepositoryFunction.apply(identifier);

        redisTemplate.opsForValue().set(key, cachedUser, Duration.ofMinutes(20));
        return cachedUser;
    }


    public boolean existsByUsername(String username){
        if(username.isBlank())
            throw new NullPointerException("Provided null/empty username parameter to existsByUsername()");

        return userRepository.existsByUsernameIgnoreCase(username);
    }

    public boolean existsByEmail(String email){
        return userRepository.existsByEmailIgnoreCase(email);
    }

    private User createAndSaveUser(
            String username, String email, String password, Collection<Role> roles){

        if(userRepository.existsByUsernameOrEmail(username, email)){
            throw new DuplicateCredentialException("Username or Email already registered.");
        }

        User user = User.builder()
                .username(username)
                .email(email)
                .password(password)
                .roles(new HashSet<>())
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .enabled(true)
                .build();

        if(roles != null && !roles.isEmpty()){
            user.getRoles().addAll(roles);
        }

        return userRepository.save(user);
    }

}
