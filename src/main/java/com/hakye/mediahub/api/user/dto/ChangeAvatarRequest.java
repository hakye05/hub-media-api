package com.hakye.mediahub.api.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangeAvatarRequest(
        @NotBlank @Size(max = 200)
        String key
) {
}
