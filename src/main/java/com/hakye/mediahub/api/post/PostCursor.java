package com.hakye.mediahub.api.post;

import com.hakye.mediahub.api.common.exceptions.BadRequestException;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

public record PostCursor(
        Instant createdAt,
        UUID id
) {

    static String encode(Post p) {
        String raw = p.getCreatedAt() + "|" + p.getId();
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    /** null for a missing cursor (first page); BadRequestException for a malformed one. */
    static PostCursor decode(String cursor) {
        if (cursor == null || cursor.isBlank()) return null;
        try {
            String raw = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            int sep = raw.indexOf('|');
            return new PostCursor(Instant.parse(raw.substring(0, sep)),
                    UUID.fromString(raw.substring(sep + 1)));
        } catch (RuntimeException e) {
            throw new BadRequestException("Invalid cursor");
        }
    }
}
