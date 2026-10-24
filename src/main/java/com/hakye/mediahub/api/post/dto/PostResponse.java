package com.hakye.mediahub.api.post.dto;

import java.time.Instant;
import java.util.UUID;

public record PostResponse(
        UUID id,
        UUID userId,
        String title,
        String description,
        String imageUrl,
        Integer width,
        Integer height,
        Instant createdAt,
        Instant updatedAt
) {
}
