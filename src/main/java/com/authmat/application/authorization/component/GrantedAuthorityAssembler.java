package com.authmat.application.authorization.component;

import com.authmat.application.authorization.dto.RoleDto;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class GrantedAuthorityAssembler {
    // Note
    // Maybe a common type to avoid creating many methods. Early optimization not sure if needed yet
    // Type could be InternalAuthority or Authority, too early to know

    public Set<GrantedAuthority> toGrantedAuthorities(Collection<RoleDto> roles){
        return roles.stream()
                .flatMap(role -> Stream.concat(
                        Stream.of(new SimpleGrantedAuthority("ROLE_"+role.name())),
                        role.permissions().stream().map(permission -> new SimpleGrantedAuthority(permission.name()))
                )).collect(Collectors.toUnmodifiableSet());
    }
}
