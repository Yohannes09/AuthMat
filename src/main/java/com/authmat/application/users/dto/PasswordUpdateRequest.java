package com.authmat.application.users.dto;

import com.authmat.application.constant.ValidationConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record PasswordUpdateRequest(
    @NotNull(message = "Must provide a current password to complete request.")
    String currentPassword,

    @NotBlank(message = ValidationConstants.PASSWORD_VALIDATION_MESSAGE)
    @Pattern(
            regexp = ValidationConstants.PASSWORD_PATTERN,
            message = ValidationConstants.PASSWORD_VALIDATION_MESSAGE)
    String newPassword){
}
