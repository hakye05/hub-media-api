package com.hakye.mediahub.api.auth.dto;

import com.hakye.mediahub.api.user.dto.UserResponse;

public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        UserResponse user
) {
}
