package com.authmat.application.user;

import com.authmat.application.authentication.exception.DuplicateCredentialException;
import com.authmat.application.outbox.UserEventPublisher;
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
    private final UserEventPublisher eventPublisher;

    public UserService(
            UserMapper userMapper,
            PasswordEncoder passwordEncoder,
            UserCache userCache,
            UserEventPublisher eventPublisher) {
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
                        savedUser.getExternalId(),
                        savedUser.getUsername(),
                        savedUser.getEmail()));

        return userMapper.entityToDto(savedUser);
    }


    // TODO: Update all update credential methods
//    @Transactional
//    public boolean updateUsername(UsernameUpdateRequest usernameUpdateRequest, Long userId){
//        try {
//            User user = new User();//fetchUserProxy(userId);
//
//            UserDto dto = userMapper.entityToDto(user);
//            String newUsername = usernameUpdateRequest.newUsername();
//
//            validateCredential(
//                    dto.username(),
//                    usernameUpdateRequest.currentUsername(),
//                    newUsername,
//                    userCache::existsByUsername);
//
//            persistCredentialChange(user, newUsername, user::updateUsername);
//
//            log.info("User {} successfully updated username. ", user.getId());
//            return true;
//        } catch (Exception e) {
//            log.warn("Username update failure: {}", e.getMessage());
//            return false;
//        }
//
//    }
//
//    @Transactional
//    public boolean updateEmail(EmailUpdateRequest emailUpdateRequest, Long userId){
//        try {
//            User user = new User();//fetchUserProxy(userId);
//
//            UserDto dto = userMapper.entityToDto(user);
//            String newEmail = emailUpdateRequest.newEmail();
//
//            validateCredential(
//                    dto.email(),
//                    emailUpdateRequest.currentEmail(),
//                    newEmail,
//                    userCache::existsByEmail);
//
//            persistCredentialChange(user, newEmail, user::updateEmail);
//
//            log.info("User {} successfully updated email. ", dto.externalId());
//            return true;
//        } catch (Exception e) {
//            log.warn("Email update failure: {}", e.getMessage());
//            return false;
//        }
//    }
//
//    @Transactional
//    public boolean updatePassword(PasswordUpdateRequest passwordUpdateRequest, Long userId){
//        try {
//            User user = new User();//fetchUserProxy(userId);
//
//            String currentPassword = user.getHashedPassword();
//            String newPassword = passwordEncoder.encode(passwordUpdateRequest.newPassword());
//
//            validateCredential(
//                    currentPassword,
//                    passwordUpdateRequest.currentPassword(),
//                    newPassword,
//                    null);
//
//            persistCredentialChange(user, newPassword, user::updatePassword);
//
//            log.info("User {} successfully updated password. ", user.getId());
//            return true;
//        } catch (Exception e) {
//            log.warn("Password update failure: {}", e.getMessage());
//            return false;
//        }
//    }
//
//
//    private void validateCredential(
//            String currentCredential,
//            String clientProvidedCredential,
//            String newCredential,
//            Predicate<String> uniquenessCheck){
//
//        if(currentCredential.equals(newCredential)){
//            throw new CredentialUpdateException("New credential cannot be the same as the current one. ");
//        }
//
//        if(!currentCredential.equals(clientProvidedCredential)){
//            throw new CredentialUpdateException("User provided the incorrect current credential.");
//        }
//
//        if(uniquenessCheck != null && uniquenessCheck.test(newCredential)){
//            throw new CredentialUpdateException(newCredential + " is already taken.");
//        }
//
//    }
//
//    private void persistCredentialChange(
//            User user, String credential, Consumer<String> setNewCredential){
//        setNewCredential.accept(credential);
//        userCache.save(user);
//    }

}
