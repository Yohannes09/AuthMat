package com.authmat.application.authentication.loginattemptmanager;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
@Slf4j
public class LoginAttemptManager {
    private static final String LOGIN_ATTEMPTS_KEY_PREFIX = "login:attempts:";

    private final LoginAttemptProperties properties;
    private final RedisTemplate<String, String> redisTemplate;

    public LoginAttemptManager(
            LoginAttemptProperties properties,
            @Qualifier("strRedisTemplate") RedisTemplate<String, String> redisTemplate) {
        this.properties = properties;
        this.redisTemplate = redisTemplate;
    }


    public void loginSucceeded(String usernameOrEmail){
        try {
            redisTemplate.delete(buildKey(usernameOrEmail));
            log.debug("Cleared ");
        } catch (RedisConnectionFailureException e) {
            log.error("Redis unavailable during loginSucceeded() for identifier: [{}]", usernameOrEmail, e);
        }
    }


    public void loginFailed(String usernameOrEmail){
        String key = buildKey(usernameOrEmail);

        try {
            String timeoutSeconds = String.valueOf(
                    properties.getFailedLoginLockoutMins().toSeconds());

            Long loginAttempts = redisTemplate.execute(
                    LoginAttemptManagerScripts.INCREMENT_EXPIRE,
                    Collections.singletonList(key),
                    timeoutSeconds);

            if(loginAttempts >= properties.getMaxFailedLoginAttempts()) {
                log.warn("User [{}] login failed, max login attempts reached.", usernameOrEmail);
            }
        } catch (RedisConnectionFailureException e) {
            log.error("Redis unavailable during loginFailed() for identifier: [{}]", usernameOrEmail, e);
        }
    }

    /**
     * <p><b>Fail-closed method</b>: any failure to read or parse
     * the attempt counter will block the login rather than permit it.
     * </p>
     *
     * @param usernameOrEmail
     * @return {@code true} if the identifier is locked out or the attempt state cannot
     *         be safely determined; {@code false} if the identifier is within the
     *         allowed attempt threshold
     */
    public boolean isBlocked(String usernameOrEmail){
        String value = redisTemplate.opsForValue().get(buildKey(usernameOrEmail));
        if(value == null) return false;

        try {
            return Integer.parseInt(value) >= properties.getMaxFailedLoginAttempts();
        } catch (NumberFormatException e) {
            log.error("Failed to parse login attempt counter.");
            redisTemplate.delete(buildKey(usernameOrEmail));
        }catch (RedisConnectionFailureException e){
            log.error("Redis unavailable during isBlocked().");
        }
        return true;
    }

    private String buildKey(String usernameOrEmail){
        return LOGIN_ATTEMPTS_KEY_PREFIX + usernameOrEmail;
    }

}
