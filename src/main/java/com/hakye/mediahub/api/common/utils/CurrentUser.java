package com.hakye.mediahub.api.common.utils;

import com.hakye.mediahub.api.common.exceptions.UnauthorizedException;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.UUID;

public final class CurrentUser {

    private CurrentUser() {
    }

    /** For protected endpoints: the token must be present and valid. */
    public static UUID requireId(Jwt jwt) {
        if (jwt == null) {
            throw new UnauthorizedException("Authentication required");
        }
        return parse(jwt);
    }

    /** For public endpoints: null means the caller is a guest. */
    public static UUID optionalId(Jwt jwt) {
        return jwt == null ? null : parse(jwt);
    }

    private static UUID parse(Jwt jwt) {
        try {
            return UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new UnauthorizedException("Invalid token");
        }
    }
}
