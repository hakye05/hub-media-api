package com.hakye.mediahub.api.media.dto;


import java.util.Map;

public record PresignedUploadResponse(
        String key,
        String uploadUrl,
        String method,
        Map<String, String> headers,
        long expiresInSeconds,
        long maxBytes
) {
}
