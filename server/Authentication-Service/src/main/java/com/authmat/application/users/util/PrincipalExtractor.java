package com.authmat.application.users.util;

import com.authmat.application.users.model.UserPrincipal;

public interface PrincipalExtractor {
    UserPrincipal extract();
}
