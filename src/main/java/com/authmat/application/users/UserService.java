package com.authmat.application.users;

import com.authmat.application.authentication.exception.DuplicateCredentialException;
import com.authmat.application.authorization.entity.Role;
import com.authmat.application.users.dto.EmailUpdateRequest;
import com.authmat.application.users.dto.PasswordUpdateRequest;
import com.authmat.application.users.dto.UserDto;
import com.authmat.application.users.dto.UsernameUpdateRequest;
import com.authmat.application.users.entity.User;
import com.authmat.application.users.exception.CredentialUpdateException;
import com.authmat.application.users.exception.UserServiceException;
import com.authmat.application.users.repository.CachedUserRepository;
import com.authmat.application.util.UserMapper;
import com.authmat.events.NewUserEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
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


    public Optional<User> createUser(String username, String email, String password){
        return buildNewUser(
                username, email, password, null,null, null, null);
    }

    public Optional<User> createUser(
            String username,
            String email,
            String password,
            Collection<Role> roles,
            String provider,
            String providerId,
            String externalId){
        return buildNewUser(
                username, email, password, roles, provider, providerId, externalId);
    }

    public boolean createAndPublishUser(
            String username,
            String email,
            String password,
            Collection<Role> roles,
            String provider,
            String providerId,
            String externalId){
        try {
            User user = buildNewUser(
                    username, email, password, roles, provider, providerId, externalId)
            .orElseThrow(() -> new UserServiceException("User creation failed."));

            kafkaTemplate.send(
                    "user-created-events",
                    new NewUserEvent(user.getId(), user.getUsername(), user.getEmail(), Instant.now()));
            return true;
        } catch (Exception e) {
            log.warn("An error occurred while creating a new user.");
            log.debug("User creation error cause: {}", e.getMessage());
            return false;
        }
    }

    @Transactional
    public boolean updateUsername(UsernameUpdateRequest usernameUpdateRequest, Long userId){
        try {
            User user = fetchUserProxy(userId);

            UserDto dto = userMapper.entityToDto(user);
            String newUsername = usernameUpdateRequest.newUsername();

            validateCredential(
                    dto.getUsername(),
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
            User user = fetchUserProxy(userId);

            UserDto dto = userMapper.entityToDto(user);
            String newEmail = emailUpdateRequest.newEmail();

            validateCredential(
                    dto.getEmail(),
                    emailUpdateRequest.currentEmail(),
                    newEmail,
                    cachedUserRepository::existsByEmail);

            persistCredentialChange(user, newEmail, user::setEmail);

            log.info("User {} successfully updated email. ", dto.getId());
            return true;
        } catch (Exception e) {
            log.warn("Email update failure: {}", e.getMessage());
            return false;
        }
    }

    @Transactional
    public boolean updatePassword(PasswordUpdateRequest passwordUpdateRequest, Long userId){
        try {
            User userProxy = fetchUserProxy(userId);

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
            log.warn("Password update failure: {}", e.getMessage());
            return false;
        }
    }

    private User fetchUserProxy(Long userId){
        return cachedUserRepository.findUser(
                CachedUserRepository.ID_KEY + userId,
                true,
                userId,
                cachedUserRepository.userRepository()::findById,
                u -> u);
    }

    @Transactional
    private Optional<User> buildNewUser(
            String username,
            String email,
            String password,
            Collection<Role> roles,
            String provider,
            String providerId,
            String externalId){
        if(cachedUserRepository.existsByUsernameOrEmail(username, email)){
            throw new DuplicateCredentialException("User attempted to use taken credentials.");
        }

        User user = User.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(password))
                .roles(new HashSet<>())
                .provider(Optional.ofNullable(provider).orElse(""))
                .providerId(Optional.ofNullable(providerId).orElse(""))
                .externalId(Optional.ofNullable(externalId).orElse(""))
                .build();

        if(Objects.nonNull(roles) && !roles.isEmpty()){
            user.getRoles().addAll(roles);
        }
        return cachedUserRepository.save(user);
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
