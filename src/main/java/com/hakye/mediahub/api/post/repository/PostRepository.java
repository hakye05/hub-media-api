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

    /**
     * Posts whose closest stored color is within :maxDistance (CIELAB) of the target, best match first.
     * Keyset-paginated on (distance, post id) ascending: pass the previous page's last row, or
     * (-1, zero uuid) for the first page. :pattern is optional (null = no keyword filter).
     */
    @Query(value = """
            select m.post_id as "postId", m.distance as "distance"
            from (
                select c.post_id,
                       min(sqrt(power(c.lab_l - :labL, 2) + power(c.lab_a - :labA, 2) + power(c.lab_b - :labB, 2))) as distance
                from post_colors c
                join posts p on p.id = c.post_id
                where (cast(:pattern as text) is null
                       or p.title ilike cast(:pattern as text) escape '!'
                       or p.description ilike cast(:pattern as text) escape '!')
                group by c.post_id
            ) m
            where m.distance <= :maxDistance
              and (m.distance, m.post_id) > (cast(:afterDistance as double precision), cast(:afterId as uuid))
            order by m.distance asc, m.post_id asc
            limit :limit
            """, nativeQuery = true)
    List<ColorMatch> searchByColor(@Param("pattern") String pattern,
                                   @Param("labL") double labL, @Param("labA") double labA, @Param("labB") double labB,
                                   @Param("maxDistance") double maxDistance,
                                   @Param("afterDistance") double afterDistance, @Param("afterId") UUID afterId,
                                   @Param("limit") int limit);

    /** One query, results in the order of {@code ids}; ids that no longer exist are skipped. */
    default List<Post> findAllInOrder(List<UUID> ids) {
        Map<UUID, Post> byId = findAllById(ids).stream()
                .collect(Collectors.toMap(Post::getId, Function.identity()));
        return ids.stream().map(byId::get).filter(Objects::nonNull).toList();
    }
}
