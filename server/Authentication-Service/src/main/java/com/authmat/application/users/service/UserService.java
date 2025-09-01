package com.authmat.application.users.service;

import com.authmat.application.authentication.exception.DuplicateCredentialException;
import com.authmat.application.authorization.entity.Role;
import com.authmat.application.users.repository.CachedUserRepository;
import com.authmat.application.users.model.User;
import com.authmat.application.users.util.UserMapper;
import com.authmat.events.NewUserEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {
    private final UserMapper userMapper;
    private final CachedUserRepository cachedUserRepository;
    private final KafkaTemplate<String,Object> kafkaTemplate;


    public void createUser(String username, String email, String encodedPassword, Collection<Role> roles){
        User user = User.builder()
                .username(username)
                .email(email)
                .password(encodedPassword)
                .roles(new HashSet<>())
                .build();
        cachedUserRepository.save(user);
    }

    public boolean createAndPublishUser(User user){
        try {
            if(cachedUserRepository.existsByUsernameOrEmail(user.getUsername(), user.getEmail())){
                throw new DuplicateCredentialException("User attempted to use taken credentials.");
            }

            User savedUser = cachedUserRepository.save(user);
            kafkaTemplate.send(
                    "user-created-events",
                    new NewUserEvent(user.getId(), user.getUsername(), user.getEmail(), Instant.now()));
            return true;
        } catch (Exception e) {
            log.warn("{}", e.getMessage());
            return false;
        }
    }

}
