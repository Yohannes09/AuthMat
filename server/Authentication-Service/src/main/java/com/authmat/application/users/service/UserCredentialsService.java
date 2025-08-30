package com.authmat.application.users.service;

import com.authmat.application.users.dto.EmailUpdateRequest;
import com.authmat.application.users.dto.PasswordUpdateRequest;
import com.authmat.application.users.dto.UsernameUpdateRequest;
import com.authmat.application.users.exception.CredentialUpdateException;
import com.authmat.application.users.model.User;
import com.authmat.application.users.exception.UserNotFoundException;
import com.authmat.application.users.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.function.Consumer;
import java.util.function.Predicate;

@Service
@Slf4j(topic = "USER_CREDENTIAL_SERVICE")
@Lazy
@RequiredArgsConstructor
public class UserCredentialsService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;


    @Transactional
    public void updateUsername(UsernameUpdateRequest usernameUpdateRequest){
        User user = findById(usernameUpdateRequest.id());
        String newUsername = usernameUpdateRequest.newUsername();

        validateCredential(user.getUsername(), newUsername, userRepository::existsByUsernameIgnoreCase);
        persistCredentialChange(user, newUsername, user::setUsername);

        log.info("User {} successfully updated username. ", user.getId());
    }


    @Transactional
    public void updateEmail(EmailUpdateRequest emailUpdateRequest){
        User user = findById(emailUpdateRequest.id());
        String newEmail = emailUpdateRequest.newEmail();

        validateCredential(user.getEmail(), newEmail, userRepository::existsByEmailIgnoreCase);
        persistCredentialChange(user, newEmail, user::setEmail);

        log.info("User {} successfully updated email. ", user.getId());
    }


    @Transactional
    public void updatePassword(PasswordUpdateRequest passwordUpdateRequest){
        User user = findById(passwordUpdateRequest.id());
        String newPassword = passwordEncoder.encode(passwordUpdateRequest.newPassword());

        validateCredential(user.getPassword(), newPassword, null);
        persistCredentialChange(user, newPassword, user::setPassword);

        log.info("User {} successfully updated password. ", user.getId());
    }

    private User findById(Long id){
        return userRepository
                .findById(id)
                .orElseThrow(()-> new UserNotFoundException("Unable to update credential. User not found: " + id));
    }

    private void validateCredential(
            String currentCredential, String newCredential, Predicate<String> uniquenessCheck
    ){
        if(currentCredential.equals(newCredential)){
            throw new CredentialUpdateException("New credential cannot be the same as the current one. ");
        }

        if(uniquenessCheck != null && uniquenessCheck.test(newCredential)){
            throw new CredentialUpdateException(newCredential + " is already taken.");
        }

    }


    private void persistCredentialChange(
            User user, String credential, Consumer<String> setNewCredential
    ){
        setNewCredential.accept(credential);
        userRepository.save(user);
    }

}
