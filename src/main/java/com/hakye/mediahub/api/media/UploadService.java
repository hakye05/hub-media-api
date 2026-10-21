package com.hakye.mediahub.api.media;

import com.hakye.mediahub.api.common.exceptions.BadRequestException;
import com.hakye.mediahub.api.media.dto.PresignUploadRequest;
import com.hakye.mediahub.api.media.dto.PresignedUploadResponse;
import com.hakye.mediahub.api.media.dto.StoredObject;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class UploadService {

    private static final Pattern FILE_NAME = Pattern.compile("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(jpg|png|webp)$");

    private final ObjectStorageService storage;
    private final StorageProperties props;

    public UploadService(ObjectStorageService storage, StorageProperties props) {
        this.storage = storage;
        this.props = props;
    }

    public PresignedUploadResponse presign(UUID userId, PresignUploadRequest req) {
        UploadKind kind = req.kind();
        String contentType = req.contentType().trim().toLowerCase();

        String extension = ImageTypes.extensionFor(contentType)
                .orElseThrow(() -> new BadRequestException("Unsupported image type. Allowed: JPEG, PNG, WebP"));
        if (req.size() > kind.maxBytes()) {
            throw new BadRequestException("File is too large (max " + kind.maxBytes() / 1024 + " KB)");
        }

        String key = kind.prefix() + "/" + userId + "/" + UUID.randomUUID() + "." + extension;
        String uploadUrl = storage.presignPut(key, contentType, req.size());

        return new PresignedUploadResponse(
                key,
                uploadUrl,
                "PUT",
                Map.of("Content-Type", contentType),
                props.presignExpirySeconds(),
                kind.maxBytes());
    }

    /**
     * Verifies a client-reported key and returns the public URL to store.
     * Rejects keys the API did not issue to this user, and objects that are missing,
     * of the wrong type, or over the size limit (those are deleted from the bucket).
     */
    public String confirm(UUID userId, UploadKind kind, String key) {
        String prefix = kind.prefix() + "/" + userId + "/";
        if (key == null || !key.startsWith(prefix) || !FILE_NAME.matcher(key.substring(prefix.length())).matches()) {
            throw new BadRequestException("Invalid upload key");
        }

        StoredObject object = storage.head(key)
                .orElseThrow(() -> new BadRequestException("Upload not found. Upload the file first"));

        String extension = key.substring(key.lastIndexOf('.') + 1);
        boolean typeOk = ImageTypes.extensionFor(object.contentType())
                .filter(extension::equals)
                .isPresent();
        boolean sizeOk = object.size() > 0 && object.size() <= kind.maxBytes();

        if (!typeOk || !sizeOk) {
            storage.deleteQuietly(key);
            throw new BadRequestException("Uploaded file is not an allowed image type or is too large");
        }
        return storage.publicUrl(key);
    }

    /** Deletes the object behind one of our public URLs; ignores null and foreign URLs. */
    public void deleteByUrl(String url) {
        storage.keyFromPublicUrl(url).ifPresent(storage::deleteQuietly);
    }
}
