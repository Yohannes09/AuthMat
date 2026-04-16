package com.authmat.application.security.oauth2;

import com.authmat.application.user.dto.UserDto;
import com.authmat.application.user.repository.UserCache;
import com.authmat.application.user.service.UserService;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collections;
import java.util.Map;

public class OAuth2UserService extends DefaultOAuth2UserService {
    private final UserService userService;
    private final UserCache userCache;

    public OAuth2UserService(UserService userService, UserCache userCache) {
        this.userService = userService;
        this.userCache = userCache;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String providerName = userRequest.getClientRegistration().getRegistrationId();
        Map<String,Object> attributes = oAuth2User.getAttributes();

        String email = (String) attributes.get("email");
        UserDto user = userCache
                .findByEmail(email)
                .orElseGet(()-> userService.provisionUser(
                        email,
                        email,
                        null,
                        providerName,
                        providerName,
                        Collections.emptyList())
                );
        return new OAuth2UserImpl(user.externalId(), user.roles(), attributes);
    }
}
