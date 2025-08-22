package com.authmat.application.users;

import com.authmat.application.authentication.DuplicateCredentialException;
import com.authmat.application.authorization.entity.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@Slf4j
@RequiredArgsConstructor
public class UserAccountManager {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    //private final DefaultRolesAndPermissionsInitializer defaultRolesAndPermissionsInitializer;


    /**
     * Creates and persists a new {@link User} with the provided credentials and roles.
     * @param username
     * @param email
     * @param password
     * @throws DuplicateCredentialException if the username or email already exists.
     */
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

    public User findById(Long userId){
        return userRepository
                .findById(userId)
                .orElseThrow(()-> new UserNotFoundException("User not found: " + userId));
    }


    public UserDto findByUsernameOrEmail(String usernameOrEmail){
        return userRepository
                .findByUsernameOrEmail(usernameOrEmail)
                .map(UserMapper::entityToDto)
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


    public boolean existsByUsername(String username){
        if(username.isBlank())
            throw new NullPointerException("Provided null/empty username parameter to existsByUsername()");

        return userRepository.existsByUsernameIgnoreCase(username);
    }
    public boolean existsByEmail(String email){
        return userRepository.existsByEmailIgnoreCase(email);
    }


//    @Transactional
//    public void addRoles(RoleAssignmentRequest request){
//        User user = findEntityById(request.userId());
//
////        Set<Role> roles = request.roleNames().stream()
////                .map(defaultRolesAndPermissionsInitializer::findRole)
////                .collect(Collectors.toSet());
////
////        boolean changed = user.getRoles().addAll(roles);
////        if (changed) {
////            userRepository.save(user);
////            log.info("Role(s) successfully added. ID: {}, New roles: {}", user.getId(), user.getRoles());
////        }
//
//    }
//
//    @Transactional
//    public void removeRoles(RoleAssignmentRequest request){
//        User user = findEntityById(request.userId());
//
//        boolean changed = user.getRoles().removeIf(role ->
//                !request.roleNames().contains(role.getName()) && !role.getName().equals(DefaultRoles.USER.name())
//        );
//
//        if (changed) {
//            userRepository.save(user);
//            log.info("Role(s) successfully removed. ID: {}, New roles: {}", user.getId(), user.getRoles());
//        }
//
//    }

}
