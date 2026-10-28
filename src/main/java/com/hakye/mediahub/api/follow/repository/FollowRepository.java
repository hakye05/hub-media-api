package com.hakye.mediahub.api.follow.repository;

import com.hakye.mediahub.api.follow.domain.Follow;
import com.hakye.mediahub.api.follow.domain.FollowId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface FollowRepository extends JpaRepository<Follow, FollowId> {

    /** Idempotent: following someone you already follow is a no-op. */
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

    @Query("select count(f) from Follow f where f.id.followeeId = :userId")
    long countFollowers(@Param("userId") UUID userId);

    @Query("select count(f) from Follow f where f.id.followerId = :userId")
    long countFollowing(@Param("userId") UUID userId);

    /** Follow rows pointing at :userId (their followers), newest first, strictly after the cursor. */
    @Query(value = """
            select * from follows
            where followee_id = :userId
              and (created_at, follower_id) < (cast(:at as timestamptz), cast(:id as uuid))
            order by created_at desc, follower_id desc
            limit :limit
            """, nativeQuery = true)
    List<Follow> findFollowerEdges(@Param("userId") UUID userId, @Param("at") Instant at,
                                   @Param("id") UUID id, @Param("limit") int limit);

    /** Follow rows made by :userId (who they follow), newest first, strictly after the cursor. */
    @Query(value = """
            select * from follows
            where follower_id = :userId
              and (created_at, followee_id) < (cast(:at as timestamptz), cast(:id as uuid))
            order by created_at desc, followee_id desc
            limit :limit
            """, nativeQuery = true)
    List<Follow> findFollowingEdges(@Param("userId") UUID userId, @Param("at") Instant at,
                                    @Param("id") UUID id, @Param("limit") int limit);

    /** Which of :ids does :viewerId follow? One query for a whole page. */
    @Query("select f.id.followeeId from Follow f where f.id.followerId = :viewerId and f.id.followeeId in :ids")
    List<UUID> findFollowedAmong(@Param("viewerId") UUID viewerId, @Param("ids") Collection<UUID> ids);
}
