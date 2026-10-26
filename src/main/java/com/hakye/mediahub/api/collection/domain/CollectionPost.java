package com.hakye.mediahub.api.collection.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/** The table also has a user_id column; it only exists for the composite foreign keys and is written by SQL. */
@Entity
@Table(name = "collection_posts")
@IdClass(CollectionPostId.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CollectionPost {

    @Id
    @Column(name = "collection_id", nullable = false, updatable = false)
    private UUID collectionId;

    @Id
    @Column(name = "post_id", nullable = false, updatable = false)
    private UUID postId;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt; // set by the DB default
}