package com.hakye.mediahub.api.upload.dto;

import com.hakye.mediahub.api.upload.domain.UploadKind;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateUploadRequest(
        @NotNull(message = "kind is required") UploadKind kind,
        @NotBlank(message = "contentType is required") String contentType,
        @Positive(message = "size must be greater than 0") long size
) {
}
