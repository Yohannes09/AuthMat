package com.authmat.application.authentication.dto;

import com.authmat.application.constant.ValidationConstants;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record LoginRequest(
        @NotBlank(message = "Username or email cannot be blank. ")
        @Pattern(
                regexp = ValidationConstants.USERNAME_OR_EMAIL_PATTERN,
                message = ValidationConstants.USERNAME_OR_EMAIL_VALIDATION_MESSAGE
        )
        @Schema(
                description = "Username or email of the entity logging in. ",
                example = "Username123 or user@example.com"
        )
        String usernameOrEmail,


        @NotBlank(message = "Password cannot be blank. ")
        @Pattern(
                regexp = ValidationConstants.PASSWORD_PATTERN,
                message = ValidationConstants.PASSWORD_VALIDATION_MESSAGE
        )
        @Schema(
                description = "User authentication password. ",
                example = "Password123@"
        )
        String password
){}
