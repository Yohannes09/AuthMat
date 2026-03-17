package com.authmat.application.config;

import com.authmat.application.authorization.constant.DefaultRole;
import com.authmat.filter.SimpleJwtAuthenticationFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@Slf4j
public class SecurityConfig {
    private final AuthenticationProvider authenticationProvider;
    private final CorsConfigurationSource corsConfig;

    public SecurityConfig(AuthenticationProvider authenticationProvider, CorsConfigurationSource corsConfig) {
        this.authenticationProvider = authenticationProvider;
        this.corsConfig = corsConfig;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .securityMatcher(request ->
                        !request.getRequestURI().startsWith("/oauth2/") &&
                        !request.getRequestURI().startsWith("/login/oauth2/"))
                .cors(cors-> cors.configurationSource(corsConfig))
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(request ->
                        request
                                .requestMatchers("**/login").permitAll()
                                .requestMatchers("/swagger-ui/**","/v3/api-docs/**")
                                .hasAnyRole(
                                        DefaultRole.ADMIN.getName(),
                                        DefaultRole.SUPER_ADMIN.getName())
                                .anyRequest().authenticated())
                .sessionManagement(session->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(
                        ,
                        UsernamePasswordAuthenticationFilter.class)
                .build();
    }

}