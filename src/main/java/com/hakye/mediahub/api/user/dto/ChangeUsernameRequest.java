package com.hakye.mediahub.api.user.dto;

import com.hakye.mediahub.api.user.domain.UserValidator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ChangeUsernameRequest(
        @NotBlank
        @Pattern(regexp = UserValidator.USERNAME_REGEX, message = "Username must be 3-30 characters: letters, numbers, dots, underscores")
        String username
) {
}
