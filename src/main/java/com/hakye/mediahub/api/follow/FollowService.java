package com.hakye.mediahub.api.follow;

import java.util.UUID;

import com.hakye.mediahub.api.common.dto.PageResponse;
import com.hakye.mediahub.api.common.exceptions.BadRequestException;
import com.hakye.mediahub.api.common.utils.PageRequests;
import com.hakye.mediahub.api.user.ProfileSummaryMapper;
import com.hakye.mediahub.api.user.User;
import com.hakye.mediahub.api.user.UserService;
import com.hakye.mediahub.api.user.dto.ProfileSummaryResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FollowService {

    private final FollowRepository followRepository;
    private final UserService userService;
    private final ProfileSummaryMapper summaryMapper;

    public FollowService(FollowRepository followRepository, UserService userService, ProfileSummaryMapper summaryMapper) {
        this.followRepository = followRepository;
        this.userService = userService;
        this.summaryMapper = summaryMapper;
    }

    @Transactional
    public void follow(UUID followerId, String targetUsername) {
        User target = userService.getByUsernameOrThrow(targetUsername);
        if (target.getId().equals(followerId)) {
            throw new BadRequestException("You can't follow yourself");
        }
        followRepository.insertIfAbsent(followerId, target.getId());
    }

    @Transactional
    public void unfollow(UUID followerId, String targetUsername) {
        User target = userService.getByUsernameOrThrow(targetUsername);
        followRepository.deleteFollow(followerId, target.getId());
    }

    @Transactional(readOnly = true)
    public PageResponse<ProfileSummaryResponse> followers(String username, UUID viewerId, int page, int size) {
        User target = userService.getByUsernameOrThrow(username);
        return summaryMapper.toPage(
                followRepository.findFollowers(target.getId(), PageRequests.of(page, size)),
                viewerId);
    }

    @Transactional(readOnly = true)
    public PageResponse<ProfileSummaryResponse> following(String username, UUID viewerId, int page, int size) {
        User target = userService.getByUsernameOrThrow(username);
        return summaryMapper.toPage(
                followRepository.findFollowing(target.getId(), PageRequests.of(page, size)),
                viewerId);
    }
}
