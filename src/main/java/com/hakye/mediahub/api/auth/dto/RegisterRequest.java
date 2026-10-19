package com.hakye.mediahub.api.auth.dto;

import com.hakye.mediahub.api.user.Gender;
import jakarta.validation.constraints.*;

public record RegisterRequest(
        @NotBlank @Email @Size(max = 255)
        String email,

        @NotBlank @Size(min = 8, max = 72, message = "Password must be 8-72 characters")
        String password,

        @NotBlank @Size(max = 60, message = "Name must be at most 60 characters")
        String displayName,

        @NotBlank @Pattern(regexp = "^[A-Za-z0-9_.]{3,30}$", message = "Username must be 3-30 characters: letters, numbers, dots, underscores")
        String username,

        @NotNull @Min(value = 13, message = "You must be at least 13") @Max(120)
        Integer age,

        @NotNull
        Gender gender
) {
}
