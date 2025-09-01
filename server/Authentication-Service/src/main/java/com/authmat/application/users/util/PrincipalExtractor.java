package com.authmat.application.users.util;

import com.authmat.application.authentication.models.UserPrincipal;

public interface PrincipalExtractor {
    UserPrincipal extract();
}
