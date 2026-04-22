package com.authmat.application.authentication.loginAttemptManager;

import org.springframework.data.redis.core.script.RedisScript;

/**
 * <p>Redis Lua script guarantees that the increment
 * and conditional expiration are executed as a single, indivisible operation,
 * preventing race conditions under concurrent access.</p>
 *
 * <p>Arguments:</p>
 * <ul>
 *   <li>{@code KEYS[1]} – Redis key used to store the login attempt count</li>
 *   <li>{@code ARGV[1]} – Expiration time in seconds for the counter</li>
 * </ul>
 *
 * <p>Returns:</p>
 * <ul>
 *   <li>The current login attempt count after incrementing</li>
 * </ul>
 */
public final class LoginAttemptManagerScripts {
    private LoginAttemptManagerScripts(){}


    public static final RedisScript<Long> INCREMENT_EXPIRE = RedisScript.of(
            """
            local current = redis.call('INCR', KEYS[1])
            if current == 1 then
                redis.call('EXPIRE', KEYS[1], ARGV[1])
            end
            return current
            """,
            Long.class
    );
}
