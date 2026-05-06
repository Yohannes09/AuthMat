package com.authmat.application.user.model;

import com.authmat.application.authorization.model.RoleDto;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.stream.Stream;

public class UserDetailsImpl implements UserDetails {
    private final Long internalId;
    private final String externalId;
    private final String username;
    private final String hashedPassword;
    private final String email;
    private final Collection<RoleDto> roles;
    private final boolean accountNonExpired;
    private final boolean accountNonLocked;
    private final boolean credentialsNonExpired;
    private final boolean enabled;

    public UserDetailsImpl(
            Long internalId,
            String externalId,
            String username,
            String hashedPassword,
            String email,
            Collection<RoleDto> roles,
            boolean accountNonExpired,
            boolean accountNonLocked,
            boolean credentialsNonExpired,
            boolean enabled) {
        this.internalId = internalId;
        this.externalId = externalId;
        this.username = username;
        this.hashedPassword = hashedPassword;
        this.email = email;
        this.roles = roles;
        this.accountNonExpired = accountNonExpired;
        this.accountNonLocked = accountNonLocked;
        this.credentialsNonExpired = credentialsNonExpired;
        this.enabled = enabled;
    }


    public void validateAccount(){
        if (!isAccountNonLocked()) {
            throw new LockedException("Account is locked");
        }
        if (!isEnabled()) {
            throw new DisabledException("Account is disabled");
        }
        if (!isAccountNonExpired()) {
            throw new AccountExpiredException("Account has expired");
        }
        if (!isCredentialsNonExpired()) {
            throw new CredentialsExpiredException("Credentials have expired");
        }
    }

    public Long getInternalId() {
        return internalId;
    }

    public String getEmail() {
        return email;
    }

    public String getExternalId() {
        return externalId;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
                .flatMap(roleDto -> Stream.concat(
                        Stream.of(roleDto)
                                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name())),

                        roleDto.permissions().stream()
                                .map(permission -> new SimpleGrantedAuthority(permission.name()))
                )).toList();
    }

    @Override
    public String getPassword() {
        return hashedPassword;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return accountNonExpired;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return credentialsNonExpired;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

}
