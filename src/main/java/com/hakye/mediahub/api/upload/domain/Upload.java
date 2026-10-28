package com.hakye.mediahub.api.upload.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "uploads")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Upload {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, updatable = false)
    private UploadKind kind;

    @Enumerated(EnumType.STRING)
    @Column(name = "image_type", nullable = false, length = 10, updatable = false)
    private ImageType imageType;

    @Column(name = "size_bytes", nullable = false, updatable = false)
    private long sizeBytes;

    @Column
    private Integer width;

    @Column
    private Integer height;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UploadStatus status = UploadStatus.PENDING;

    @Column(name = "media_key", length = 255)
    private String mediaKey;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    public Upload(UUID userId, UploadKind kind, ImageType imageType, long sizeBytes, Instant expiresAt) {
        this.userId = userId;
        this.kind = kind;
        this.imageType = imageType;
        this.sizeBytes = sizeBytes;
        this.expiresAt = expiresAt;
    }

    public String finalKey() { return kind.prefix() + "/" + id + "." + imageType.extension(); }
    public String tmpKey()   { return "tmp/" + finalKey(); }

    public void markCompleted(ImageSize size) {
        this.status = UploadStatus.COMPLETED;
        this.mediaKey = finalKey();
        this.width = size.width();
        this.height = size.height();
        this.completedAt = Instant.now();
    }
}
