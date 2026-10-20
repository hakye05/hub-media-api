package com.hakye.mediahub.api.follow;

import java.util.UUID;

import com.hakye.mediahub.api.common.BadRequestException;
import com.hakye.mediahub.api.user.User;
import com.hakye.mediahub.api.user.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FollowService {

    private final FollowRepository followRepository;
    private final UserService userService;

    public FollowService(FollowRepository followRepository, UserService userService) {
        this.followRepository = followRepository;
        this.userService = userService;
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
}
