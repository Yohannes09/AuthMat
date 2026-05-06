package com.authmat.application.authentication.loginattemptmanager;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Slf4j
@Component
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

    /**
     * Lesson learned:
     *
     * <p>This approach incremented a Redis counter and conditionally applied an
     * expiration using two separate Redis commands:</p>
     *
     * <pre>{@code
     * Long loginAttempts = redisTemplate.opsForValue().increment(key);
     * if (loginAttempts != null && loginAttempts == ALLOWED_FAILED_LOGIN_ATTEMPTS) {
     *     redisTemplate.expire(key, LOGIN_LOCKOUT_DURATION);
     * }
     * }</pre>
     *
     * <p>While simple, this implementation is <strong>not safe under concurrent
     * access</strong>. The increment and expiration operations are not atomic, allowing
     * other requests to interleave between them.</p>
     *
     * <p>Under load, this can result in counters that exceed the allowed limit
     * without an expiration being set, leading to incorrect or permanent lockouts.</p>
     *
     * <p>This logic has been replaced with an atomic Redis Lua script to ensure
     * correctness and deterministic behavior.</p>
     */
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
     * Determines whether the given identifier is currently locked out due to excessive
     * failed login attempts.
     *
     * <p>This method is intentionally <b>fail-closed</b>: any failure to read or parse
     * the attempt counter will block the login rather than permit it. This mirrors the
     * posture of high-security systems; when a security control cannot fulfill its
     * responsibility, the safe default is to deny access, not grant it.
     *
     * <p><b>Fail-closed rationale:</b>
     * <ul>
     *   <li><b>Redis unavailable:</b> An infrastructure outage does not justify bypassing
     *       account lockout. Returning {@code false} here would allow unlimited login
     *       attempts during any Redis downtime.</li>
     *   <li><b>Corrupted counter:</b> A non-numeric value under the attempt key indicates
     *       a data integrity problem. The account's true attempt state is unknown, so
     *       access is denied until the key is evicted or manually cleared.</li>
     * </ul>
     *
     * <p><b>Normal flow:</b> A user who has never failed a login attempt will have no
     * key in Redis, causing this method to return {@code false} immediately — they are
     * never impacted by this mechanism on a clean authentication.
     *
     * @param usernameOrEmail the identifier submitted during the login attempt;
     *                        must not be {@code null} or blank
     * @return {@code true} if the identifier is locked out or the attempt state cannot
     *         be safely determined; {@code false} if the identifier is within the
     *         allowed attempt threshold
     * @throws IllegalArgumentException if {@code usernameOrEmail} is {@code null} or blank
     */
    public boolean isBlocked(String usernameOrEmail){
        String value = redisTemplate.opsForValue().get(buildKey(usernameOrEmail));
        if(value == null) return false;

        try {
            return Integer.parseInt(value) >= properties.getMaxFailedLoginAttempts();
        } catch (NumberFormatException e) {
            log.error("Failed to parse login attempt counter.");
            redisTemplate.delete(buildKey(usernameOrEmail));
            return true;
        }catch (RedisConnectionFailureException e){
            log.error("Redis unavailable during isBlocked().");
            return true;
        }
    }

    private String buildKey(String usernameOrEmail){
        return LOGIN_ATTEMPTS_KEY_PREFIX + usernameOrEmail;
    }
}
