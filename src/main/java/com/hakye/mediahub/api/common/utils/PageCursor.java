package com.hakye.mediahub.api.common.utils;

import com.hakye.mediahub.api.common.exceptions.BadRequestException;
import com.hakye.mediahub.api.post.Post;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

public record PageCursor(
        Instant createdAt,
        UUID id
) {

    public static String encode(Post p) {
        return encode(p.getCreatedAt(), p.getId());
    }

    public static String encode(Instant createdAt, UUID id) {
        String raw = createdAt + "|" + id;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    /** null for a missing cursor (first page); BadRequestException for a malformed one. */
    public static PageCursor decode(String cursor) {
        if (cursor == null || cursor.isBlank()) return null;
        try {
            String raw = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            int sep = raw.indexOf('|');
            return new PageCursor(Instant.parse(raw.substring(0, sep)),
                    UUID.fromString(raw.substring(sep + 1)));
        } catch (RuntimeException e) {
            throw new BadRequestException("Invalid cursor");
        }
    }
}
