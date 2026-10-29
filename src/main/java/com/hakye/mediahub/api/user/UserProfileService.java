package com.hakye.mediahub.api.user;

import com.hakye.mediahub.api.common.dto.CursorPage;
import com.hakye.mediahub.api.common.exceptions.BadRequestException;
import com.hakye.mediahub.api.common.exceptions.ConflictException;
import com.hakye.mediahub.api.follow.FollowService;
import com.hakye.mediahub.api.storage.ObjectStorageService;
import com.hakye.mediahub.api.upload.UploadService;
import com.hakye.mediahub.api.upload.domain.Upload;
import com.hakye.mediahub.api.upload.domain.UploadKind;
import com.hakye.mediahub.api.user.domain.User;
import com.hakye.mediahub.api.user.domain.UserValidator;
import com.hakye.mediahub.api.user.dto.*;
import com.hakye.mediahub.api.user.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Everything behind /api/users: the caller's own account, public profiles, and the social graph around them. */
@Service
public class UserProfileService {

    private final UserRepository userRepository;
    private final UserService userService;
    private final FollowService followService;
    private final ProfileSummaryMapper summaryMapper;
    private final PasswordEncoder passwordEncoder;
    private final ObjectStorageService storage;
    private final UploadService uploadService;

    public UserProfileService(UserRepository userRepository, UserService userService,
                              FollowService followService, ProfileSummaryMapper summaryMapper,
                              PasswordEncoder passwordEncoder, ObjectStorageService storage, UploadService uploadService) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.followService = followService;
        this.summaryMapper = summaryMapper;
        this.passwordEncoder = passwordEncoder;
        this.storage = storage;
        this.uploadService = uploadService;
    }

    // ---- Personal profile

    @Transactional(readOnly = true)
    public UserResponse getMe(UUID userId) {
        return toResponse(userService.getCurrent(userId));
    }

    @Transactional
    public UserResponse updateProfile(UUID userId, UpdateProfileRequest req) {
        User user = userService.getCurrent(userId);

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
        return toResponse(user);
    }

    @Transactional
    public UserResponse changeUsername(UUID userId, String requested) {
        User user = userService.getCurrent(userId);
        String username = UserValidator.normalizeString(requested);

        if (username.equals(user.getUsername())) {
            return toResponse(user);
        }
        if (!userService.isUsernameAvailable(username)) {
            throw new ConflictException("Username is not available");
        }
        user.setUsername(username);
        flushOrConflict("Username is not available");
        return toResponse(user);
    }

    @Transactional
    public UserResponse changeEmail(UUID userId, ChangeEmailRequest req) {
        User user = userService.getCurrent(userId);

        if (!passwordEncoder.matches(req.currentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Current password is incorrect");
        }
        String email = UserValidator.normalizeString(req.newEmail());
        if (email.equals(user.getEmail())) {
            return toResponse(user);
        }
        if (!userService.isEmailAvailable(email)) {
            throw new ConflictException("Email is already registered");
        }
        user.setEmail(email);
        flushOrConflict("Email is already registered");
        return toResponse(user);
    }

    public UserResponse changeAvatar(UUID userId, UUID uploadId) {
        Upload upload = uploadService.getFinalizedUpload(userId, uploadId, UploadKind.AVATAR);
        String newKey = upload.getMediaKey();

        String oldKey = userService.replaceAvatarKey(userId, newKey);
        if (oldKey != null && !oldKey.equals(newKey)) {
            storage.deleteQuietly(oldKey);
        }
        return toResponse(userService.getCurrent(userId));
    }

    public UserResponse removeAvatar(UUID userId) {
        String oldKey = userService.replaceAvatarKey(userId, null);
        if (oldKey != null) {
            storage.deleteQuietly(oldKey);
        }
        return toResponse(userService.getCurrent(userId));
    }

    // ---- Public profile

    @Transactional(readOnly = true)
    public PublicProfileResponse getPublicProfile(String username, UUID viewerId) {
        User user = userService.getByUsername(username);
        ProfileSummaryResponse summary = summaryMapper.toSummary(user, viewerId);

        return new PublicProfileResponse(
                user.getId(), user.getUsername(), user.getDisplayName(), user.getBio(),
                summary.avatarUrl(), user.getCreatedAt(),
                followService.countFollowers(user.getId()),
                followService.countFollowing(user.getId()),
                summary.isSelf(), summary.isFollowing());
    }

    @Transactional
    public void follow(UUID viewerId, String username) {
        followService.follow(viewerId, userService.getByUsername(username).getId());
    }

    @Transactional
    public void unfollow(UUID viewerId, String username) {
        followService.unfollow(viewerId, userService.getByUsername(username).getId());
    }

    @Transactional(readOnly = true)
    public CursorPage<ProfileSummaryResponse> followers(String username, UUID viewerId, String cursor, int size) {
        User target = userService.getByUsername(username);
        return followService.followerIds(target.getId(), cursor, size)
                .mapItems(ids -> summaries(ids, viewerId));
    }

    @Transactional(readOnly = true)
    public CursorPage<ProfileSummaryResponse> following(String username, UUID viewerId, String cursor, int size) {
        User target = userService.getByUsername(username);
        return followService.followingIds(target.getId(), cursor, size)
                .mapItems(ids -> summaries(ids, viewerId));
    }

    private List<ProfileSummaryResponse> summaries(List<UUID> ids, UUID viewerId) {
        return summaryMapper.toSummaries(userService.getAllInOrder(ids), viewerId);
    }

    // ---- Helpers

    /** Flush now so a unique-index race surfaces as a 409 instead of a 500. */
    private void flushOrConflict(String message) {
        try {
            userRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException(message);
        }
    }

    private UserResponse toResponse(User user) {
        return UserResponse.from(user, storage.publicUrl(user.getAvatarKey()));
    }
}