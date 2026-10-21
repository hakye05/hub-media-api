package com.hakye.mediahub.api.user.dto;

import com.hakye.mediahub.api.user.Gender;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(

    @Size(max = 50, message = "Name must be at most 50 characters")
    String displayName,

    Gender gender,

    @Size(max = 500, message = "Bio must be at most 500 characters")
    String bio
) {
}
