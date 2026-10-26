package com.hakye.mediahub.api.collection.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

public record CollectionResponse(
        UUID id,
        UUID userId,
        String title,

        @JsonProperty("isPublic")
        boolean isPublic,

        boolean savedByViewer,
        long postCount,
        String coverImageUrl,
        Instant createdAt,
        Instant updatedAt
) {
}
