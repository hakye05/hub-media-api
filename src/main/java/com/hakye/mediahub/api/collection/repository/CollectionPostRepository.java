package com.hakye.mediahub.api.collection.repository;

import com.hakye.mediahub.api.collection.domain.CollectionPost;
import com.hakye.mediahub.api.collection.domain.CollectionPostId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface CollectionPostRepository extends JpaRepository<CollectionPost, CollectionPostId> {

    /** Idempotent: adding a post that is already in the collection is a no-op. */
    @Modifying
    @Query(value = """
            insert into collection_posts (collection_id, user_id, post_id, created_at)
            values (:collectionId, :userId, :postId, now())
            on conflict do nothing
            """, nativeQuery = true)
    int addIfAbsent(@Param("collectionId") UUID collectionId, @Param("userId") UUID userId,
                    @Param("postId") UUID postId);

    @Modifying
    @Query("delete from CollectionPost cp where cp.collectionId = :collectionId and cp.postId = :postId")
    int remove(@Param("collectionId") UUID collectionId, @Param("postId") UUID postId);

    /** Posts in a collection, most recently added first, strictly after the cursor. Pass limit + 1. */
    @Query(value = """
            select * from collection_posts
            where collection_id = :collectionId
              and (created_at, post_id) < (cast(:at as timestamptz), cast(:id as uuid))
            order by created_at desc, post_id desc
            limit :limit
            """, nativeQuery = true)
    List<CollectionPost> findPage(@Param("collectionId") UUID collectionId, @Param("at") Instant at,
                                  @Param("id") UUID id, @Param("limit") int limit);

    /** Per collection: [collection_id, post count, image key of the most recently added post]. */
    @Query(value = """
        select cp.collection_id,
               count(*),
               (select p.image_key
                  from collection_posts l
                  join posts p on p.id = l.post_id
                 where l.collection_id = cp.collection_id
                 order by l.created_at desc, l.post_id desc
                 limit 1)
          from collection_posts cp
         where cp.collection_id in (:collectionIds)
         group by cp.collection_id
        """, nativeQuery = true)
    List<Object[]> findStats(@Param("collectionIds") Collection<UUID> collectionIds);
}
