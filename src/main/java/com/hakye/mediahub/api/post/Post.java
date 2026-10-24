package com.hakye.mediahub.api.post;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Entity
@Table(name = "posts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Setter
    @Column(nullable = false, length = 100)
    private String title;

    @Setter
    @Column(length = 500)
    private String description;

    @Column(name = "image_key", nullable = false, updatable = false, length = 255)
    private String imageKey;

    @Column(nullable = false, updatable = false)
    private Integer width;

    @Column(nullable = false, updatable = false)
    private Integer height;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Post(UUID userId, String title, String description, String imageKey, Integer width, Integer height) {
        this.userId = userId;
        this.title = title;
        this.description = description;
        this.imageKey = imageKey;
        this.width = width;
        this.height = height;
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
