package com.hakye.mediahub.api.user.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ChangeAvatarRequest(
        @NotNull(message = "uploadId is required")
        UUID uploadId
) {
}