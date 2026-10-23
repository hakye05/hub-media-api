package com.hakye.mediahub.api.media;

import com.hakye.mediahub.api.common.exceptions.BadRequestException;
import com.hakye.mediahub.api.common.exceptions.ConflictException;
import com.hakye.mediahub.api.common.exceptions.NotFoundException;
import com.hakye.mediahub.api.media.dto.CreateUploadRequest;
import com.hakye.mediahub.api.media.dto.PresignedUploadResponse;
import com.hakye.mediahub.api.media.dto.UploadResponse;
import com.hakye.mediahub.api.user.User;
import com.hakye.mediahub.api.user.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class UploadService {

    /** Must stay far below the 1-day tmp/ lifecycle TTL. */
    private static final Duration FINALIZE_WINDOW = Duration.ofHours(1);
    private static final String IMMUTABLE_CACHE = "public, max-age=31536000, immutable";

    private final UploadRepository uploads;
    private final UserService userService;
    private final ObjectStorageService storage;
    private final StorageProperties props;
    private final TransactionTemplate tx;

    public UploadService(UploadRepository uploads, UserService userService,
                         ObjectStorageService storage, StorageProperties props,
                         TransactionTemplate tx) {
        this.uploads = uploads;
        this.userService = userService;
        this.storage = storage;
        this.props = props;
        this.tx = tx;
    }

    /** Steps 1-2: validate the request, record it, hand out a presigned PUT. */
    public PresignedUploadResponse create(UUID userId, CreateUploadRequest req) {
        ImageType type = ImageType.fromContentType(req.contentType())
                .orElseThrow(() -> new BadRequestException(
                        "Unsupported content type. Allowed: image/jpeg, image/png, image/webp"));
        if (req.size() > req.kind().maxBytes()) {
            throw new BadRequestException("File too large. Maximum for "
                    + req.kind().name().toLowerCase(Locale.ROOT) + " is "
                    + req.kind().maxBytes() / 1024 + " KB");
        }

        Upload upload = uploads.save(new Upload(userId, req.kind(), type, req.size(),
                Instant.now().plus(FINALIZE_WINDOW)));

        String url = storage.presignPut(upload.tmpKey(), type.contentType(), req.size());
        return new PresignedUploadResponse(
                upload.getId(), url, "PUT",
                Map.of("Content-Type", type.contentType()),
                Instant.now().plusSeconds(props.presignExpirySeconds()));
    }

    /** Steps 4-6: verify, promote, record. Safe to call again after success or a crash. */
    public UploadResponse complete(UUID userId, UUID uploadId) {
        Upload upload = uploads.findByIdAndUserId(uploadId, userId)
                .orElseThrow(() -> new NotFoundException("Upload not found"));

        if (upload.getStatus() == UploadStatus.COMPLETED) {
            return toResponse(upload);
        }
        if (Instant.now().isAfter(upload.getExpiresAt())) {
            throw new BadRequestException("Upload expired. Request a new upload");
        }

        verifyObject(upload);
        storage.copy(upload.tmpKey(), upload.finalKey(),
                upload.getImageType().contentType(), IMMUTABLE_CACHE);

        String replacedAvatarKey = tx.execute(status -> finish(userId, uploadId));

        if (replacedAvatarKey != null
                && !replacedAvatarKey.equals(upload.finalKey())
                && replacedAvatarKey.startsWith(UploadKind.AVATAR.prefix() + "/")) {
            storage.deleteQuietly(replacedAvatarKey);
        }
        return toResponse(upload);
    }

    /** Short DB-only transaction. Returns the avatar key that was replaced, if any. */
    private String finish(UUID userId, UUID uploadId) {
        Upload upload = uploads.findForUpdate(uploadId, userId)
                .orElseThrow(() -> new NotFoundException("Upload not found"));
        if (upload.getStatus() == UploadStatus.COMPLETED) {
            return null; // a concurrent call already finished it
        }
        upload.markCompleted();

        if (upload.getKind() == UploadKind.AVATAR) {
            User user = userService.getById(userId);
            String old = user.getAvatarKey();
            user.setAvatarKey(upload.finalKey());
            return old;
        }
        return null;
    }

    private void verifyObject(Upload upload) {
        String key = upload.tmpKey();
        HeadObjectResponse head = storage.head(key)
                .orElseThrow(() -> new ConflictException("File has not been uploaded yet"));

        if (head.contentLength() != upload.getSizeBytes()) {
            reject(key, "Uploaded file size does not match the declared size");
        }
        if (!upload.getImageType().contentType().equalsIgnoreCase(head.contentType())) {
            reject(key, "Uploaded file content type does not match the declared type");
        }
        if (!upload.getImageType().matches(storage.readHead(key, 12))) {
            reject(key, "File is not a valid " + upload.getImageType().contentType());
        }
    }

    private void reject(String tmpKey, String message) {
        storage.deleteQuietly(tmpKey);
        throw new BadRequestException(message);
    }

    private UploadResponse toResponse(Upload upload) {
        return new UploadResponse(upload.getId(), upload.finalKey(), storage.publicUrl(upload.finalKey()));
    }

    /** For attaching an upload to another entity: it must be the caller's, of the expected kind, and finalized. */
    public Upload getFinalizedUpload(UUID userId, UUID uploadId, UploadKind expectedKind) {
        Upload upload = uploads.findByIdAndUserId(uploadId, userId)
                .orElseThrow(() -> new NotFoundException("Upload not found"));
        if (upload.getKind() != expectedKind) {
            throw new BadRequestException("Upload was not created for "
                    + expectedKind.name().toLowerCase(Locale.ROOT) + " images");
        }
        if (upload.getStatus() != UploadStatus.COMPLETED) {
            throw new ConflictException("Upload has not been finalized. Call POST /api/uploads/"
                    + uploadId + "/complete first");
        }
        return upload;
    }
}
