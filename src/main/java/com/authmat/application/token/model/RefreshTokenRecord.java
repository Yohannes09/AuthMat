package com.authmat.application.token.model;

import java.time.Instant;
/**
 *  CURRENT DESIGN INTENTION:
 *  - CANNOT LEAVE SERVER*/
public record RefreshTokenRecord(
        String newRefreshToken,
        String externalId,
        Instant issuedAt
) {
}
