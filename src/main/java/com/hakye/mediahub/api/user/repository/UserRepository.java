package com.hakye.mediahub.api.user.repository;

import com.hakye.mediahub.api.user.domain.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Email and username are stored lowercase: callers pass UserValidator.normalizeString(...) values. */
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);
    Optional<User> findByUsername(String username);

    boolean existsByEmail(String email);
    boolean existsByUsername(String username);

    /** One query, results in the order of {@code ids}; ids that no longer exist are skipped. */
    default List<User> findAllInOrder(List<UUID> ids) {
        Map<UUID, User> byId = findAllById(ids).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        return ids.stream().map(byId::get).filter(Objects::nonNull).toList();
    }

    /** Name or username contains :pattern (lowercase, '!' escape), newest accounts first, strictly after the cursor. */
    @Query("""
            select u from User u
            where (lower(u.username) like :pattern escape '!' or lower(u.displayName) like :pattern escape '!')
              and (u.createdAt < :at or (u.createdAt = :at and u.id < :id))
            order by u.createdAt desc, u.id desc
            """)
    List<User> search(@Param("pattern") String pattern, @Param("at") Instant at,
                      @Param("id") UUID id, Pageable page);

    @Modifying
    @Query("DELETE FROM User u WHERE u.id = :id")
    void deleteAccount(@Param("id") UUID id);
}
