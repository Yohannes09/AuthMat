package com.authmat.application.util;

import com.authmat.application.authentication.models.UserPrincipal;

import java.util.Optional;

@FunctionalInterface
public interface UserPrincipalExtractor {
    Optional<UserPrincipal> extract();
}
