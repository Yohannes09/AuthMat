package com.authmat.application.authorization.component;

import com.authmat.application.user.dto.UserDto;
import com.authmat.application.user.repository.UserCache;
import com.authmat.security.AuthorityResolver;
import org.springframework.security.core.GrantedAuthority;
import java.util.Set;

public class LocalAuthorityResolver implements AuthorityResolver {
    private final UserCache userCache;
    private final GrantedAuthorityAssembler assembler;

    public LocalAuthorityResolver(UserCache userCache, GrantedAuthorityAssembler assembler) {
        this.userCache = userCache;
        this.assembler = assembler;
    }

    @Override
    public Set<GrantedAuthority> resolve(String userId) {
        try {
            UserDto user = userCache.findByExternalId(userId).orElseThrow();
            return assembler.toGrantedAuthorities(user.roles());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
