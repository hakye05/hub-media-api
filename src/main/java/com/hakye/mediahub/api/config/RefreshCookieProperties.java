package com.hakye.mediahub.api.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.refresh-cookie")
public record RefreshCookieProperties(
        @NotBlank String name,
        boolean secure,
        @NotBlank String sameSite,
        @NotBlank String path
) {
}