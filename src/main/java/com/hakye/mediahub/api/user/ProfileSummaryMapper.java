package com.hakye.mediahub.api.user;

import com.hakye.mediahub.api.common.dto.PageResponse;
import com.hakye.mediahub.api.follow.FollowRepository;
import com.hakye.mediahub.api.media.ObjectStorageService;
import com.hakye.mediahub.api.user.dto.ProfileSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
public class ProfileSummaryMapper {

    private final FollowRepository followRepository;
    private final ObjectStorageService storage;

    public ProfileSummaryMapper(FollowRepository followRepository, ObjectStorageService storage) {
        this.followRepository = followRepository;
        this.storage = storage;
    }

    /** viewerId is null for guests. Uses one query to resolve follow state for the whole page. */
    public PageResponse<ProfileSummaryResponse> toPage(Page<User> page, UUID viewerId) {
        List<User> users = page.getContent();

        Set<UUID> followedByViewer = new HashSet<>();
        if (viewerId != null && !users.isEmpty()) {
            List<UUID> ids = users.stream().map(User::getId).toList();
            followedByViewer.addAll(followRepository.findFollowedAmong(viewerId, ids));
        }

        List<ProfileSummaryResponse> items = users.stream()
                .map(u -> ProfileSummaryResponse.of(u, storage.publicUrl(u.getAvatarKey()),
                        u.getId().equals(viewerId), followedByViewer.contains(u.getId())))
                .toList();
        return PageResponse.of(items, page);
    }
}
