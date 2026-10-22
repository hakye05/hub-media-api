package com.hakye.mediahub.api.media.dto;

import com.hakye.mediahub.api.media.UploadKind;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PresignUploadRequest(
        @NotNull UploadKind kind,
        @NotBlank String contentType,
        @NotNull @Positive Long size
) {
}
