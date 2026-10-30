package com.hakye.mediahub.api.auth.dto;

import com.hakye.mediahub.api.auth.RefreshTokenService;

/**
 * Body for the client plus the refresh token that goes into the cookie.
 * refreshToken is null when the existing session should be left untouched.
 */
public record AuthResult(
        AuthResponse body,
        RefreshTokenService.Issued refreshToken
) {
}
