package com.authmat.application.users.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EmailUpdateRequest(
        @NotBlank
        @Email
        String currentEmail,

        @NotBlank
        @Email
        String newEmail
){}
