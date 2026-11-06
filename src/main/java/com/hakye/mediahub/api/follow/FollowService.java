package com.hakye.mediahub.api.follow;

import java.util.*;

import com.hakye.mediahub.api.common.dto.CursorPage;
import com.hakye.mediahub.api.common.exceptions.BadRequestException;
import com.hakye.mediahub.api.common.utils.PageCursor;
import com.hakye.mediahub.api.common.utils.SearchQuery;
import com.hakye.mediahub.api.follow.domain.Follow;
import com.hakye.mediahub.api.follow.domain.FollowId;
import com.hakye.mediahub.api.follow.repository.FollowRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The follow graph, expressed in user ids only */
@Service
public class FollowService {

    private final FollowRepository followRepository;

    public FollowService(FollowRepository followRepository) {
        this.followRepository = followRepository;
    }

    /** Idempotent. */
    @Transactional
    public void follow(UUID followerId, UUID targetId) {
        if (followerId.equals(targetId)) {
            throw new BadRequestException("You can't follow yourself");
        }
        followRepository.insertIfAbsent(followerId, targetId);
    }

    /** Idempotent. */
    @Transactional
    public void unfollow(UUID followerId, UUID targetId) {
        followRepository.deleteFollow(followerId, targetId);
    }

    @Transactional(readOnly = true)
    public boolean isFollowing(UUID viewerId, UUID targetId) {
        return viewerId != null && followRepository.existsById(new FollowId(viewerId, targetId));
    }

    @Transactional(readOnly = true)
    public long countFollowers(UUID userId) {
        return followRepository.countFollowers(userId);
    }

    @Transactional(readOnly = true)
    public long countFollowing(UUID userId) {
        return followRepository.countFollowing(userId);
    }

    /** Which of {@code userIds} the viewer follows. Guests and empty input skip the query. */
    @Transactional(readOnly = true)
    public Set<UUID> followedAmong(UUID viewerId, Collection<UUID> userIds) {
        if (viewerId == null || userIds.isEmpty()) return Set.of();
        return new HashSet<>(followRepository.findFollowedAmong(viewerId, userIds));
    }

    /** Ids of the users who follow {@code userId}, newest follow first. A blank query means no filter. */
    @Transactional(readOnly = true)
    public CursorPage<UUID> followerIds(UUID userId, String query, String cursor, int size) {
        int limit = PageCursor.clampSize(size);
        PageCursor after = PageCursor.decode(cursor);
        List<Follow> edges = (query == null || query.isBlank())
                ? followRepository.findFollowerEdges(userId, after.createdAt(), after.id(), limit + 1)
                : followRepository.searchFollowerEdges(userId, pattern(query),
                after.createdAt(), after.id(), PageRequest.ofSize(limit + 1));
        return CursorPage.of(edges, limit,
                f -> new PageCursor(f.getCreatedAt(), f.getId().followerId()),
                page -> page.stream().map(f -> f.getId().followerId()).toList());
    }

    /** Ids of the users {@code userId} follows, newest follow first. A blank query means no filter. */
    @Transactional(readOnly = true)
    public CursorPage<UUID> followingIds(UUID userId, String query, String cursor, int size) {
        int limit = PageCursor.clampSize(size);
        PageCursor after = PageCursor.decode(cursor);
        List<Follow> edges = (query == null || query.isBlank())
                ? followRepository.findFollowingEdges(userId, after.createdAt(), after.id(), limit + 1)
                : followRepository.searchFollowingEdges(userId, pattern(query),
                after.createdAt(), after.id(), PageRequest.ofSize(limit + 1));
        return CursorPage.of(edges, limit,
                f -> new PageCursor(f.getCreatedAt(), f.getId().followeeId()),
                page -> page.stream().map(f -> f.getId().followeeId()).toList());
    }

    private static String pattern(String query) {
        return SearchQuery.containsPattern(SearchQuery.require(query));
    }
}
