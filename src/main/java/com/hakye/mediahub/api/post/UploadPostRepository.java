package com.hakye.mediahub.api.post;

import com.hakye.mediahub.api.media.Upload;
import com.hakye.mediahub.api.media.UploadKind;
import com.hakye.mediahub.api.media.UploadStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface UploadPostRepository extends Repository<Upload, UUID> {

    @Query("""
        select u from Upload u
        where u.kind = :kind and u.status = :status and u.completedAt < :before
          and not exists (select 1 from Post p where p.imageKey = u.mediaKey)
        """)
    List<Upload> findUnattached(@Param("kind") UploadKind kind, @Param("status") UploadStatus status,
                                @Param("before") Instant before, Pageable pageable);

    @Modifying
    @Transactional
    @Query("delete from Upload u where u.status = :status and u.expiresAt < :before")
    int deleteExpired(@Param("status") UploadStatus status, @Param("before") Instant before);

    void deleteById(UUID id);
}
