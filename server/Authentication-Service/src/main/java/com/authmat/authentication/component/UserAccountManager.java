package com.authmat.authentication.component;

import com.authmat.authentication.constant.DefaultRoles;
import com.authmat.authentication.dto.privileged.RoleAssignmentRequest;
import com.authmat.authentication.util.Mapper;
import com.authmat.authentication.dto.UserDto;
import com.authmat.authentication.entity.Role;
import com.authmat.authentication.entity.User;
import com.authmat.authentication.exception.DuplicateCredentialException;
import com.authmat.authentication.exception.UserNotFoundException;
import com.authmat.authentication.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
public class UserAccountManager {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleProvider roleProvider;


    /**
     * Creates and persists a new {@link User} with the provided credentials and roles.
     * <p>
     * This method encodes the user's password, initializes account status flags,
     * and checks for duplicate username or email entries before saving.
     * </p>
     *
     * @param username the desired username for the new user; must be unique
     * @param email the user's email address; must be unique
     * @param password the raw password to be encoded and stored securely
     * @throws DuplicateCredentialException if the username or email already exists
     */
    public void createNewUser(String username, String email, String password){

        if(userRepository.existsByUsernameOrEmail(username, email)){
            throw new DuplicateCredentialException("Username or Email already registered. ");
        }

        Set<Role> defaultRoles = new HashSet<>();
        Role userRole = roleProvider.findRole(DefaultRoles.USER.getRole());
        defaultRoles.add(userRole);

        userRepository.save(
                User.builder()
                        .username(username)
                        .email(email)
                        .password(passwordEncoder.encode(password))
                        .roles(defaultRoles)
                        .accountNonExpired(true)
                        .accountNonLocked(true)
                        .credentialsNonExpired(true)
                        .enabled(true)
                        .build()
        );

    }

    // Not for production use, just to easily create default profiles.
    public void createNewUser(String username, String email, String password, Set<Role> roles){

        if(userRepository.existsByUsernameOrEmail(username, email)){
            throw new DuplicateCredentialException("Username or Email already registered. ");
        }

        userRepository.save(
                User.builder()
                        .username(username)
                        .email(email)
                        .password(passwordEncoder.encode(password))
                        .roles(roles)
                        .accountNonExpired(true)
                        .accountNonLocked(true)
                        .credentialsNonExpired(true)
                        .enabled(true)
                        .build()
        );

    }


    @Cacheable(cacheNames = "user", key = "#userId")
    public UserDto findById(Long userId){
        return userRepository
                .findById(userId)
                .map(Mapper::entityToDto)
                .orElseThrow(()-> new UserNotFoundException("User not found: " + userId));
    }


    @Cacheable(cacheNames = "user", key = "#usernameOrEmail")
    public UserDto findByUsernameOrEmail(String usernameOrEmail){
        return userRepository
                .findByUsernameOrEmail(usernameOrEmail)
                .map(Mapper::entityToDto)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + usernameOrEmail));
    }


    public User findEntityById(Long userId){
        return userRepository
                .findById(userId)
                .orElseThrow(()-> new UserNotFoundException("User not found: " + userId));
    }

    public void persistUserUpdate(User user){
        if (user.getId() == null)
            throw new RuntimeException("A user must be persisted before being updated. ");

        userRepository.save(user);
    }


    // Provides encapsulation and allows for future enhancements. (caching, logging, etc.)
    public boolean existsByUsername(String username){
        if(username.isBlank())
            throw new NullPointerException("Provided null/empty username parameter to existsByUsername()");

        return userRepository.existsByUsernameIgnoreCase(username);
    }
    public boolean existsByEmail(String email){
        return userRepository.existsByEmailIgnoreCase(email);
    }


    @Transactional
    public void addRoles(RoleAssignmentRequest request){
        User user = findEntityById(request.userId());

        Set<Role> roles = request.roleNames().stream()
                .map(roleProvider::findRole)
                .collect(Collectors.toSet());

        boolean changed = user.getRoles().addAll(roles);
        if (changed) {
            userRepository.save(user);
            log.info("Role(s) successfully added. ID: {}, New roles: {}", user.getId(), user.getRoles());
        }

    }

    @Transactional
    public void removeRoles(RoleAssignmentRequest request){
        User user = findEntityById(request.userId());

        boolean changed = user.getRoles().removeIf(role ->
                !request.roleNames().contains(role.getRole()) && !role.getRole().equals(DefaultRoles.USER.name())
        );

        if (changed) {
            userRepository.save(user);
            log.info("Role(s) successfully removed. ID: {}, New roles: {}", user.getId(), user.getRoles());
        }

    }

}
