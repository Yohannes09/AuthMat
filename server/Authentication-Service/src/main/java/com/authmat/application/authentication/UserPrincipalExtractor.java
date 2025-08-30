package com.authmat.application.authentication;

import com.authmat.application.users.model.UserPrincipal;

import java.util.Optional;

@FunctionalInterface
public interface UserPrincipalExtractor {
    Optional<UserPrincipal> extract();
}
