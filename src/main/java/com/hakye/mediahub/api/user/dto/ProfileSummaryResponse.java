package com.hakye.mediahub.api.user.dto;

import com.hakye.mediahub.api.user.domain.User;

import java.util.UUID;

public record ProfileSummaryResponse(
        UUID id,
        String username,
        String displayName,
        String avatarUrl,
        boolean isSelf,
        boolean isFollowing
) {
    public static ProfileSummaryResponse of(User user, String avatarUrl, boolean isSelf, boolean isFollowing) {
        return new ProfileSummaryResponse(user.getId(), user.getUsername(),
                user.getDisplayName(), avatarUrl, isSelf, isFollowing);
    }
}
