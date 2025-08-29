package com.authmat.application.authentication.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@Slf4j
public class OAuth2SecurityFilterChainConfig {
    @PostConstruct
    public void init(){
        System.out.println("*** SecurityConfig loaded ***");
    }

    @Bean
    @Profile("prod")
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception{
//        http
//                .authorizeHttpRequests(request -> request
//                        .requestMatchers("/api/public").permitAll()
//                        .anyRequest().authenticated()
//                )
//                .oauth2Login(oAuth2LoginConfigurer -> oAuth2LoginConfigurer
//                        .userInfoEndpoint(userInfo -> userInfo.
//                                userService()
//                        )
//                        .successHandler(new OAuth2AuthenticationSuccessHandler())
//
//                );

        return http.build();
    }

    @Bean("noSecurityOAuth2FilterChain")
    @Profile("dev")
    public SecurityFilterChain noSecurityFilterChain(HttpSecurity http) throws Exception {
        log.info("OAuth2 Secur");
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(
                        request -> request.anyRequest().permitAll()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        return http.build();
    }
}
