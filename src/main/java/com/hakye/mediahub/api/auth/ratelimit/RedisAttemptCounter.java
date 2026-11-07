package com.hakye.mediahub.api.auth.ratelimit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Fixed-window failure counter backed by Redis. The window starts at the first failure
 * and Redis expires the key itself, so no cleanup job is needed.
 * Fails open: if Redis is unreachable, logins still work (and a warning is logged).
 */
class RedisAttemptCounter {

    private static final Logger log = LoggerFactory.getLogger(RedisAttemptCounter.class);

    // INCR and PEXPIRE run atomically, so a crash can never leave a key without a TTL.
    private static final DefaultRedisScript<Long> INCR_WITH_TTL = new DefaultRedisScript<>("""
            local c = redis.call('INCR', KEYS[1])
            if c == 1 then redis.call('PEXPIRE', KEYS[1], ARGV[1]) end
            return c
            """, Long.class);

    private final StringRedisTemplate redis;
    private final String prefix;
    private final Duration window;

    RedisAttemptCounter(StringRedisTemplate redis, String prefix, Duration window) {
        this.redis = redis;
        this.prefix = prefix;
        this.window = window;
    }

    /** Seconds until the key may try again, or 0 if it is under the limit. */
    long retryAfterSeconds(String key, int max) {
        try {
            String value = redis.opsForValue().get(prefix + key);
            if (value == null || Long.parseLong(value) < max) {
                return 0;
            }
            Long ttlMs = redis.getExpire(prefix + key, TimeUnit.MILLISECONDS);
            if (ttlMs == null || ttlMs == -2) {
                return 0;                                   // key expired in between
            }
            if (ttlMs < 0) {
                return window.toSeconds();                  // no TTL (should not happen)
            }
            return Math.max(1, (ttlMs + 999) / 1000);
        } catch (DataAccessException e) {
            log.warn("Rate limiter check skipped, Redis unavailable: {}", e.getMessage());
            return 0;
        }
    }

    void recordFailure(String key) {
        try {
            redis.execute(INCR_WITH_TTL, List.of(prefix + key), String.valueOf(window.toMillis()));
        } catch (DataAccessException e) {
            log.warn("Failed login not recorded, Redis unavailable: {}", e.getMessage());
        }
    }

    void reset(String key) {
        try {
            redis.delete(prefix + key);
        } catch (DataAccessException e) {
            log.warn("Rate limiter reset skipped, Redis unavailable: {}", e.getMessage());
        }
    }
}