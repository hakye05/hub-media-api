package com.hakye.mediahub.api.media;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UploadRepository extends JpaRepository<Upload, UUID> {

    Optional<Upload> findByIdAndUserId(UUID id, UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from Upload u where u.id = :id and u.userId = :userId")
    Optional<Upload> findForUpdate(@Param("id") UUID id, @Param("userId") UUID userId);
}
