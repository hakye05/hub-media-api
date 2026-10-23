package com.hakye.mediahub.api.common.dto;

import java.util.List;

public record CursorPage<T>(
        List<T> items,
        String nextCursor
) {
}
