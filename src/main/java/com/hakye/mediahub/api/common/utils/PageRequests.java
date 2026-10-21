package com.hakye.mediahub.api.common.utils;

import org.springframework.data.domain.PageRequest;

public final class PageRequests {

    public static final int DEFAULT_MAX_SIZE = 50;

    private PageRequests() {
    }

    public static PageRequest of(int page, int size) {
        return of(page, size, DEFAULT_MAX_SIZE);
    }

    public static PageRequest of(int page, int size, int maxSize) {
        return PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, maxSize));
    }
}