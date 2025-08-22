package com.authmat.application.authorization.service;

import com.authmat.application.authorization.entity.Permission;
import com.authmat.application.authorization.entity.Role;
import com.authmat.application.authorization.persistence.RolePermissionRepository;
import com.authmat.application.users.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserRolePermissionService {
    private final UserRepository userRepository;
    private final RolePermissionRepository rolePermissionRepository;


    // Repeated similar-like logic.
    public UserDto assignRolesToUser(Long userId, List<Long> ids){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found."));

        Set<Role> roles = rolePermissionRepository.findRolesById(ids);

        user.getRoles().addAll(roles);
        userRepository.save(user);

        return UserMapper.entityToDto(user);
    }

    public UserDto assignPermissionsToUser(Long userId, List<Long> ids){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found."));

        Set<Permission> permissions = rolePermissionRepository.findPermissionsById(ids);

        //user.getPermissions().addAll(permissions);
        userRepository.save(user);

        return UserMapper.entityToDto(user);
    }

    //public

//    public <RoleRepo, PermissionRepo> User assignUserPrivileges(
//            Function<RoleRepo, >,
//    ){
//
//    }
}
