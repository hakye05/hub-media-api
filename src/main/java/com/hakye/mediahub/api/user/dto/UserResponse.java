package com.hakye.mediahub.api.user.dto;

import com.hakye.mediahub.api.user.Gender;
import com.hakye.mediahub.api.user.User;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String displayName,
        String username,
        Integer age,
        Gender gender,
        String bio,
        String avatarUrl,
        Instant createdAt,
        Instant updatedAt
) {
    public static UserResponse from(User user, String avatarUrl) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getUsername(),
                user.getAge(),
                user.getGender(),
                user.getBio(),
                avatarUrl,
                user.getCreatedAt(),
                user.getUpdatedAt());
    }
}
