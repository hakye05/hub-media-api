package com.hakye.mediahub.api.collection.repository;

import com.hakye.mediahub.api.collection.domain.SavedPost;
import com.hakye.mediahub.api.collection.domain.SavedPostId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface SavedPostRepository extends JpaRepository<SavedPost, SavedPostId> {

    /** Idempotent: saving a post that is already saved is a no-op. */
    @Modifying
    @Query(value = """
            insert into saved_posts (user_id, post_id, created_at)
            values (:userId, :postId, now())
            on conflict do nothing
            """, nativeQuery = true)
    int saveIfAbsent(@Param("userId") UUID userId, @Param("postId") UUID postId);

    @Modifying
    @Query("delete from SavedPost s where s.userId = :userId and s.postId = :postId")
    int unsave(@Param("userId") UUID userId, @Param("postId") UUID postId);

    /** The user's saved posts, most recently saved first, strictly after the cursor. Pass limit + 1. */
    @Query(value = """
            select * from saved_posts
            where user_id = :userId
              and (created_at, post_id) < (cast(:at as timestamptz), cast(:id as uuid))
            order by created_at desc, post_id desc
            limit :limit
            """, nativeQuery = true)
    List<SavedPost> findPage(@Param("userId") UUID userId, @Param("at") Instant at,
                             @Param("id") UUID id, @Param("limit") int limit);

    /** Which of these posts has the user saved? One query per page instead of one per post. */
    @Query("select s.postId from SavedPost s where s.userId = :userId and s.postId in :postIds")
    List<UUID> findSavedPostIds(@Param("userId") UUID userId, @Param("postIds") Collection<UUID> postIds);
}
