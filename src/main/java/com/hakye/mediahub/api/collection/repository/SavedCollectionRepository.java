package com.hakye.mediahub.api.collection.repository;

import com.hakye.mediahub.api.collection.domain.SavedCollection;
import com.hakye.mediahub.api.collection.domain.SavedCollectionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface SavedCollectionRepository extends JpaRepository<SavedCollection, SavedCollectionId> {

    @Modifying
    @Query(value = """
            INSERT INTO saved_collections (user_id, collection_id)
            VALUES (:userId, :collectionId)
            ON CONFLICT DO NOTHING
            """, nativeQuery = true)
    void saveIfAbsent(@Param("userId") UUID userId, @Param("collectionId") UUID collectionId);

    @Modifying
    @Query("DELETE FROM SavedCollection s WHERE s.userId = :userId AND s.collectionId = :collectionId")
    void unsave(@Param("userId") UUID userId, @Param("collectionId") UUID collectionId);

    /** Newest save first. Collections that have since gone private are hidden. */
    @Query(value = """
            SELECT sc.* FROM saved_collections sc
            JOIN collections c ON c.id = sc.collection_id
            WHERE sc.user_id = :userId
              AND c.is_public = TRUE
              AND (sc.created_at, sc.collection_id) < (:createdAt, :id)
            ORDER BY sc.created_at DESC, sc.collection_id DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<SavedCollection> findPage(@Param("userId") UUID userId,
                                   @Param("createdAt") Instant createdAt,
                                   @Param("id") UUID id,
                                   @Param("limit") int limit);

    @Query("SELECT s.collectionId FROM SavedCollection s WHERE s.userId = :userId AND s.collectionId IN :ids")
    List<UUID> findSavedCollectionIds(@Param("userId") UUID userId, @Param("ids") Collection<UUID> ids);
}
