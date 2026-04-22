package com.authmat.application;

import com.authmat.application.authentication.config.RegistrationProperties;
import com.authmat.application.authentication.loginAttemptManager.LoginAttemptProperties;
import com.authmat.application.security.properties.ServiceProperties;
import com.authmat.application.token.TokenProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

//
@SpringBootApplication
@EnableConfigurationProperties({
        TokenProperties.class,
        ServiceProperties.class,
        LoginAttemptProperties.class,
        RegistrationProperties.class,
})
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
