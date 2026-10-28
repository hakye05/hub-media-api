package com.hakye.mediahub.api.common.utils;

import com.hakye.mediahub.api.common.exceptions.BadRequestException;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

/**
 * Keyset position: "everything strictly older than (createdAt, id)".
 * Opaque to clients (url-safe base64). A missing cursor decodes to {@link #START}, a position
 * past every real row, so the first page and later pages share one query.
 */
public record PageCursor(
        Instant createdAt,
        UUID id
) {

    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 50;

    /** String form of DEFAULT_SIZE for @RequestParam(defaultValue = ...), which needs a constant. */
    public static final String DEFAULT_SIZE_PARAM = "20";

    private static final PageCursor START =
            new PageCursor(Instant.parse("9999-12-31T23:59:59Z"), new UUID(-1L, -1L));

    public static int clampSize(int size) {
        return Math.clamp(size, 1, MAX_SIZE);
    }

    public String encode() {
        String raw = createdAt + "|" + id;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    /** {@link #START} for a missing cursor; BadRequestException for a malformed one. */
    public static PageCursor decode(String cursor) {
        if (cursor == null || cursor.isBlank()) return START;
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
