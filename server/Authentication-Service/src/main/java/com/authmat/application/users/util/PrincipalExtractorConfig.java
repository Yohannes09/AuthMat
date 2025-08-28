package com.authmat.application.users.util;

import com.authmat.application.users.model.UserPrincipal;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Configuration
public class PrincipalExtractorConfig {
    @Bean
    public PrincipalExtractor extractor(){
        return ()-> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if(authentication.getPrincipal() instanceof UserPrincipal principal) return principal;
            return UserPrincipal.builder().build();
        };
    }
}
