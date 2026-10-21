package com.hakye.mediahub.api.user;

import com.hakye.mediahub.api.common.exceptions.BadRequestException;
import com.hakye.mediahub.api.common.exceptions.ConflictException;
import com.hakye.mediahub.api.follow.FollowId;
import com.hakye.mediahub.api.follow.FollowRepository;
import com.hakye.mediahub.api.user.dto.ChangeEmailRequest;
import com.hakye.mediahub.api.user.dto.PublicProfileResponse;
import com.hakye.mediahub.api.user.dto.UpdateProfileRequest;
import com.hakye.mediahub.api.user.dto.UserResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@Service
public class UserProfileService {

    private final UserRepository userRepository;
    private final UserService userService;
    private final FollowRepository followRepository;
    private final PasswordEncoder passwordEncoder;
    private final List<String> allowedAvatarHosts;

    public UserProfileService(UserRepository userRepository,
                              UserService userService,
                              FollowRepository followRepository,
                              PasswordEncoder passwordEncoder,
                              @Value("${app.media.allowed-hosts:}") List<String> allowedAvatarHosts) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.followRepository = followRepository;
        this.passwordEncoder = passwordEncoder;
        this.allowedAvatarHosts = allowedAvatarHosts;
    }

    @Transactional(readOnly = true)
    public PublicProfileResponse getPublicProfile(String username, UUID viewerId) {
        User user = userService.getByUsernameOrThrow(username);
        boolean isSelf = viewerId != null && viewerId.equals(user.getId());
        boolean isFollowing = viewerId != null && !isSelf
                && followRepository.existsById(new FollowId(viewerId, user.getId()));

        return new PublicProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getBio(),
                user.getAvatarUrl(),
                user.getCreatedAt(),
                followRepository.countFollowers(user.getId()),
                followRepository.countFollowing(user.getId()),
                isSelf,
                isFollowing);
    }

    @Transactional
    public UserResponse updateProfile(UUID userId, UpdateProfileRequest req) {
        User user = userService.getById(userId);

        if (req.displayName() != null) {
            String name = req.displayName().trim();
            if (name.isEmpty()) {
                throw new BadRequestException("Name cannot be blank");
            }
            user.setDisplayName(name);
        }
        if (req.gender() != null) {
            user.setGender(req.gender());
        }
        if (req.bio() != null) {
            String bio = req.bio().trim();
            user.setBio(bio.isEmpty() ? null : bio);
        }
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse changeUsername(UUID userId, String requested) {
        User user = userService.getById(userId);
        String normalized = UserValidator.normalizeString(requested);

        if (normalized.equals(user.getUsername())) {
            return UserResponse.from(user);
        }
        if (!userService.isUsernameAvailable(normalized)) {
            throw new ConflictException("Username is not available");
        }
        user.setUsername(normalized);
        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Username is not available");
        }
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse changeEmail(UUID userId, ChangeEmailRequest req) {
        User user = userService.getById(userId);

        if (!passwordEncoder.matches(req.currentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Current password is incorrect");
        }
        String email = UserValidator.normalizeString(req.newEmail());
        if (email.equals(user.getEmail())) {
            return UserResponse.from(user);
        }
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("Email is already registered");
        }
        user.setEmail(email);
        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Email is already registered");
        }
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse setAvatar(UUID userId, String avatarUrl) {
        String url = avatarUrl.trim();
        validateAvatarUrl(url);
        User user = userService.getById(userId);
        user.setAvatarUrl(url);
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse removeAvatar(UUID userId) {
        User user = userService.getById(userId);
        user.setAvatarUrl(null);
        return UserResponse.from(user);
    }

    private void validateAvatarUrl(String url) {
        URI uri;
        try {
            uri = URI.create(url);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Avatar URL is not valid");
        }
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null || uri.getUserInfo() != null) {
            throw new BadRequestException("Avatar URL must be a valid https URL");
        }
        if (!allowedAvatarHosts.isEmpty()
                && allowedAvatarHosts.stream().noneMatch(h -> h.equalsIgnoreCase(uri.getHost()))) {
            throw new BadRequestException("Avatar must be hosted on an allowed domain");
        }
    }
}