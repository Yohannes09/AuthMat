package com.authmat.application.users;

import com.authmat.application.authentication.exception.DuplicateCredentialException;
import com.authmat.application.authorization.entity.Role;
import com.authmat.application.users.dto.EmailUpdateRequest;
import com.authmat.application.users.dto.PasswordUpdateRequest;
import com.authmat.application.users.dto.UsernameUpdateRequest;
import com.authmat.application.users.exception.CredentialUpdateException;
import com.authmat.application.users.model.User;
import com.authmat.application.users.model.UserDto;
import com.authmat.application.users.repository.CachedUserRepository;
import com.authmat.application.util.UserMapper;
import com.authmat.events.NewUserEvent;
import com.authmat.tool.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.function.Consumer;
import java.util.function.Predicate;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final CachedUserRepository cachedUserRepository;
    private final KafkaTemplate<String,Object> kafkaTemplate;

    // Create event to update other services about user changes

    public void createUser(String username, String email, String password, Collection<Role> roles){
        User user = User.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(password))
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

    // Below will do for now
    @Transactional
    public boolean updateUsername(UsernameUpdateRequest usernameUpdateRequest, Long userId){
        try {
            User user = cachedUserRepository.findById(userId, uzer -> uzer);
            String newUsername = usernameUpdateRequest.newUsername();
            UserDto cachedUser = cachedUserRepository
                    .findById(userId)
                    .orElseThrow(() ->
                            new UserNotFoundException("Could not find user " + usernameUpdateRequest.currentUsername()));

            validateCredential(
                    cachedUser.getUsername(),
                    usernameUpdateRequest.currentUsername(),
                    newUsername,
                    cachedUserRepository::existsByUsername);

            persistCredentialChange(user, newUsername, user::setUsername);

            log.info("User {} successfully updated username. ", user.getId());
            return true;
        } catch (Exception e) {
            log.warn("Username update failure: {}", e.getMessage());
            return false;
        }

    }

    @Transactional
    public boolean updateEmail(EmailUpdateRequest emailUpdateRequest, Long userId){
        try {
            User proxyEntity = cachedUserRepository.findById(userId, user -> user);
            UserDto cachedUser = cachedUserRepository
                    .findById(userId)
                    .orElseThrow(() -> new UserNotFoundException(""));

            String newEmail = emailUpdateRequest.newEmail();

            validateCredential(
                    cachedUser.getEmail(),
                    emailUpdateRequest.currentEmail(),
                    newEmail,
                    cachedUserRepository::existsByEmail);

            persistCredentialChange(proxyEntity, newEmail, proxyEntity::setEmail);

            log.info("User {} successfully updated email. ", proxyEntity.getId());
            return true;
        } catch (Exception e) {
            log.warn("Username update failure: {}", e.getMessage());
            return false;
        }
    }

    @Transactional
    public boolean updatePassword(PasswordUpdateRequest passwordUpdateRequest, Long userId){
        try {
            User userProxy = cachedUserRepository.findById(userId, user -> user);

            String currentPassword = userProxy.getPassword();
            String newPassword = passwordEncoder.encode(passwordUpdateRequest.newPassword());

            validateCredential(
                    currentPassword,
                    passwordUpdateRequest.currentPassword(),
                    newPassword,
                    null);

            persistCredentialChange(userProxy, newPassword, userProxy::setPassword);

            log.info("User {} successfully updated password. ", userProxy.getId());
            return true;
        } catch (Exception e) {
            log.warn("Username update failure: {}", e.getMessage());
            return false;
        }
    }


    private void validateCredential(
            String currentCredential,
            String clientProvidedCredential,
            String newCredential,
            Predicate<String> uniquenessCheck){

        if(currentCredential.equals(newCredential)){
            throw new CredentialUpdateException("New credential cannot be the same as the current one. ");
        }

        if(!currentCredential.equals(clientProvidedCredential)){
            throw new CredentialUpdateException("User provided the incorrect current credential.");
        }

        if(uniquenessCheck != null && uniquenessCheck.test(newCredential)){
            throw new CredentialUpdateException(newCredential + " is already taken.");
        }

    }

    private void persistCredentialChange(
            User user, String credential, Consumer<String> setNewCredential){
        setNewCredential.accept(credential);
        cachedUserRepository.save(user);
    }

}
