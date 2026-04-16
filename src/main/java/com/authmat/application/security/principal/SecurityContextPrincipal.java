package com.authmat.application.security.principal;

import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

public interface SecurityContextPrincipal {
    String getIdentifier(); // User: external id/uuid Service: spiffe_id
    Collection<GrantedAuthority> getAuthorities();
}
