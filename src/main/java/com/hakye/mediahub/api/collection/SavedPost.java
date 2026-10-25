package com.hakye.mediahub.api.collection;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "saved_posts")
@IdClass(SavedPostId.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SavedPost {

    @Id
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Id
    @Column(name = "post_id", nullable = false, updatable = false)
    private UUID postId;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;
}
