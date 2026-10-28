package com.hakye.mediahub.api.auth;

import com.hakye.mediahub.api.auth.dto.AuthResponse;
import com.hakye.mediahub.api.auth.dto.AuthUserResponse;
import com.hakye.mediahub.api.auth.dto.LoginRequest;
import com.hakye.mediahub.api.auth.dto.RegisterRequest;
import com.hakye.mediahub.api.common.exceptions.ConflictException;
import com.hakye.mediahub.api.common.exceptions.UnauthorizedException;
import com.hakye.mediahub.api.user.domain.User;
import com.hakye.mediahub.api.user.repository.UserRepository;
import com.hakye.mediahub.api.user.UserService;
import com.hakye.mediahub.api.user.domain.UserValidator;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, UserService userService, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponse register(RegisterRequest req) {
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
        return toResponse(user);
    }

    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByEmail(UserValidator.normalizeString(req.email()))
                .filter(u -> passwordEncoder.matches(req.password(), u.getPasswordHash()))
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));
        return toResponse(user);
    }

    private AuthResponse toResponse(User user) {
        JwtService.IssuedToken token = jwtService.issue(user);
        return new AuthResponse(token.value(), "Bearer", token.expiresInSeconds(), AuthUserResponse.from(user));
    }
}
