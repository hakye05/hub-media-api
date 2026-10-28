package com.hakye.mediahub.api.user.repository;

import com.hakye.mediahub.api.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
