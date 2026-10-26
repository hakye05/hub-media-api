package com.hakye.mediahub.api.media.image;

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
}
