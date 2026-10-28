package com.hakye.mediahub.api.post.repository;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

/**
 * What the post feature needs to know about saved posts. Implemented by the collection feature,
 * which owns saving; declaring it here keeps post from depending on collection.
 */
public interface SavedPostLookup {

    /** Which of {@code postIds} the user has saved. Guests (null user) and empty input yield an empty set. */
    Set<UUID> savedAmong(UUID userId, Collection<UUID> postIds);
}
