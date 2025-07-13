package com.authmat.authentication.controller;

import com.authmat.authentication.component.RoleProvider;
import com.authmat.authentication.component.UserAccountManager;
import com.authmat.authentication.constant.Endpoints;
import com.authmat.authentication.dto.privileged.RoleAssignmentRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
@RequestMapping(Endpoints.Role.BASE)
@RequiredArgsConstructor
public class RoleController {
    private final RoleProvider roleProvider;
    private final UserAccountManager userAccountManager;

    @PostMapping(Endpoints.Role.DomainRole)
    public ResponseEntity<Void> addRole(){
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping(Endpoints.Role.DomainRole)
    public ResponseEntity<Void> removeRole(){
        return ResponseEntity.noContent().build();
    }

    @PostMapping(Endpoints.Role.AddUserRoles)
    public ResponseEntity<Void> addUserRoles(@RequestBody @Valid RoleAssignmentRequest request){
        userAccountManager.addRoles(request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping(Endpoints.Role.AddUserRoles)
    public ResponseEntity<Void> removeUserRoles(@RequestBody @Valid RoleAssignmentRequest request){
        userAccountManager.removeRoles(request);
        return ResponseEntity.noContent().build();
    }

}
