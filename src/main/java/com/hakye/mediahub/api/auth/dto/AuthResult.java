package com.hakye.mediahub.api.auth.dto;

import com.hakye.mediahub.api.auth.RefreshTokenService;

public record AuthResult(
        AuthResponse body,
        RefreshTokenService.Issued refreshToken
) {
}
