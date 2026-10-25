package com.hakye.mediahub.api.collection.domain;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class CollectionPostId implements Serializable {
    private UUID collectionId;
    private UUID postId;
}
