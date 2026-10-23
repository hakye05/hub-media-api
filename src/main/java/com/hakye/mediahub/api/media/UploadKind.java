package com.hakye.mediahub.api.media;

public enum UploadKind {

    AVATAR("avatars", 400L * 1024),
    POST("posts", 10L * 1024 * 1024);

    private final String prefix;
    private final long maxBytes;

    UploadKind(String prefix, long maxBytes) {
        this.prefix = prefix;
        this.maxBytes = maxBytes;
    }

    public String prefix() {
        return prefix;
    }

    public long maxBytes() {
        return maxBytes;
    }
}
