package com.hakye.mediahub.api.common.storage;

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
