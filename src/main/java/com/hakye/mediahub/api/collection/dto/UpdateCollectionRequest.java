package com.hakye.mediahub.api.collection.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Size;

public record UpdateCollectionRequest(
        @Size(max = 100, message = "Title must be at most 100 characters")
        String title,

        @JsonProperty("isPublic")
        Boolean isPublic
) {
}
