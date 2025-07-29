package com.authmat.application.authentication;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class LoginAttemptManager {
    private static final String KEY_PREFIX = "login:attempts:";
    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final Duration ACCOUNT_DISABLED_DURATION = Duration.ofMinutes(10);

    private final RedisTemplate<String, Object> redisTemplate;


    public void loginSucceeded(String usernameOrEmail){
        redisTemplate.delete(buildKey(usernameOrEmail));
    }

    public void loginFailed(String usernameOrEmail){
        Long attempts = redisTemplate.opsForValue().increment(buildKey(usernameOrEmail));
        if(attempts == 1){
            redisTemplate.expire(usernameOrEmail, ACCOUNT_DISABLED_DURATION);
        }
    }

    public boolean isBlocked(String usernameOrEmail){
        Object value = redisTemplate.opsForValue().get(buildKey(usernameOrEmail));
        return (value instanceof Long attempts) && attempts >= MAX_LOGIN_ATTEMPTS;
    }

    private String buildKey(String key){
        return KEY_PREFIX + key.toLowerCase();
    }
}
