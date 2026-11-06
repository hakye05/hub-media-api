package com.hakye.mediahub.api.collection.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hakye.mediahub.api.user.dto.ProfileSummaryResponse;

import java.time.Instant;
import java.util.UUID;

public record CollectionSearchResponse(
        UUID id,
        UUID userId,
        String title,

        @JsonProperty("isPublic")
        boolean isPublic,

        boolean savedByViewer,
        long postCount,
        String coverImageUrl,
        Instant createdAt,
        Instant updatedAt,
        ProfileSummaryResponse owner
) {
    public static CollectionSearchResponse of(CollectionResponse c, ProfileSummaryResponse owner) {
        return new CollectionSearchResponse(c.id(), c.userId(), c.title(), c.isPublic(), c.savedByViewer(),
                c.postCount(), c.coverImageUrl(), c.createdAt(), c.updatedAt(), owner);
    }
}
