package com.authmat.application.config;

import com.authmat.application.authorization.constant.DefaultRole;
import com.authmat.application.properties.PublicPathsProperties;
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
    private final MtlsEnforcementFilter mtlsEnforcementFilter;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final AuthenticationProvider authenticationProvider;
    private final CorsConfigurationSource corsConfig;
    private final PublicPathsProperties publicPathsProperties;

//    public SecurityConfig(
//            MtlsEnforcementFilter mtlsEnforcementFilter,
//            JwtAuthenticationFilter jwtAuthenticationFilter,
//            AuthenticationProvider authenticationProvider,
//            CorsConfigurationSource corsConfig) {
//        this.mtlsEnforcementFilter = mtlsEnforcementFilter;
//        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
//        this.authenticationProvider = authenticationProvider;
//        this.corsConfig = corsConfig;
//    }


    public SecurityConfig(MtlsEnforcementFilter mtlsEnforcementFilter, JwtAuthenticationFilter jwtAuthenticationFilter, AuthenticationProvider authenticationProvider, CorsConfigurationSource corsConfig, PublicPathsProperties publicPathsProperties) {
        this.mtlsEnforcementFilter = mtlsEnforcementFilter;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.authenticationProvider = authenticationProvider;
        this.corsConfig = corsConfig;
        this.publicPathsProperties = publicPathsProperties;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        String[] publicPaths = publicPathsProperties
                                    .publicPaths()
                                    .values()
                                    .toArray(String[]::new);

        return http
                .securityMatcher(request ->
                        !request.getRequestURI().startsWith("/oauth2/") &&
                        !request.getRequestURI().startsWith("/login/oauth2/"))
                .cors(cors-> cors.configurationSource(corsConfig))
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(request ->
                        request
                                .requestMatchers(publicPaths).permitAll()
                                .requestMatchers("/swagger-ui/**","/v3/api-docs/**")
                                .hasAnyRole(
                                        DefaultRole.SUPPORT.getName(),
                                        DefaultRole.ADMIN.getName(),
                                        DefaultRole.SUPER_ADMIN.getName()
                                )
                                .anyRequest().authenticated())
                .sessionManagement(
                        session-> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(
                        mtlsEnforcementFilter,
                        UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(
                        jwtAuthenticationFilter,
                        MtlsEnforcementFilter.class)
                .build();
    }

}