package com.authmat.application.security.oauth2;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Slf4j
@Configuration
public class OAuth2FilterChain {
    private final AuthenticationProvider authenticationProvider;
    private final OAuth2UserService oAuth2UserService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;

    public OAuth2FilterChain(
            AuthenticationProvider authenticationProvider,
            OAuth2UserService oAuth2UserService,
            OAuth2SuccessHandler oAuth2SuccessHandler) {
        this.authenticationProvider = authenticationProvider;
        this.oAuth2UserService = oAuth2UserService;
        this.oAuth2SuccessHandler = oAuth2SuccessHandler;
    }

    @Bean
    @Order(1)
    public SecurityFilterChain oAuth2Chain(HttpSecurity http) throws Exception{
        return http
                .securityMatcher("/oauth2/**", "/login/oauth2/**")
                .csrf(AbstractHttpConfigurer::disable)
                // The OAuth2 code flow requires a session to survive redirect
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authenticationProvider(authenticationProvider)
                .oauth2Login(loginConfigurer ->
                        loginConfigurer.userInfoEndpoint(userInfo ->
                                        userInfo.userService(oAuth2UserService))
                                .successHandler(oAuth2SuccessHandler))
                .authorizeHttpRequests(request ->
                        request.anyRequest().authenticated())
                .build();
    }

}
