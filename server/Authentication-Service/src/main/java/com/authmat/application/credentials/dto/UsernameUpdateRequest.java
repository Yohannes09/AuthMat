package com.authmat.application.credentials.dto;

import com.authmat.application.constant.ValidationConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record UsernameUpdateRequest (
        @NotNull
        Long id,

        @NotBlank(message = ValidationConstants.USERNAME_VALIDATION_MESSAGE)
        @Pattern(
                regexp = ValidationConstants.USERNAME_PATTERN,
                message = ValidationConstants.USERNAME_VALIDATION_MESSAGE
        )
        String newUsername
){}
