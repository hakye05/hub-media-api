package com.hakye.mediahub.api.storage;

import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.Optional;

@Service
public class ObjectStorageService {

    private static final Logger log = LoggerFactory.getLogger(ObjectStorageService.class);

    private final S3Client s3Client;
    private final S3Presigner presigner;
    private final StorageProperties props;
    private final String publicBase;

    public ObjectStorageService(S3Client s3Client, S3Presigner presigner, StorageProperties props) {
        this.s3Client = s3Client;
        this.presigner = presigner;
        this.props = props;
        this.publicBase = props.publicBaseUrl().replaceAll("/+$", "");
    }

    /** Presigned PUT URL. Content-Type and Content-Length are part of the signature. */
    public String presignPut(String key, String contentType, long size) {
        PutObjectRequest put = PutObjectRequest.builder()
                .bucket(props.bucket())
                .key(key)
                .contentType(contentType)
                .contentLength(size)
                .build();

        PutObjectPresignRequest request = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(props.presignExpirySeconds()))
                .putObjectRequest(put)
                .build();

        return presigner.presignPutObject(request).url().toString();
    }

    public Optional<HeadObjectResponse> head(String key) {
        try {
            return Optional.of(s3Client.headObject(b -> b.bucket(props.bucket()).key(key)));
        } catch (NoSuchKeyException e) {
            return Optional.empty();
        }
    }

    /** First {@code count} bytes of the object, for magic-byte sniffing. */
    public byte[] readHead(String key, int count) {
        return s3Client.getObjectAsBytes(b -> b.bucket(props.bucket()).key(key)
                .range("bytes=0-" + (count - 1))).asByteArray();
    }

    /** R2 has no rename: copy to the final key. The tmp object is left to the lifecycle rule. */
    public void copy(String sourceKey, String destKey, String contentType, String cacheControl) {
        s3Client.copyObject(b -> b
                .sourceBucket(props.bucket()).sourceKey(sourceKey)
                .destinationBucket(props.bucket()).destinationKey(destKey)
                .metadataDirective(MetadataDirective.REPLACE)
                .contentType(contentType)
                .cacheControl(cacheControl));
    }

    public void delete(String key) {
        s3Client.deleteObject(b -> b.bucket(props.bucket()).key(key));
    }

    public void deleteQuietly(String key) {
        try {
            delete(key);
        } catch (RuntimeException e) {
            log.warn("Could not delete object {}: {}", key, e.getMessage());
        }
    }

    public String publicUrl(String key) {
        return key == null ? null : publicBase + "/" + key;
    }
}
