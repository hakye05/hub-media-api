package com.hakye.mediahub.api.media;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.storage")
public record StorageProperties(
        String accountId,
        String bucket,
        String accessKeyId,
        String secretAccessKey,
        String publicBaseUrl,
        long presignExpirySeconds
) {
}
