package com.hakye.mediahub.api.collection.repository;

import com.hakye.mediahub.api.collection.domain.PostCollection;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PostCollectionRepository extends JpaRepository<PostCollection, UUID> {

    Optional<PostCollection> findByIdAndUserId(UUID id, UUID userId);

    @Modifying
    @Query("delete from PostCollection c where c.id = :id and c.userId = :userId")
    int deleteOwned(@Param("id") UUID id, @Param("userId") UUID userId);

    @Query(value = """
        select * from collections
        where user_id = :userId
          and (:includePrivate or is_public)
        order by created_at desc, id desc
        """, nativeQuery = true)
    List<PostCollection> findFirstPage(@Param("userId") UUID userId,
                                       @Param("includePrivate") boolean includePrivate,
                                       Pageable pageable);

    @Query(value = """
        select * from collections
        where user_id = :userId
          and (:includePrivate or is_public)
          and (created_at, id) < (cast(:at as timestamptz), cast(:id as uuid))
        order by created_at desc, id desc
        """, nativeQuery = true)
    List<PostCollection> findPageAfter(@Param("userId") UUID userId,
                                       @Param("includePrivate") boolean includePrivate,
                                       @Param("at") Instant at, @Param("id") UUID id,
                                       Pageable pageable);

    @Query(value = """
        select c.* from collections c
        join collection_posts cp on cp.collection_id = c.id
        where cp.user_id = :userId and cp.post_id = :postId
        order by cp.created_at desc, c.id desc
        """, nativeQuery = true)
    List<PostCollection> findContainingPost(@Param("userId") UUID userId, @Param("postId") UUID postId);
}
