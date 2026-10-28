package com.hakye.mediahub.api.user;

import com.hakye.mediahub.api.follow.FollowService;
import com.hakye.mediahub.api.storage.ObjectStorageService;
import com.hakye.mediahub.api.user.domain.User;
import com.hakye.mediahub.api.user.dto.ProfileSummaryResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Builds the compact "avatar + name + follow state" view of users, shared by every feature that shows people. */
@Component
public class ProfileSummaryMapper {

    private final FollowService followService;
    private final ObjectStorageService storage;

    public ProfileSummaryMapper(FollowService followService, ObjectStorageService storage) {
        this.followService = followService;
        this.storage = storage;
    }

    /** viewerId is null for guests. */
    public ProfileSummaryResponse toSummary(User user, UUID viewerId) {
        return toSummaries(List.of(user), viewerId).get(0);
    }

    /** Resolves follow state for a whole list with one query. viewerId is null for guests. */
    public List<ProfileSummaryResponse> toSummaries(List<User> users, UUID viewerId) {
        Set<UUID> followedByViewer = followService.followedAmong(viewerId, users.stream().map(User::getId).toList());
        return users.stream()
                .map(u -> ProfileSummaryResponse.of(u, storage.publicUrl(u.getAvatarKey()),
                        u.getId().equals(viewerId), followedByViewer.contains(u.getId())))
                .toList();
    }
}
