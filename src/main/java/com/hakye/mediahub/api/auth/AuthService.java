package com.hakye.mediahub.api.auth;

import com.hakye.mediahub.api.auth.dto.*;
import com.hakye.mediahub.api.common.exceptions.BadRequestException;
import com.hakye.mediahub.api.common.exceptions.ConflictException;
import com.hakye.mediahub.api.common.exceptions.UnauthorizedException;
import com.hakye.mediahub.api.user.domain.User;
import com.hakye.mediahub.api.user.dto.ChangePasswordRequest;
import com.hakye.mediahub.api.user.repository.UserRepository;
import com.hakye.mediahub.api.user.UserService;
import com.hakye.mediahub.api.user.domain.UserValidator;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AuthService(UserRepository userRepository, UserService userService, PasswordEncoder passwordEncoder,
                       JwtService jwtService, RefreshTokenService refreshTokenService) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    public AuthResult register(RegisterRequest req) {
        String email = UserValidator.normalizeString(req.email());
        String username = UserValidator.normalizeString(req.username());

        if (!userService.isEmailAvailable(email)) {
            throw new ConflictException("Email is already registered");
        }
        if (!userService.isUsernameAvailable(username)) {
            throw new ConflictException("Username is not available");
        }

        User user = new User(
                email,
                passwordEncoder.encode(req.password()),
                req.displayName().trim(),
                username,
                req.age(),
                req.gender());
        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            // Two people grabbed the same email/username at the same moment.
            throw new ConflictException("Email or username is already taken");
        }
        return startSession(user);
    }

    public AuthResult login(LoginRequest req) {
        User user = userRepository.findByEmail(UserValidator.normalizeString(req.email()))
                .filter(u -> passwordEncoder.matches(req.password(), u.getPasswordHash()))
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));
        return startSession(user);
    }

    public void logout(String rawRefreshToken) {
        if (rawRefreshToken != null) {
            refreshTokenService.revoke(rawRefreshToken);
        }
    }

    /**
     * Verifies the current password and sets the new one. Optionally ends every existing
     * session and starts a fresh one for the caller. One transaction: all or nothing.
     */
    @Transactional
    public AuthResult changePassword(UUID userId, ChangePasswordRequest req) {
        User user = userService.getCurrent(userId);

        if (!passwordEncoder.matches(req.currentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Current password is incorrect");
        }
        if (req.currentPassword().equals(req.newPassword())) {
            throw new BadRequestException("New password must be different from the current one");
        }

        user.setPasswordHash(passwordEncoder.encode(req.newPassword()));

        if (req.shouldLogoutOtherDevices()) {
            refreshTokenService.revokeAllForUser(userId);
            return startSession(user);
        }
        return new AuthResult(toResponse(user), null);   // new access token only, cookie untouched
    }

    public AuthResult refresh(String rawRefreshToken) {
        if (rawRefreshToken == null) {
            throw new UnauthorizedException("Missing refresh token");
        }
        RefreshTokenService.Rotated rotated = refreshTokenService.rotate(rawRefreshToken);
        return new AuthResult(toResponse(rotated.user()), rotated.token());
    }

    private AuthResult startSession(User user) {
        return new AuthResult(toResponse(user), refreshTokenService.issue(user));
    }

    private AuthResponse toResponse(User user) {
        JwtService.IssuedToken token = jwtService.issue(user);
        return new AuthResponse(token.value(), "Bearer", token.expiresInSeconds(), AuthUserResponse.from(user));
    }
}
