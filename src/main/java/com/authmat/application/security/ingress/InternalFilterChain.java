package com.authmat.application.security.ingress;

import com.authmat.application.authorization.constant.DefaultRole;
import com.authmat.application.security.properties.PublicPathsProperties;
import lombok.extern.slf4j.Slf4j;
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
public class InternalFilterChain {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final AuthenticationProvider authenticationProvider;
    private final CorsConfigurationSource corsConfig;
    private final PublicPathsProperties publicPathsProperties;


    public InternalFilterChain(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            AuthenticationProvider authenticationProvider,
            CorsConfigurationSource corsConfig,
            PublicPathsProperties publicPathsProperties) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.authenticationProvider = authenticationProvider;
        this.corsConfig = corsConfig;
        this.publicPathsProperties = publicPathsProperties;
    }

    @Bean
    @Order(2)
    public SecurityFilterChain internalChain(HttpSecurity http) throws Exception {
        String[] publicPaths = this.publicPathsProperties.getPublicPathsArr();

        return http
                .securityMatcher("/**")
                .cors(cors-> cors.configurationSource(corsConfig))
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(request ->
                        request
                                .requestMatchers(publicPaths).permitAll()
                                .requestMatchers("/swagger-ui/**","/v3/api-docs/**", "/actuator/**")
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
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class)
                .build();
    }

}