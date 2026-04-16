package com.authmat.application.security.principal;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.stream.Collectors;

public class ServiceContextPrincipal implements SecurityContextPrincipal{
    private final String spiffeId;
    private final Collection<String> authorities;


    public ServiceContextPrincipal(String spiffeId, Collection<String> authorities) {
        this.spiffeId = spiffeId;
        this.authorities = authorities;
    }

    @Override
    public String getIdentifier() {
        return spiffeId;
    }

    @Override
    public Collection<GrantedAuthority> getAuthorities() {
        return authorities.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toUnmodifiableList());
    }
}
