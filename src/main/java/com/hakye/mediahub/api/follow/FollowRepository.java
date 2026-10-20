package com.hakye.mediahub.api.follow;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface FollowRepository extends JpaRepository<Follow, FollowId> {

    @Modifying
    @Query(value = """
            insert into follows (follower_id, followee_id, created_at)
            values (:follower, :followee, now())
            on conflict do nothing
            """, nativeQuery = true)
    int insertIfAbsent(@Param("follower") UUID follower, @Param("followee") UUID followee);

    @Modifying
    @Query("delete from Follow f where f.id.followerId = :follower and f.id.followeeId = :followee")
    int deleteFollow(@Param("follower") UUID follower, @Param("followee") UUID followee);
}
