package com.authmat.application.config;

import com.authmat.application.authentication.token.config.TokenSigningConfig;
import com.authmat.application.authentication.token.history.PublicKeyHistory;
import com.authmat.application.authentication.token.service.TokenService;
import com.authmat.application.authorization.constant.DefaultRole;
import com.authmat.application.users.UserRepository;
import com.authmat.application.users.model.User;
import com.authmat.application.users.model.UserPrincipal;
import com.authmat.application.users.util.UserMapper;
import com.authmat.client.PublicKeyResolver;
import com.authmat.filter.SimpleJwtAuthenticationFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;


@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@Slf4j
public class SecurityConfig {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final TokenService tokenService;
    private final PublicKeyHistory publicKeyHistory;

    public SecurityConfig(
            UserRepository userRepository,
            UserMapper userMapper,
            TokenService tokenService,
            @Qualifier(TokenSigningConfig.ACCESS_KEY_HISTORY_BEAN_NAME)
            PublicKeyHistory publicKeyHistory) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.tokenService = tokenService;
        this.publicKeyHistory = publicKeyHistory;
    }

    @Bean
    @Profile("prod")
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .cors(cors-> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(request ->
                        request
                                .requestMatchers("/auth/**").permitAll()
                                .anyRequest().authenticated()
                                .requestMatchers("/swagger-ui/**","/v3/api-docs/**")
                                .hasAnyRole(
                                        DefaultRole.ADMIN.getName(),
                                        DefaultRole.SUPER_ADMIN.getName()))
                .sessionManagement(session->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider(
                        passwordEncoder(),
                        userDetailsService()))
                .addFilterBefore(
                        simpleAuthenticationFilter(),
                        UsernamePasswordAuthenticationFilter.class)
                .securityMatcher("/auth/v1/login")
                .build();
    }

    @Bean
    @Profile("prod")
    public SecurityFilterChain oAuth2SecurityFilterChain(HttpSecurity http) throws Exception{
        return http
                .authorizeHttpRequests(request -> request
                        .requestMatchers("/api/public").permitAll()
                        .anyRequest().authenticated())
                .authenticationProvider(authenticationProvider(
                        passwordEncoder(), userDetailsService()))
                .oauth2Login(oAuth2LoginConfigurer ->
                        oAuth2LoginConfigurer
//                        .userInfoEndpoint(userInfo -> userInfo.
//                                userService())
                                .successHandler(oAuth2SuccessHandler()))
                .securityMatcher("/oauth2-login")
                .build();
    }

    @Bean
    @Profile("dev")
    public SecurityFilterChain noSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .cors(cors-> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(request ->
                        request.anyRequest().permitAll())
                .sessionManagement(session->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .build();
    }

    @Bean("noSecurityOAuth2FilterChain")
    @Profile("dev")
    public SecurityFilterChain oAuth2NoSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(
                        request -> request.anyRequest().permitAll())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .build();
    }

    @Bean
    @Lazy
    public CorsConfigurationSource corsConfigurationSource(){
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config); // Apply CORS settings across all endpoints

        return source;
    }

    @Bean
    @Lazy
    public OncePerRequestFilter simpleAuthenticationFilter(){
        return new SimpleJwtAuthenticationFilter(publicKeyResolver(), Set.of(""));
    }

    @Bean
    @Lazy
    public PublicKeyResolver publicKeyResolver(){
        return kid ->
            publicKeyHistory.getKeyHistoryAscending().stream()
                    .filter(publicKey-> kid.equals(publicKey.getId().toString()))
                    .findFirst();
    }

    @Bean
    public UserDetailsService userDetailsService(){
        return usernameOrEmail -> userRepository
                .findByUsernameOrEmail(usernameOrEmail)
                .map(userMapper::entityToPrincipal)
                .orElseThrow();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authConfiguration) throws Exception {
        return authConfiguration.getAuthenticationManager();
    }

    @Bean
    public AuthenticationProvider authenticationProvider(
            PasswordEncoder passwordEncoder,
            UserDetailsService userDetailsService){
        DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider();
        authenticationProvider.setUserDetailsService(userDetailsService);
        authenticationProvider.setPasswordEncoder(passwordEncoder);

        return authenticationProvider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationSuccessHandler oAuth2SuccessHandler(){
        return (request, response, authentication) -> {
            response.setContentType("application/json");
            try {
                if(authentication.getPrincipal() instanceof OAuth2User auth2User) {

                    String subject = Optional.of(auth2User.getName())
                            .orElseThrow(() -> new IllegalArgumentException("OAuth2 User did not provide a username or email"));

                    User user = userRepository.findByUsernameOrEmail(subject)
                            .orElse(userRepository.save(User.builder().username(subject).build()));

                    UserPrincipal principal = userMapper.entityToPrincipal(user);

                    Set<String> authorities = principal.getAuthorities()
                            .stream()
                            .map(GrantedAuthority::getAuthority)
                            .collect(Collectors.toSet());

                    String accessToken = tokenService.generateAccessToken(user.getId().toString(), authorities);
                    String refreshToken = tokenService.generateRefreshToken(user.getId().toString());

                    response.getWriter().write("""
                            {
                                "accessToken": "%s",
                                "refreshToken": "%s"
                            }
                            """.formatted(accessToken, refreshToken));
                }
                throw new IllegalArgumentException("Type mismatch, Expected type: " + OAuth2User.class.getName());

            } catch (Exception e) {
                log.warn("OAuth2 login attempt failed: {}", e.getMessage());
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                response.getWriter().write("""
                        {
                            "status": "error",
                            "error": {
                                "message": "UNAUTHORIZED",
                                "code": "OAUTH2_FAILED"
                            }
                        }
                        """);
            }
        };
    }

}



