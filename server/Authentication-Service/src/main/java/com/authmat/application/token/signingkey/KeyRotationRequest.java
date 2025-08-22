package com.authmat.application.token.signingkey;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Represents a request to rotate the active signing key, intended for users with elevated privileges.
 *
 * <p>Includes a single field - reason, which must be a non-blank string
 * between 5 and 250 characters, explaining why the key rotation is necessary.</p>
 *
 * <p>This could be expanded with fields like user ID or timestamps, and
 * integrated with a persistence layer for tracking and auditing of key rotations.</p>
 *
 * @param reason The concise, valid explanation (5-250 characters) for the signing key rotation.
 */
public record KeyRotationRequest(
        @NotBlank(message = "Reason cannot be blank. ")
        @Size(min = 5, max = 250, message = "Must provide a valid reason for signing key rotation.")
        String reason
){}

