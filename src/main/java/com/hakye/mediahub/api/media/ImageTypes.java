package com.hakye.mediahub.api.media;

import java.util.Map;
import java.util.Optional;

public final class ImageTypes {

    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp");

    private ImageTypes() {
    }

    public static Optional<String> extensionFor(String contentType) {
        return contentType == null
                ? Optional.empty()
                : Optional.ofNullable(EXTENSIONS.get(contentType.trim().toLowerCase()));
    }
}
