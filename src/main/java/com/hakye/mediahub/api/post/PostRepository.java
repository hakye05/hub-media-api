package com.hakye.mediahub.api.post;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PostRepository extends JpaRepository<Post, UUID> {

    Optional<Post> findByIdAndUserId(UUID id, UUID userId);

    List<Post> findAllByOrderByCreatedAtDescIdDesc(Pageable pageable);

    @Query(value = """
            select * from posts
            where (created_at, id) < (cast(:at as timestamptz), cast(:id as uuid))
            order by created_at desc, id desc
            """, nativeQuery = true)
    List<Post> findFeedAfter(@Param("at") Instant at, @Param("id") UUID id, Pageable pageable);

    List<Post> findByUserIdOrderByCreatedAtDescIdDesc(UUID userId, Pageable pageable);

    @Query(value = """
            select * from posts
            where user_id = :userId
              and (created_at, id) < (cast(:at as timestamptz), cast(:id as uuid))
            order by created_at desc, id desc
            """, nativeQuery = true)
    List<Post> findByUserAfter(@Param("userId") UUID userId, @Param("at") Instant at,
                               @Param("id") UUID id, Pageable pageable);
}
