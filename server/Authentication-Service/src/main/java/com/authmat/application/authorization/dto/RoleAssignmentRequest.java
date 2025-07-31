package com.authmat.application.authorization.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Collection;

public record RoleAssignmentRequest (
    @NotNull(message = "User ID cannot be null.")
    Long userId,

    @NotNull(message = "Role list cannot be null.")
    @Size(min = 1, message = "At least ONE role must be specified.")
    Collection<@NotBlank(message = "Roles cannot be blank.") String> roleNames
){}