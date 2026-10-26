package com.hakye.mediahub.api.collection.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hakye.mediahub.api.user.dto.ProfileSummaryResponse;

import java.time.Instant;
import java.util.UUID;

public record CollectionDetailResponse(
        UUID id,
        String title,

        @JsonProperty("isPublic")
        boolean isPublic,

        boolean savedByViewer,
        Instant createdAt,
        Instant updatedAt,
        ProfileSummaryResponse author
) {
}
