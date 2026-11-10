package com.hakye.mediahub.api.post.color;

import com.hakye.mediahub.api.common.exceptions.BadRequestException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

/**
 * Keyset position for color search, which is ordered by (distance, id) ascending:
 * "everything strictly after this match". A missing cursor decodes to {@link #START}.
 */
public record ColorCursor(double distance, UUID id) {

    private static final ColorCursor START = new ColorCursor(-1.0, new UUID(0L, 0L));

    public String encode() {
        String raw = distance + "|" + id;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public static ColorCursor decode(String cursor) {
        if (cursor == null || cursor.isBlank()) return START;
        try {
            String raw = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            int sep = raw.indexOf('|');
            double distance = Double.parseDouble(raw.substring(0, sep));
            if (Double.isNaN(distance) || Double.isInfinite(distance)) throw new IllegalArgumentException();
            return new ColorCursor(distance, UUID.fromString(raw.substring(sep + 1)));
        } catch (RuntimeException e) {
            throw new BadRequestException("Invalid cursor");
        }
    }
}