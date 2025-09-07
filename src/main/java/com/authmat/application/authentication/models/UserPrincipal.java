package com.authmat.application.authentication.models;

import com.authmat.application.authorization.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
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

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class UserPrincipal implements UserDetails {
    private Long id;
    private String username;
    private String password;
    private String email;
    private Set<Role> roles = new HashSet<>();
    private boolean accountNonExpired;
    private boolean accountNonLocked;
    private boolean credentialsNonExpired;
    private boolean enabled;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {

        Set<GrantedAuthority> roleAuthorities = roles
                .stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName()))
                .collect(Collectors.toSet());

        Set<GrantedAuthority> permissionAuthorities = roles
                .stream()
                .flatMap(role ->
                                role
                                        .getPermissions()
                                        .stream()
                                        .map(permission -> new SimpleGrantedAuthority(permission.getName()))
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
        if (isAccountNonLocked()) {
            throw new LockedException("Account is locked");
        }
        if (isEnabled()) {
            throw new DisabledException("Account is disabled");
        }
        if (isAccountNonExpired()) {
            throw new AccountExpiredException("Account has expired");
        }
        if (isCredentialsNonExpired()) {
            throw new CredentialsExpiredException("Credentials have expired");
        }
    }

}
