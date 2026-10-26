package com.hakye.mediahub.api.post.dto;

import jakarta.validation.constraints.Size;

public record UpdatePostRequest(
        @Size(max = 100, message = "Title must be at most 100 characters")
        String title,

        @Size(max = 500, message = "Description must be at most 500 characters")
        String description
) {
}
