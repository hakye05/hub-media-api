package com.hakye.mediahub.api.collection.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public record CollectionSummaryResponse(
        UUID id,
        String title,

        @JsonProperty("isPublic")
        boolean isPublic
) {
}
