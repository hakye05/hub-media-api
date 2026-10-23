package com.hakye.mediahub.api.media;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public enum ImageType {
    JPEG("image/jpeg", "jpg"),
    PNG("image/png", "png"),
    WEBP("image/webp", "webp");

    private final String contentType;
    private final String extension;

    ImageType(String contentType, String extension) {
        this.contentType = contentType;
        this.extension = extension;
    }

    public String contentType() { return contentType; }
    public String extension() { return extension; }

    public static Optional<ImageType> fromContentType(String value) {
        if (value == null) return Optional.empty();
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return Arrays.stream(values()).filter(t -> t.contentType.equals(normalized)).findFirst();
    }

    /** Checks the file signature of the first bytes of the object. */
    public boolean matches(byte[] h) {
        return switch (this) {
            case JPEG -> has(h, 0, 0xFF, 0xD8, 0xFF);
            case PNG  -> has(h, 0, 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A);
            case WEBP -> has(h, 0, 'R', 'I', 'F', 'F') && has(h, 8, 'W', 'E', 'B', 'P');
        };
    }

    private static boolean has(byte[] h, int offset, int... expected) {
        if (h.length < offset + expected.length) return false;
        for (int i = 0; i < expected.length; i++) {
            if ((h[offset + i] & 0xFF) != expected[i]) return false;
        }
        return true;
    }
}
