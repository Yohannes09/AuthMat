package com.authmat.application.token;

import com.authmat.application.token.constant.SigningType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

//@Component
@ConfigurationProperties(prefix = "authmat.token")
@Validated
public record TokenProperties(
        @DurationUnit(ChronoUnit.MINUTES)
        @DurationMin(minutes = 1)
        Duration accessTokenTtl,

        @DurationUnit(ChronoUnit.MINUTES)
        @DurationMin(minutes = 1)
        Duration refreshTokenTtl,

        @NotNull @Valid AlgorithmProperties algorithm,

        @NotBlank String issuer,

        @NotBlank String audience,

        @NotNull SigningType signer,

        String keyId,

        @DurationUnit(ChronoUnit.MINUTES)
        @DurationMin(minutes = 1)
        Duration publicKeyTtl) // todo, i dont think i need this
{

        public record AlgorithmProperties(
                @NotBlank String keyAlgorithm, // EC
                @NotBlank String signatureAlgorithm, // ES256
                @NotBlank String curve // P-256
        ){}


        // KMS PRESENT -> FALSE, KID PRESENT -> (FALSE, TRUE) -> TRUE
        // KMS PRESENT -> FALSE, KID ABSENT -> (FALSE, FALSE) -> FALSE
        // LOCAL PRESENT -> TRUE, (SKIPS NEXT CONDITION) -> TRUE
        @AssertTrue(message = "KMS KID must be provided if using signing-type KMS")
        public boolean isKidPresentForKmsSigner(){
                return signer != SigningType.KMS || StringUtils.hasText(keyId);
        }

        @AssertTrue(message = "KID must be blank if using signing-type LOCAL")
        public boolean isKidAbsentForLocalSigner(){
                return signer != SigningType.LOCAL || !StringUtils.hasText(keyId);
        }

}
