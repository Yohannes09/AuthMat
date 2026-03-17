package com.authmat.application.authentication.config;

import com.authmat.application.user.util.UserMapper;
import com.authmat.application.user.dto.UserDto;
import com.authmat.application.user.repository.UserCache;
import com.authmat.tool.exception.UserNotFoundException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AuthenticationConfig {
    private final UserCache userCache;
    private final UserMapper userMapper;

    public AuthenticationConfig(UserCache userCache, UserMapper userMapper) {
        this.userCache = userCache;
        this.userMapper = userMapper;
    }

    // TODO: Only finding by username not email
    @Bean
    public UserDetailsService userDetailsService(){
        return usernameOrEmail -> {
            UserDto dto = userCache
                    .findByUsername(usernameOrEmail)
                    .or(() -> userCache.findByEmail(usernameOrEmail))
                    .orElseThrow(() -> new UserNotFoundException("User not found: " + usernameOrEmail));

            return userMapper.dtoToUserDetails(dto);
        };
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
}
