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
import org.springframework.core.annotation.Order;
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
import java.util.stream.Collectors;

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
    @Order(1)
    public SecurityFilterChain oAuth2SecurityFilterChain(HttpSecurity http) throws Exception{
        String[] patterns = {"/oauth2/**", "/login/oauth2/**", "/login/**"};
        return http
                .securityMatcher(patterns)
                .authorizeHttpRequests(request -> request
                        .requestMatchers(patterns).permitAll()
                        .anyRequest().authenticated())
                .authenticationProvider(authenticationProvider(
                        passwordEncoder(), userDetailsService()))
                .oauth2Login(loginConfigurer -> loginConfigurer
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(oAuth2UserService()))
                        .successHandler(oAuth2SuccessHandler()))
//                .sessionManagement(sessionConfigurer -> sessionConfigurer
//                        .sessionCreationPolicy(SessionCreationPolicy.ALWAYS))
                .build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .securityMatcher(request ->
                        !request.getRequestURI().startsWith("/oauth2/") &&
                        !request.getRequestURI().startsWith("/login/oauth2/"))
                .cors(cors-> cors.configurationSource(corsConfigurationSource()))
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
                .authenticationProvider(authenticationProvider(
                        passwordEncoder(),
                        userDetailsService()))
                .addFilterBefore(
                        simpleAuthenticationFilter(),
                        UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
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
    public OncePerRequestFilter simpleAuthenticationFilter(){
        return new SimpleJwtAuthenticationFilter(publicKeyResolver(), Set.of("/ping", "/auth/v1/login", "/auth/v1/register"));
    }

    @Bean
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
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            try {
                if(authentication.getPrincipal() instanceof OAuth2User auth2User) {

                    String accessToken = tokenService.generateAccessToken(
                            auth2User.getAttribute("email"),
                            auth2User.getAuthorities()
                                    .stream()
                                    .map(GrantedAuthority::getAuthority)
                                    .collect(Collectors.toSet()));

                    String refreshToken = tokenService.generateRefreshToken(auth2User.getAttribute("email"));

                    response.setHeader("access_token", accessToken);
                    response.setHeader("refresh_token", refreshToken);
                    response.setHeader("token_type", "bearer");
                    log.info("oauth success");
                }

            } catch (Exception e) {
                log.warn("OAuth2 login attempt failed: {}", e.getMessage());
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
            }

        };

    }

}