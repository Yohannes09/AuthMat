package com.authmat.application.authentication.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "authmat.registration")
@Validated
public record registrationProperties(
        @NotBlank
        String provider,

        @NotBlank
        String providerId
) {
}
