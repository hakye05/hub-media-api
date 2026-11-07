package com.hakye.mediahub.api.auth.ratelimit;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@EnableConfigurationProperties(RateLimitProperties.class)
public class LoginRateLimiter {

    private final RateLimitProperties props;
    private final RedisAttemptCounter accounts;
    private final RedisAttemptCounter ips;

    public LoginRateLimiter(RateLimitProperties props, StringRedisTemplate redis) {
        this.props = props;
        this.accounts = new RedisAttemptCounter(redis, "rl:login:acct:", props.window());
        this.ips = new RedisAttemptCounter(redis, "rl:login:ip:", props.window());
    }

    /** Throws TooManyRequestsException if either the account or the IP is over its limit. */
    public void checkAllowed(String account, String ip) {
        long wait = Math.max(
                accounts.retryAfterSeconds(account, props.maxAttemptsPerAccount()),
                ips.retryAfterSeconds(ip, props.maxAttemptsPerIp()));
        if (wait > 0) {
            throw new TooManyRequestsException(wait);
        }
    }

    public void recordFailure(String account, String ip) {
        accounts.recordFailure(account);
        ips.recordFailure(ip);
    }

    public void recordSuccess(String account) {
        accounts.reset(account);
    }
}