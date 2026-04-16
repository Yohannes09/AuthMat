package com.authmat.application.security.principal;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.stream.Collectors;

public class UserContextPrincipal implements SecurityContextPrincipal{
    private final String identifier;
    private final Collection<String> authorities;

    public UserContextPrincipal(String identifier, Collection<String> authorities) {
        this.identifier = identifier;
        this.authorities = authorities;
    }


    @Override
    public String getIdentifier() {
        return identifier;
    }

    @Override
    public Collection<GrantedAuthority> getAuthorities() {
        return authorities.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toUnmodifiableList());
    }
}
