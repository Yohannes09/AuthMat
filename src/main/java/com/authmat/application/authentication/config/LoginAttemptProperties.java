package com.authmat.application.authentication.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "security.login-attempt")
public class LoginAttemptProperties {
    @Min(value = 1, message = "A value of at least 1 must be set for maxFailedAttempts")
    private int maxFailedAttempts;

    @NotNull
    private Duration lockoutDuration = Duration.ofMinutes(15);

    public int getMaxFailedAttempts() {
        return maxFailedAttempts;
    }

    public Duration getLockoutDuration() {
        return lockoutDuration;
    }
}
