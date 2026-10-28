package com.hakye.mediahub.api.post.repository;

import com.hakye.mediahub.api.post.domain.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Every list is keyset-paginated, newest first: rows strictly older than (:at, :id).
 * The caller passes limit + 1 so it can tell whether another page exists.
 */
public interface PostRepository extends JpaRepository<Post, UUID> {

    Optional<Post> findByIdAndUserId(UUID id, UUID userId);

    @Query(value = """
            select * from posts
            where (created_at, id) < (cast(:at as timestamptz), cast(:id as uuid))
            order by created_at desc, id desc
            limit :limit
            """, nativeQuery = true)
    List<Post> findFeed(@Param("at") Instant at, @Param("id") UUID id, @Param("limit") int limit);

    @Query(value = """
            select * from posts
            where user_id = :userId
              and (created_at, id) < (cast(:at as timestamptz), cast(:id as uuid))
            order by created_at desc, id desc
            limit :limit
            """, nativeQuery = true)
    List<Post> findByUser(@Param("userId") UUID userId, @Param("at") Instant at,
                          @Param("id") UUID id, @Param("limit") int limit);

    /** Posts from the users :viewerId follows. */
    @Query(value = """
            select p.* from posts p
            join follows f on f.followee_id = p.user_id
            where f.follower_id = :viewerId
              and (p.created_at, p.id) < (cast(:at as timestamptz), cast(:id as uuid))
            order by p.created_at desc, p.id desc
            limit :limit
            """, nativeQuery = true)
    List<Post> findFollowingFeed(@Param("viewerId") UUID viewerId, @Param("at") Instant at,
                                 @Param("id") UUID id, @Param("limit") int limit);

    /** :pattern is a LIKE pattern that escapes with '!'. */
    @Query(value = """
            select * from posts
            where (title ilike :pattern escape '!' or description ilike :pattern escape '!')
              and (created_at, id) < (cast(:at as timestamptz), cast(:id as uuid))
            order by created_at desc, id desc
            limit :limit
            """, nativeQuery = true)
    List<Post> search(@Param("pattern") String pattern, @Param("at") Instant at,
                      @Param("id") UUID id, @Param("limit") int limit);

    /** One query, results in the order of {@code ids}; ids that no longer exist are skipped. */
    default List<Post> findAllInOrder(List<UUID> ids) {
        Map<UUID, Post> byId = findAllById(ids).stream()
                .collect(Collectors.toMap(Post::getId, Function.identity()));
        return ids.stream().map(byId::get).filter(Objects::nonNull).toList();
    }
}
