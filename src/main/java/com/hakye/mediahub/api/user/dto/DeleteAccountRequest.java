package com.hakye.mediahub.api.user.dto;

import jakarta.validation.constraints.NotBlank;

public record DeleteAccountRequest(@NotBlank String currentPassword) {
}
