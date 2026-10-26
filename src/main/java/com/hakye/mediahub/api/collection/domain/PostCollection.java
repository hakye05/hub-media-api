package com.hakye.mediahub.api.collection.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Entity
@Table(name = "collections")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostCollection {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Setter
    @Column(nullable = false, length = 100)
    private String title;

    @Setter
    @Column(name = "is_public", nullable = false)
    private boolean isPublic;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public PostCollection(UUID userId, String title, boolean isPublic) {
        this.userId = userId;
        this.title = title;
        this.isPublic = isPublic;
    }

    public boolean isVisibleTo(UUID viewerId) {
        return isPublic || userId.equals(viewerId);
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
    }
}