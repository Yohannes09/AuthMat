package com.authmat.application.token.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

// TODO: decide a good prefix. maybe "authmat.token"???
@ConfigurationProperties(prefix = "")
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
