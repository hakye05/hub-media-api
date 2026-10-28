package com.hakye.mediahub.api.follow.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.UUID;

@Embeddable
public record FollowId(
        @Column(name = "follower_id") UUID followerId,
        @Column(name = "followee_id") UUID followeeId
) {
}