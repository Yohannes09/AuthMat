package com.authmat.application.config;

import com.authmat.application.authentication.exception.FailedAuthencticationException;
import com.authmat.application.authentication.models.CustomOAuth2User;
import com.authmat.application.authentication.models.UserPrincipal;
import com.authmat.application.authorization.constant.DefaultRole;
import com.authmat.application.token.history.PublicKeyHistory;
import com.authmat.application.token.service.TokenService;
import com.authmat.application.users.model.User;
import com.authmat.application.users.model.UserDto;
import com.authmat.application.users.repository.CachedUserRepository;
import com.authmat.application.util.UserMapper;
import com.authmat.client.PublicKeyResolver;
import com.authmat.filter.SimpleJwtAuthenticationFilter;
import com.authmat.tool.exception.UserNotFoundException;
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
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
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


@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@Slf4j
public class SecurityConfig {
    private final CachedUserRepository userRepository;
    private final UserMapper userMapper;
    private final TokenService tokenService;
    private final PublicKeyHistory publicKeyHistory;

    public SecurityConfig(
            CachedUserRepository userRepository,
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
        log.info("Secured filter chain loaded.");
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
        log.info("Secured OAuth2SecurityFilter loaded");
        return http
                .securityMatcher("/oauth2/**", "/login/oauth2/**")
                .authorizeHttpRequests(request -> request
                        .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                        .anyRequest().authenticated())
                .authenticationProvider(authenticationProvider(
                        passwordEncoder(), userDetailsService()))
                .oauth2Login(loginConfigurer -> loginConfigurer
                        //.loginPage("http://localhost:8080/oauth2/authorization/google")
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(oAuth2UserService()))
                        .successHandler(oAuth2SuccessHandler()))
                .sessionManagement(sessionConfigurer -> sessionConfigurer
                        .sessionCreationPolicy(SessionCreationPolicy.ALWAYS))
                .build();
    }

//    @Bean
//    @Order(1)
//    @Profile("dev")
//    public SecurityFilterChain noSecurityFilterChain(HttpSecurity http) throws Exception {
//        log.info("Unsecured filter chain loaded");
//        return http
//                .cors(cors-> cors.configurationSource(corsConfigurationSource()))
//                .csrf(AbstractHttpConfigurer::disable)
//                .authorizeHttpRequests(request ->
//                        request.anyRequest().permitAll())
//                .sessionManagement(session->
//                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
//                .build();
//    }

    @Bean("noSecurityOAuth2FilterChain")
    @Profile("dev")
    public SecurityFilterChain oAuth2NoSecurityFilterChain(
            HttpSecurity http,
            OAuth2UserService<OAuth2UserRequest, OAuth2User> oAuth2UserService) throws Exception {

        log.info("Loaded OAuth2 no security filter chain");
        return http
                .securityMatcher("/oauth2/**", "/login/oauth2/**")
                .authorizeHttpRequests(
                        request -> request
                                .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll())
                .oauth2Login(loginConfigurer -> loginConfigurer
                        .userInfoEndpoint(userInfoEndpointConfig ->
                                userInfoEndpointConfig.userService(oAuth2UserService))
                        .successHandler(oAuth2SuccessHandler()))
                .sessionManagement(sessionConfigurer -> sessionConfigurer
                        .sessionCreationPolicy(SessionCreationPolicy.ALWAYS))
                .csrf(AbstractHttpConfigurer::disable)
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
    @Profile("prod")
    public OncePerRequestFilter simpleAuthenticationFilter(){
        return new SimpleJwtAuthenticationFilter(publicKeyResolver(), Set.of("/auth/v1/login", "/auth/v1/register"));
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
                .map(userMapper::dtoToPrincipal)
                .orElseThrow(() -> new UserNotFoundException("User not found " + usernameOrEmail));
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
    public OAuth2UserService<OAuth2UserRequest, OAuth2User> oAuth2UserService(){
        return userRequest -> {
            log.info("Hello from OAuth2UserService");

            OAuth2User oAuth2User = new DefaultOAuth2UserService().loadUser(userRequest);

            String provider = userRequest.getClientRegistration().getRegistrationId();
            String providerId = oAuth2User.getName();

            String email = Optional.ofNullable(oAuth2User.getAttribute("email"))
                    .map(String.class::cast)
                    .orElseThrow(() -> new FailedAuthencticationException("""
                            Field 'email' was not provided in attributes.
                            """));

            UserDto userDto = userRepository.findByUsernameOrEmail(email)
                    .orElseGet(()-> {
                        log.info("New user registered via OAuth2");
                        User user = userRepository.save(
                                User.builder()
                                        .username(email)
                                        .provider(provider)
                                        .providerId(providerId)
                                        .externalId(providerId + providerId)
                                        .build());

                        return userMapper.entityToDto(user);
                    });

            UserPrincipal userPrincipal = userMapper.dtoToPrincipal(userDto);

            return CustomOAuth2User.builder()
                    .userPrincipal(userPrincipal)
                    .oAuth2User(oAuth2User)
                    .build();
        };
    }

    @Bean
    public AuthenticationSuccessHandler oAuth2SuccessHandler(){
        return (request, response, authentication) -> {
            log.info("Hello from Success Handler");
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            try {
                if(authentication.getPrincipal() instanceof CustomOAuth2User auth2User) {

                    String accessToken = tokenService.generateAccessToken(
                            auth2User.getId(),
                            auth2User.getAuthoritiesToString());

                    String refreshToken = tokenService.generateRefreshToken(auth2User.getId());

                    response.setHeader("access_token", accessToken);
                    response.setHeader("refresh_token", refreshToken);
                    response.setHeader("token_type", "bearer");

                    return;
                }
                throw new IllegalArgumentException("""
                        OAuth2 login failed user ID was not present OR
                        there was a type mismatch. Extracted type: %s
                        """.formatted(OAuth2User.class.getName()));
            } catch (Exception e) {
                log.warn("OAuth2 login attempt failed: {}", e.getMessage());
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
            }

        };

    }


}



