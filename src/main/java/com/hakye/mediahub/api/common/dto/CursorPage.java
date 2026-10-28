package com.hakye.mediahub.api.common.dto;

import com.hakye.mediahub.api.common.utils.PageCursor;

import java.util.List;
import java.util.function.Function;

public record CursorPage<T>(
        List<T> items,
        String nextCursor
) {

    /**
     * Builds a page from rows fetched with {@code limit + 1}: the extra row only signals that
     * another page exists. The next cursor points at the last row that is returned.
     *
     * @param cursorOf position of a row in the sort order
     * @param mapper   turns the returned rows into response items (batch lookups go here)
     */
    public static <S, T> CursorPage<T> of(List<S> fetched, int limit,
                                          Function<S, PageCursor> cursorOf,
                                          Function<List<S>, List<T>> mapper) {
        boolean hasMore = fetched.size() > limit;
        List<S> page = hasMore ? fetched.subList(0, limit) : fetched;
        String next = hasMore ? cursorOf.apply(page.get(page.size() - 1)).encode() : null;
        return new CursorPage<>(mapper.apply(page), next);
    }

    /** Same page, different items (e.g. ids resolved to profiles). */
    public <R> CursorPage<R> mapItems(Function<List<T>, List<R>> mapper) {
        return new CursorPage<>(mapper.apply(items), nextCursor);
    }
}
