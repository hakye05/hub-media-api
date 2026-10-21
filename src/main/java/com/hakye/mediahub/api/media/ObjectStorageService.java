package com.hakye.mediahub.api.media;

import com.hakye.mediahub.api.media.dto.StoredObject;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
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

    public Optional<StoredObject> head(String key) {
        try {
            HeadObjectResponse r = s3Client.headObject(b -> b.bucket(props.bucket()).key(key));
            long size = r.contentLength() == null ? 0 : r.contentLength();
            return Optional.of(new StoredObject(size, r.contentType()));
        } catch (NoSuchKeyException e) {
            return Optional.empty();
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                return Optional.empty();
            }
            throw e;
        }
    }

    public void deleteQuietly(String key) {
        try {
            s3Client.deleteObject(b -> b.bucket(props.bucket()).key(key));
        } catch (RuntimeException e) {
            log.warn("Could not delete object {}: {}", key, e.getMessage());
        }
    }

    public String publicUrl(String key) {
        return publicBase + "/" + key;
    }

    /** Reverse of publicUrl; empty for URLs that don't belong to bucket. */
    public Optional<String> keyFromPublicUrl(String url) {
        String prefix = publicBase + "/";
        if (url == null || !url.startsWith(prefix) || url.length() == prefix.length()) {
            return Optional.empty();
        }
        return Optional.of(url.substring(prefix.length()));
    }
}
