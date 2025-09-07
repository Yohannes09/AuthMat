package com.authmat.application.authorization.controller;

import com.authmat.application.util.DefaultAuthoritiesInitializer;
import com.authmat.application.users.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
@RequestMapping("${endpoints.roles.base:/api/roles}")
@RequiredArgsConstructor
public class RoleController {
    private final DefaultAuthoritiesInitializer defaultAuthoritiesInitializer;
    private final UserService userService;

    @PostMapping("${endpoints.roles.create:}")
    public ResponseEntity<Void> addRole(){
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("${endpoints.roles.remove:}")
    public ResponseEntity<Void> removeRole(){
        return ResponseEntity.noContent().build();
    }

//    @PostMapping("${endpoints.roles.attach-to-user:}")
//    public ResponseEntity<Void> addUserRoles(@RequestBody @Valid RoleAssignmentRequest request){
//        userAccountManager.addRoles(request);
//        return ResponseEntity.noContent().build();
//    }
//
//    @DeleteMapping("${endpoints.roles.remove-from-user.:}")
//    public ResponseEntity<Void> removeUserRoles(@RequestBody @Valid RoleAssignmentRequest request){
//        userAccountManager.removeRoles(request);
//        return ResponseEntity.noContent().build();
//    }

}
