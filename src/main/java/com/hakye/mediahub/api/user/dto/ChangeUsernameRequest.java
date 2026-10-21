package com.hakye.mediahub.api.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ChangeUsernameRequest(
        @NotBlank
        @Pattern(regexp = "^[A-Za-z0-9_.]{3,30}$",
                message = "Username must be 3-30 characters: letters, numbers, dots, underscores")
        String username
) {
}
