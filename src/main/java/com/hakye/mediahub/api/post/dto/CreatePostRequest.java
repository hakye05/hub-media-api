package com.hakye.mediahub.api.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record CreatePostRequest(
        @NotNull(message = "uploadId is required")
        UUID uploadId,

        @NotBlank(message = "Title is required")
        @Size(max = 100, message = "Title must be at most 100 characters")
        String title,

        @Size(max = 500, message = "Description must be at most 500 characters")
        String description,

        @Size(max = 5, message = "At most 5 colors")
        List<@Pattern(regexp = "^#?[0-9a-fA-F]{6}$", message = "Colors must be hex values like #8F3030") String> colors
) {
}
