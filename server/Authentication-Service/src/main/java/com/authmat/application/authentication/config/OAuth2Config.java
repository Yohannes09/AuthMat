package com.authmat.application.authentication.config;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class OAuth2Config {
    @PostConstruct
    public void init(){
        System.out.println("*** SecurityConfig loaded ***");
    }

    @Bean
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
}
