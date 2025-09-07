package com.authmat.application.authentication.dto;

import com.authmat.application.constant.ValidationConstants;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record RegistrationRequest(
        @NotBlank(message = "Username cannot be blank.")
        @Pattern(
                regexp = ValidationConstants.USERNAME_PATTERN,
                message = ValidationConstants.USERNAME_VALIDATION_MESSAGE
        )
        @Schema(
                description = ValidationConstants.USERNAME_PATTERN,
                example = "Username123"
        )
        String username,

        @NotBlank(message = "Email cannot be blank.")
        @Email(message = ValidationConstants.EMAIL_VALIDATION_MESSAGE)
        @Schema(
                description = ValidationConstants.EMAIL_VALIDATION_MESSAGE,
                example = "user@example.com"
        )
        String email,

        @NotBlank(message = "Password cannot be blank.")
        @Pattern(
                regexp = ValidationConstants.PASSWORD_PATTERN,
                message = ValidationConstants.PASSWORD_VALIDATION_MESSAGE
        )
        @Schema(
                description = ValidationConstants.PASSWORD_PATTERN,
                example = "Password123@"
        )
        String password
){}
