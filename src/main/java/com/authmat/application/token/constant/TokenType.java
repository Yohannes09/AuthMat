package com.authmat.application.token.constant;

import java.util.EnumSet;
import java.util.Set;

// TODO: a more appropriate name for this could be Token Principal
public enum TokenType {
    ACCESS,
    REFRESH,
    SERVICE;

    public Set<TokenType> getTokenTypes() {
        return EnumSet.allOf(TokenType.class);
    }
}
