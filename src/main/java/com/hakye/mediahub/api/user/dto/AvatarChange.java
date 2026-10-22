package com.hakye.mediahub.api.user.dto;

public record AvatarChange(
        UserResponse response,
        String previousUrl
) {
}
