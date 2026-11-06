package com.hakye.mediahub.api.common.utils;

import com.hakye.mediahub.api.common.exceptions.BadRequestException;

public final class SearchQuery {

    public static final int MAX_LENGTH = 100;

    private SearchQuery() {
    }

    /** The trimmed query; blank or oversized input is a 400. */
    public static String require(String raw) {
        String q = raw == null ? "" : raw.trim();
        if (q.isEmpty()) throw new BadRequestException("Search query cannot be blank");
        if (q.length() > MAX_LENGTH) throw new BadRequestException("Search query is too long");
        return q;
    }

    /** Lowercased "contains" pattern; %, _ and ! in the input match literally. Escape char is '!'. */
    public static String containsPattern(String q) {
        String escaped = q.toLowerCase().replace("!", "!!").replace("%", "!%").replace("_", "!_");
        return "%" + escaped + "%";
    }
}
