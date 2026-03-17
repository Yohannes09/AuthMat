package com.authmat.application.config;

import com.authmat.application.authentication.exception.FailedAuthencticationException;
import com.authmat.application.authentication.models.OAuth2UserImpl;
import com.authmat.application.authentication.models.UserDetailsImpl;
import com.authmat.application.token.service.TokenService;
import com.authmat.application.user.util.UserMapper;
import com.authmat.application.user.dto.UserDto;
import com.authmat.application.user.repository.UserCache;
import com.authmat.tool.exception.UserNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.Optional;

@Slf4j
@Configuration
public class OAuth2Config {
    private final AuthenticationProvider authenticationProvider;
    private final CorsConfigurationSource corsConfig;
    private final TokenService tokenService;
    private final UserCache userCache;
    private final UserMapper userMapper;

    public OAuth2Config(
            AuthenticationProvider authenticationProvider,
            CorsConfigurationSource corsConfig,
            TokenService tokenService,
            UserCache userCache,
            UserMapper userMapper
    ) {
        this.authenticationProvider = authenticationProvider;
        this.corsConfig = corsConfig;
        this.tokenService = tokenService;
        this.userCache = userCache;
        this.userMapper = userMapper;
    }

    // TODO: Get this working again

    @Bean
    public SecurityFilterChain oAuth2SecurityFilterChain(HttpSecurity http) throws Exception{
        String[] patterns = {"/oauth2/**", "/login/oauth2/**", "/login/**"};
        return http
                .securityMatcher(patterns)
                .authorizeHttpRequests(request -> request
                        .requestMatchers(patterns).permitAll()
                        .anyRequest().authenticated())
                .authenticationProvider(authenticationProvider)
                .oauth2Login(loginConfigurer -> loginConfigurer
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(oAuth2UserService()))
                        .successHandler(oAuth2SuccessHandler()))
//                .sessionManagement(sessionConfigurer -> sessionConfigurer
//                        .sessionCreationPolicy(SessionCreationPolicy.ALWAYS))
                .build();
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

            UserDto userDto = userCache.findByEmail(email)
                    .orElseThrow(() -> new UserNotFoundException("Could not find user"));


            UserDetailsImpl userDetailsImpl = userMapper.dtoToUserDetails(userDto);

            return OAuth2UserImpl.builder()
                    .userDetailsImpl(userDetailsImpl)
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

//                    CompletableFuture<AccessToken> accessToken = tokenService.generateAccessToken(
//                            auth2User.getAttribute("email"));
//
//                    String refreshToken = tokenService.generateRefreshToken(auth2User.getAttribute("email"));
//
//                    response.setHeader("access_token", accessToken);
//                    response.setHeader("refresh_token", refreshToken);
//                    response.setHeader("token_type", "bearer");
                    log.info("oauth success");
                }

            } catch (Exception e) {
                log.warn("OAuth2 login attempt failed: {}", e.getMessage());
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
            }

        };

    }
}
