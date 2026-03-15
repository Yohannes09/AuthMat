package com.authmat.application.authentication.models;

import com.authmat.application.authorization.entity.Role;
import com.authmat.application.users.model.User;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class UserPrincipal implements UserDetails {
    private final Long internalId;
    private final String externalId;
    private final String username;
    private final String password;
    private final String email;
    private final Collection<Role> roles;
    private final boolean accountNonExpired;
    private final boolean accountNonLocked;
    private final boolean credentialsNonExpired;
    private final boolean enabled;


    public UserPrincipal(User user){
        this.internalId = user.getId();
        this.externalId = user.getExternalId().toString();
        this.username = user.getUsername();
        this.password = user.getHashedPassword();
        this.email = user.getEmail();
        this.roles = user.getRoles();
        this.accountNonExpired = user.isAccountNonExpired();
        this.accountNonLocked = user.isAccountNonLocked();
        this.credentialsNonExpired = user.isCredentialsNonExpired();
        this.enabled = user.isEnabled();
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

        Set<GrantedAuthority> roleAuthorities = roles
                .stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName()))
                .collect(Collectors.toSet());

        Set<GrantedAuthority> permissionAuthorities = roles.stream()
                .flatMap(role ->
                                role.getPermissions().stream()
                                        .map(permission ->
                                                new SimpleGrantedAuthority(permission.getName()))
                )
                .collect(Collectors.toSet());

        Set<GrantedAuthority> authorities = new HashSet<>(roleAuthorities);
        authorities.addAll(permissionAuthorities);

        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
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

}
