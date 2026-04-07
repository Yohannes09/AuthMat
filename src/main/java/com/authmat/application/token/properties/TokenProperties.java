package com.authmat.application.token.properties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

@ConfigurationProperties(prefix = "authmat.token")
@Validated
public record TokenProperties(
        @DurationUnit(ChronoUnit.SECONDS)
        @Positive
        Duration accessTokenTtl,

        @DurationUnit(ChronoUnit.SECONDS)
        @Positive
        Duration refreshTokenTtl,

        @NotBlank
        String issuer,

        @NotBlank
        String audience,

        @NotBlank
        String kmsKeyId
) {}
