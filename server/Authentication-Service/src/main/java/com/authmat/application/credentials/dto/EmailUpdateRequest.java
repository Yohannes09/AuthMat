package com.authmat.application.credentials.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record EmailUpdateRequest(
        @NotNull
        Long id,

        @NotBlank
        @Email
        String newEmail
){}
