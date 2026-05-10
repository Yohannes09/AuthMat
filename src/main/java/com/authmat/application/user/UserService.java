package com.authmat.application.user;

import com.authmat.application.authentication.exception.DuplicateCredentialException;
import com.authmat.application.outbox.OutboxWriter;
import com.authmat.application.user.model.User;
import com.authmat.application.user.model.UserDto;
import com.authmat.application.user.repository.UserCache;
import com.authmat.events.NewUserEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class UserService {
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final UserCache userCache;
    private final OutboxWriter eventPublisher;

    public UserService(
            UserMapper userMapper,
            PasswordEncoder passwordEncoder,
            UserCache userCache,
            OutboxWriter eventPublisher) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.userCache = userCache;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public UserDto register(
            String username,
            String email,
            String password,
            String provider,
            String providerId){
        if(userCache.existsByUsernameOrEmail(username, email)){
            throw new DuplicateCredentialException(
                    "User creation failed - username or email already in use");
        }

        User savedUser = userCache.save(
                new User(
                        username,
                        passwordEncoder.encode(password),
                        email,
                        provider,
                        providerId));

        eventPublisher.userCreatedEvent(
                NewUserEvent.of(
                        savedUser.getExternalId().toString(),
                        savedUser.getUsername(),
                        savedUser.getEmail()));

        return userMapper.entityToDto(savedUser);
    }

}
