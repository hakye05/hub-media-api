package com.hakye.mediahub.api.auth.dto;

import com.hakye.mediahub.api.user.domain.Gender;
import com.hakye.mediahub.api.user.domain.UserValidator;
import jakarta.validation.constraints.*;

public record RegisterRequest(
        @NotBlank @Email @Size(max = 255)
        String email,

        @NotBlank @Size(min = 8, max = 72, message = "Password must be 8-72 characters")
        String password,

        @NotBlank @Size(max = UserValidator.DISPLAY_NAME_MAX, message = "Name must be at most 50 characters")
        String displayName,

        @NotBlank @Pattern(regexp = UserValidator.USERNAME_REGEX, message = "Username must be 3-30 characters: letters, numbers, dots, underscores")
        String username,

        @NotNull @Min(value = 13, message = "You must be at least 13") @Max(120)
        Integer age,

        @NotNull
        Gender gender
) {
}
