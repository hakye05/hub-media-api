package com.hakye.mediahub.api.auth;

import com.hakye.mediahub.api.config.RefreshCookieProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.util.WebUtils;

import java.time.Duration;

@Component
public class RefreshCookieFactory {

    private final RefreshCookieProperties props;

    public RefreshCookieFactory(RefreshCookieProperties props) {
        this.props = props;
    }

    public ResponseCookie create(RefreshTokenService.Issued token) {
        return build(token.value(), token.ttl());
    }

    public ResponseCookie clear() {
        return build("", Duration.ZERO);
    }

    public String read(HttpServletRequest request) {
        Cookie cookie = WebUtils.getCookie(request, props.name());
        return cookie == null || cookie.getValue().isBlank() ? null : cookie.getValue();
    }

    private ResponseCookie build(String value, Duration maxAge) {
        return ResponseCookie.from(props.name(), value)
                .httpOnly(true)
                .secure(props.secure())
                .sameSite(props.sameSite())
                .path(props.path())
                .maxAge(maxAge)
                .build();
    }
}