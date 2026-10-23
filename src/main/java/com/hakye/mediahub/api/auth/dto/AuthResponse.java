package com.hakye.mediahub.api.auth.dto;

public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        AuthUserResponse user
) {
}
