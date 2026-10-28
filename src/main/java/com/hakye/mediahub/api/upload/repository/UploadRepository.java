package com.hakye.mediahub.api.upload.repository;

import com.hakye.mediahub.api.upload.domain.Upload;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UploadRepository extends JpaRepository<Upload, UUID> {

    Optional<Upload> findByIdAndUserId(UUID id, UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from Upload u where u.id = :id and u.userId = :userId")
    Optional<Upload> findForUpdate(@Param("id") UUID id, @Param("userId") UUID userId);

    /** Finalized post images that never got attached to a post (SQL, so upload doesn't depend on post). */
    @Query(value = """
            select u.* from uploads u
            where u.kind = 'POST' and u.status = 'COMPLETED' and u.completed_at < :before
              and not exists (select 1 from posts p where p.image_key = u.media_key)
            order by u.completed_at
            limit :limit
            """, nativeQuery = true)
    List<Upload> findUnattachedPostImages(@Param("before") Instant before, @Param("limit") int limit);

    @Transactional
    @Modifying
    @Query("delete from Upload u where u.status = com.hakye.mediahub.api.upload.domain.UploadStatus.PENDING and u.expiresAt < :before")
    int deleteExpiredPending(@Param("before") Instant before);
}
