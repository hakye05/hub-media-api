package com.hakye.mediahub.api.upload.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record PresignedUploadResponse(
        UUID uploadId,
        String uploadUrl,
        String method,
        Map<String, String> headers,
        Instant expiresAt
) {
}
