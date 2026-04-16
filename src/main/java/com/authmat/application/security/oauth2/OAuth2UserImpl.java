package com.authmat.application.security.oauth2;

import com.authmat.application.authorization.dto.RoleDto;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class OAuth2UserImpl implements OAuth2User {
    private final String identifier;
    private final Collection<RoleDto> authorities;
    private final Map<String, Object> attributes;

    public OAuth2UserImpl(
            String identifier,
            Collection<RoleDto> authorities,
            Map<String, Object> attributes) {
        this.identifier = identifier;
        this.authorities = authorities;
        this.attributes = attributes;
    }


    /**
     * Get the OAuth 2.0 token attributes
     *
     * @return the OAuth 2.0 token attributes
     */
    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    /**
     * Get the {@link Collection} of {@link GrantedAuthority}s associated with this OAuth
     * 2.0 token
     *
     * @return the OAuth 2.0 token authorities
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities.stream()
                .flatMap(role -> Stream.concat(
                        Stream.of(role)
                                .map(roleDto -> new SimpleGrantedAuthority("ROLE_" + roleDto.name())),
                        role.permissions().stream()
                                .map(permission -> new SimpleGrantedAuthority(permission.name()))
                ))
                .collect(Collectors.toSet());
    }

    /**
     * Returns the name of the authenticated <code>Principal</code>. Never
     * <code>null</code>.
     *
     * @return the name of the authenticated <code>Principal</code>
     */
    @Override
    public String getName() {
        return identifier;
    }

    /**
     * Get the OAuth 2.0 token attribute by name
     *
     * @param name the name of the attribute
     * @return the attribute or {@code null} otherwise
     */
    @Override
    public <A> A getAttribute(String name) {
        return OAuth2User.super.getAttribute(name);
    }

}
