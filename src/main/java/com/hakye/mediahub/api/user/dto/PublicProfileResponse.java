package com.hakye.mediahub.api.user.dto;

import java.time.Instant;
import java.util.UUID;

public record PublicProfileResponse(
        UUID id,
        String username,
        String displayName,
        String bio,
        String avatarUrl,
        Instant createdAt,
        long followerCount,
        long followingCount,
        boolean isSelf,
        boolean isFollowing
) {
}
