package com.hakye.mediahub.api.post;

import com.hakye.mediahub.api.common.storage.ObjectStorageService;
import com.hakye.mediahub.api.media.Upload;
import com.hakye.mediahub.api.media.UploadKind;
import com.hakye.mediahub.api.media.UploadStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
public class UploadCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(UploadCleanupJob.class);
    private static final Duration UNATTACHED_GRACE = Duration.ofHours(24);
    private static final int BATCH = 100;

    private final UploadPostRepository uploads;
    private final ObjectStorageService storage;

    public UploadCleanupJob(UploadPostRepository uploads, ObjectStorageService storage) {
        this.uploads = uploads;
        this.storage = storage;
    }

    @Scheduled(fixedDelayString = "PT1H", initialDelayString = "PT5M")
    public void run() {
        Instant now = Instant.now();

        for (Upload u : uploads.findUnattached(UploadKind.POST, UploadStatus.COMPLETED,
                now.minus(UNATTACHED_GRACE), PageRequest.of(0, BATCH))) {
            try {
                storage.delete(u.getMediaKey()); // idempotent: fine if already gone
                uploads.deleteById(u.getId());   // row removed only after the object is
            } catch (RuntimeException e) {
                log.warn("Cleanup failed for upload {}: {}", u.getId(), e.getMessage());
            }
        }
        // PENDING rows are never touched by the file side: tmp/ expires on its own.
        uploads.deleteExpired(UploadStatus.PENDING, now.minus(Duration.ofDays(2)));
    }
}
