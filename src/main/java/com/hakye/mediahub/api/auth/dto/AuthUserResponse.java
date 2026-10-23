package com.hakye.mediahub.api.auth.dto;

import com.hakye.mediahub.api.user.User;

import java.util.UUID;

public record AuthUserResponse(
        UUID id,
        String email,
        String username,
        String displayName
) {
    public static AuthUserResponse from(User user) {
        return new AuthUserResponse(user.getId(), user.getEmail(),
                user.getUsername(), user.getDisplayName());
    }
}
