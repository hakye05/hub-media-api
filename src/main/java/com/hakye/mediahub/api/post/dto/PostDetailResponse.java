package com.hakye.mediahub.api.post.dto;

import com.hakye.mediahub.api.user.dto.ProfileSummaryResponse;

import java.time.Instant;
import java.util.UUID;

public record PostDetailResponse(
        UUID id,
        String title,
        String description,
        String imageUrl,
        Instant createdAt,
        Instant updatedAt,
        ProfileSummaryResponse author
) {
}
