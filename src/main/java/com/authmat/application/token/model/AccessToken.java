package com.authmat.application.token.model;

public record AccessToken(
        String accessToken,
        String tokenType,
        long expiresIn  // seconds until expiry, per RFC 6749
) {
    private static final String BEARER = "BEARER";

    public static AccessToken of(String token, long expiresIn){
        return new AccessToken(token, BEARER, expiresIn);
    }
}
