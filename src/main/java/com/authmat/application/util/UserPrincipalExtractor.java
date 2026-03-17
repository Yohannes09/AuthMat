package com.authmat.application.util;

import com.authmat.application.authentication.models.UserDetailsImpl;

import java.util.Optional;

@FunctionalInterface
public interface UserPrincipalExtractor {
    Optional<UserDetailsImpl> extract();
}
