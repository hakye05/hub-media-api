package com.hakye.mediahub.api.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank @Size(max = 72)
        String currentPassword,

        @NotBlank @Size(min = 8, max = 72, message = "Password must be 8-72 characters")
        String newPassword,

        Boolean logoutOtherDevices
) {
    public boolean shouldLogoutOtherDevices() {
        return logoutOtherDevices == null || logoutOtherDevices;
    }
}
