package com.hakye.mediahub.api.collection.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "saved_collections")
@IdClass(SavedCollectionId.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SavedCollection {

    @Id
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Id
    @Column(name = "collection_id", nullable = false, updatable = false)
    private UUID collectionId;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt; // set by the DB default
}
