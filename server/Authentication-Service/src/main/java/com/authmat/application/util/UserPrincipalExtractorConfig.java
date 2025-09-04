package com.authmat.application.util;

import com.authmat.application.authentication.models.UserPrincipal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

@Configuration
@Slf4j
public class UserPrincipalExtractorConfig {

    @Bean
    public UserPrincipalExtractor userPrincipalExtractor(){
        return () -> {
            Authentication authentication = SecurityContextHolder
                    .getContext()
                    .getAuthentication();

            if(authentication == null || !authentication.isAuthenticated()){
                log.warn("");
                return Optional.empty();
            }

            if(authentication.getPrincipal() instanceof UserPrincipal userPrincipal){
                return Optional.of(userPrincipal);
            }

            throw new IllegalStateException("""
                    UserPrincipal type mismatch
                    Type returned: %s
                    """.formatted(authentication.getPrincipal().getClass().getName()));
        };
    }

}
