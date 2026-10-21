package com.hakye.mediahub.api.media;

public enum UploadKind {

    AVATAR("avatars", 1024L * 1024);

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
