package com.hakye.mediahub.api.follow;

import com.hakye.mediahub.api.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
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

    @Query("select count(f) from Follow f where f.id.followeeId = :userId")
    long countFollowers(@Param("userId") UUID userId);

    @Query("select count(f) from Follow f where f.id.followerId = :userId")
    long countFollowing(@Param("userId") UUID userId);

    /** Users who follow :userId, newest follow first. */
    @Query(value = """
            select u from User u, Follow f
            where f.id.followerId = u.id and f.id.followeeId = :userId
            order by f.createdAt desc, u.id
            """,
            countQuery = "select count(f) from Follow f where f.id.followeeId = :userId")
    Page<User> findFollowers(@Param("userId") UUID userId, Pageable pageable);

    /** Users that :userId follows, newest follow first. */
    @Query(value = """
            select u from User u, Follow f
            where f.id.followeeId = u.id and f.id.followerId = :userId
            order by f.createdAt desc, u.id
            """,
            countQuery = "select count(f) from Follow f where f.id.followerId = :userId")
    Page<User> findFollowing(@Param("userId") UUID userId, Pageable pageable);

    /** Which of :ids does :viewerId follow? One query for a whole page. */
    @Query("select f.id.followeeId from Follow f where f.id.followerId = :viewerId and f.id.followeeId in :ids")
    List<UUID> findFollowedAmong(@Param("viewerId") UUID viewerId, @Param("ids") List<UUID> ids);
}
