package com.hakye.mediahub.api.upload.dto;

import java.util.UUID;

public record UploadResponse(
        UUID uploadId,
        String key,
        String url
) {
}
