package com.authmat.application.authentication.models;

import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Builder
@RequiredArgsConstructor
public class OAuth2UserImpl implements OAuth2User {
    private final OAuth2User oAuth2User;
    private final UserDetailsImpl userDetailsImpl;


    public String getId(){
        return userDetailsImpl.getExternalId();
    }

    public Set<String> getAuthoritiesToString(){
        return userDetailsImpl.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
    }

    @Override
    public String getName() {
        return userDetailsImpl.getUsername();
    }

    /**
     * Get the OAuth 2.0 token attribute by name
     *
     * @param name the name of the attribute
     * @return the attribute or {@code null} otherwise
     */
    @Override
    public <A> A getAttribute(String name) {
        return oAuth2User.getAttribute(name);
    }

    /**
     * Get the {@link Collection} of {@link GrantedAuthority}s associated with this OAuth
     * 2.0 token
     *
     * @return the OAuth 2.0 token authorities
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return userDetailsImpl.getAuthorities();
    }

    /**
     * Get the OAuth 2.0 token attributes
     *
     * @return the OAuth 2.0 token attributes
     */
    @Override
    public Map<String, Object> getAttributes() {
        return oAuth2User.getAttributes();
    }

}
