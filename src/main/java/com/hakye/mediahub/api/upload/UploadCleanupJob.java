package com.hakye.mediahub.api.upload;

import com.hakye.mediahub.api.storage.ObjectStorageService;
import com.hakye.mediahub.api.upload.domain.Upload;
import com.hakye.mediahub.api.upload.repository.UploadRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/** Hourly sweep of uploads that were never used. Objects under tmp/ expire on their own (R2 lifecycle rule). */
@Component
public class UploadCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(UploadCleanupJob.class);
    private static final Duration UNATTACHED_GRACE = Duration.ofHours(24);
    private static final Duration PENDING_RETENTION = Duration.ofDays(2);
    private static final int BATCH = 100;

    private final UploadRepository uploads;
    private final ObjectStorageService storage;

    public UploadCleanupJob(UploadRepository uploads, ObjectStorageService storage) {
        this.uploads = uploads;
        this.storage = storage;
    }

    @Scheduled(fixedDelayString = "PT1H", initialDelayString = "PT5M")
    public void run() {
        Instant now = Instant.now();

        for (Upload u : uploads.findUnattachedPostImages(now.minus(UNATTACHED_GRACE), BATCH)) {
            try {
                storage.delete(u.getMediaKey()); // idempotent: fine if already gone
                uploads.deleteById(u.getId());   // row removed only after the object is
            } catch (RuntimeException e) {
                log.warn("Cleanup failed for upload {}: {}", u.getId(), e.getMessage());
            }
        }
        uploads.deleteExpiredPending(now.minus(PENDING_RETENTION));
    }
}
