package com.hakye.mediahub.api.auth;

import com.hakye.mediahub.api.auth.domain.RefreshToken;
import com.hakye.mediahub.api.auth.repository.RefreshTokenRepository;
import com.hakye.mediahub.api.common.exceptions.UnauthorizedException;
import com.hakye.mediahub.api.config.JwtProperties;
import com.hakye.mediahub.api.user.UserService;
import com.hakye.mediahub.api.user.domain.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class RefreshTokenService {

    public record Issued(String value, Duration ttl) { }

    public record Rotated(User user, Issued token) { }

    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * A just-rotated token presented again within this window is treated as a
     * race between tabs (plain 401), not as theft (family revoked).
     */
    private static final Duration REUSE_GRACE = Duration.ofSeconds(10);

    private final RefreshTokenRepository repository;
    private final UserService userService;
    private final JwtProperties props;

    public RefreshTokenService(RefreshTokenRepository repository, UserService userService, JwtProperties props) {
        this.repository = repository;
        this.userService = userService;
        this.props = props;
    }

    /** Starts a new session (new token family). */
    @Transactional
    public Issued issue(User user) {
        return create(user.getId(), UUID.randomUUID());
    }

    /**
     * Validates the presented token, revokes it and issues its replacement.
     */
    @Transactional(noRollbackFor = UnauthorizedException.class)
    public Rotated rotate(String rawToken) {
        RefreshToken current = repository.findByTokenHashForUpdate(hash(rawToken))
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));
        Instant now = Instant.now();

        if (current.getRevokedAt() != null) {
            if (current.getRevokedAt().plus(REUSE_GRACE).isBefore(now)) {
                repository.revokeFamily(current.getFamilyId(), now);
            }
            throw new UnauthorizedException("Invalid refresh token");
        }
        if (current.getExpiresAt().isBefore(now)) {
            throw new UnauthorizedException("Refresh token expired");
        }

        User user = userService.getCurrent(current.getUserId());
        current.setRevokedAt(now);
        return new Rotated(user, create(user.getId(), current.getFamilyId()));
    }

    /** Logout: ends the whole session the token belongs to. */
    @Transactional
    public void revoke(String rawToken) {
        repository.findByTokenHash(hash(rawToken))
                .ifPresent(t -> repository.revokeFamily(t.getFamilyId(), Instant.now()));
    }

    @Transactional
    public int purgeExpired() {
        return repository.deleteExpiredBefore(Instant.now().minus(Duration.ofDays(1)));
    }

    private Issued create(UUID userId, UUID familyId) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        Duration ttl = Duration.ofDays(props.refreshTtlDays());
        repository.save(new RefreshToken(userId, familyId, hash(raw), Instant.now().plus(ttl)));
        return new Issued(raw, ttl);
    }

    private static String hash(String raw) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
