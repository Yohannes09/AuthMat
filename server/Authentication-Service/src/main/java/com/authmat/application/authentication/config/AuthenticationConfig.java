package com.authmat.application.authentication.config;

import com.authmat.application.users.util.UserMapper;
import com.authmat.application.users.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class AuthenticationConfig {
    private final UserRepository userRepository;
    private final UserMapper userMapper;


    @Bean
    public UserDetailsService loadByUsername(){
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

//    @Bean
//    public AuthenticationFilterConfig jwtAuthenticationFilter(){
//        return new AuthenticationFilterConfig(userDetailsService());
//    }

    @Bean
    public AuthenticationProvider authenticationProvider(
            PasswordEncoder passwordEncoder,
            UserDetailsService userDetailsService
    ){
        DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider();
        authenticationProvider.setUserDetailsService(userDetailsService);
        authenticationProvider.setPasswordEncoder(passwordEncoder);
        return authenticationProvider;
    }

}
