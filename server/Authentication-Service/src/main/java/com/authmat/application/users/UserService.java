package com.authmat.application.users;

import com.authmat.application.authentication.DuplicateCredentialException;
import com.authmat.application.authorization.entity.Role;
import com.authmat.application.users.model.User;
import com.authmat.application.users.model.UserDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {
    private static final String USERNAME_OR_EMAIL_LOOKUP = "usernameOrEmail:lookup:";
    private static final String ID_LOOKUP = "id:lookup:";
    private static final String USER_DATA_LOOKUP = "userData:lookup:";

    private final UserRepository userRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final UserMapper userMapper;


    /**
     * Creates and persists a new {@link User} with the provided credentials and roles.
     * @param username
     * @param email
     * @param encodedPassword
     * @param roles
     * @throws DuplicateCredentialException if the username or email already exists.
     */
    public void createNewUser(
            String username, String email, String encodedPassword, Set<Role> roles
    ){
        if(userRepository.existsByUsernameOrEmail(username, email)){
            throw new DuplicateCredentialException("Username or Email already registered. ");
        }

        userRepository.save(User.builder()
                        .username(username)
                        .email(email)
                        .password(encodedPassword)
                        .roles(roles)
                        .accountNonExpired(true)
                        .accountNonLocked(true)
                        .credentialsNonExpired(true)
                        .enabled(true)
                        .build()
        );

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

}
