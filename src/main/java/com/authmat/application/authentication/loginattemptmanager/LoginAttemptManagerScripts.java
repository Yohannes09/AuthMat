package com.authmat.application.authentication.loginattemptmanager;

import org.springframework.data.redis.core.script.RedisScript;

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
