package com.authmat.application.authentication.loginAttemptManager;

import jakarta.validation.constraints.Min;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

@Validated
@ConfigurationProperties(prefix = "security.login-attempt")
public class LoginAttemptProperties {
    @Min(value = 1, message = "A value of at least 1 must be set for maxFailedAttempts")
    private int maxFailedLoginAttempts;

    @DurationUnit(ChronoUnit.MINUTES)
    @DurationMin(minutes = 1)
    private Duration failedLoginLockoutMins;

    public LoginAttemptProperties(int  maxFailedLoginAttempts, int failedLoginLockoutMins) {
        this.maxFailedLoginAttempts = maxFailedLoginAttempts;
        this.failedLoginLockoutMins = Duration.ofMinutes(failedLoginLockoutMins);
    }


    public int getMaxFailedLoginAttempts() {
        return maxFailedLoginAttempts;
    }
    public Duration getFailedLoginLockoutMins() {
        return failedLoginLockoutMins;
    }
}
