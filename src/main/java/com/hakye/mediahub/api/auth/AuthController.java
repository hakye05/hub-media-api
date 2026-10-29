package com.hakye.mediahub.api.auth;

import com.hakye.mediahub.api.auth.dto.*;
import com.hakye.mediahub.api.common.exceptions.UnauthorizedException;
import com.hakye.mediahub.api.user.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;
    private final RefreshCookieFactory cookies;
    private final String allowedOrigin;

    public AuthController(AuthService authService, UserService userService, RefreshCookieFactory cookies,
                          @Value("${app.cors.allowed-origin}") String allowedOrigin) {
        this.authService = authService;
        this.userService = userService;
        this.cookies = cookies;
        this.allowedOrigin = allowedOrigin;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request, HttpServletResponse response) {
        return withCookie(authService.register(request), response);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        return withCookie(authService.login(request), response);
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(HttpServletRequest request, HttpServletResponse response) {
        requireTrustedOrigin(request);
        try {
            return withCookie(authService.refresh(cookies.read(request)), response);
        } catch (UnauthorizedException e) {
            clearCookie(response); // don't keep sending a dead token
            throw e;
        }
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        requireTrustedOrigin(request);
        authService.logout(cookies.read(request));
        clearCookie(response);
    }

    @GetMapping("/check-email")
    public AvailabilityResponse checkEmail(@RequestParam String email) {
        return new AvailabilityResponse(userService.isEmailAvailable(email));
    }

    @GetMapping("/check-username")
    public AvailabilityResponse checkUsername(@RequestParam String username) {
        return new AvailabilityResponse(userService.isUsernameAvailable(username));
    }

    private AuthResponse withCookie(AuthResult result, HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookies.create(result.refreshToken()).toString());
        return result.body();
    }

    private void clearCookie(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookies.clear().toString());
    }

    /**
     * Cookie-authenticated endpoints get a CSRF defense in depth on top of SameSite:
     * browsers always send Origin on cross-origin POSTs, so reject anything but our frontend.
     */
    private void requireTrustedOrigin(HttpServletRequest request) {
        String origin = request.getHeader(HttpHeaders.ORIGIN);
        if (origin != null && !origin.equals(allowedOrigin)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Untrusted origin");
        }
    }
}
