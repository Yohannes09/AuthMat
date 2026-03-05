package com.authmat.application.users.request;

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
