package com.hakye.mediahub.api.auth.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.rate-limit.login")
public record RateLimitProperties(
        Duration window,
        int maxAttemptsPerAccount,
        int maxAttemptsPerIp
) {
}