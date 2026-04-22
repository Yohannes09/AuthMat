package com.authmat.application.security.ingress;

import com.authmat.application.authorization.constant.DefaultRole;
import com.authmat.application.security.properties.PublicPathsProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
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
@ConditionalOnProperty(name = "security.mtls-enabled", havingValue = "true")
public class InternalMtlsFilterChain {
    private final MtlsEnforcementFilter mtlsEnforcementFilter;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final AuthenticationProvider authenticationProvider;
    private final CorsConfigurationSource corsConfig;
    private final PublicPathsProperties publicPathsProperties;


    public InternalMtlsFilterChain(
            MtlsEnforcementFilter mtlsEnforcementFilter,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            AuthenticationProvider authenticationProvider,
            CorsConfigurationSource corsConfig,
            PublicPathsProperties publicPathsProperties) {
        this.mtlsEnforcementFilter = mtlsEnforcementFilter;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.authenticationProvider = authenticationProvider;
        this.corsConfig = corsConfig;
        this.publicPathsProperties = publicPathsProperties;
    }

    @Bean
    @Order(2)
    public SecurityFilterChain internalChain(HttpSecurity http) throws Exception {
        String[] publicPaths = publicPathsProperties
                .publicPaths()
                .values()
                .toArray(String[]::new);

        return http
                // TODO: eventually come up with organized way of giving each filter chain a dedicated path matcher
                .securityMatcher("/auth/**", "/api/**")
                .cors(cors-> cors.configurationSource(corsConfig))
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(request ->
                        request
                                .requestMatchers(publicPaths).permitAll()
                                .requestMatchers("/swagger-ui/**","/v3/api-docs/**")
                                .hasAnyRole(
                                        DefaultRole.SUPPORT.trimmedName(),
                                        DefaultRole.ADMIN.trimmedName(),
                                        DefaultRole.SUPER_ADMIN.trimmedName()
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
