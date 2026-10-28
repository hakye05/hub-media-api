package com.hakye.mediahub.api.collection;

import com.hakye.mediahub.api.collection.repository.SavedPostRepository;
import com.hakye.mediahub.api.post.repository.SavedPostLookup;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Answers the post feature's "which of these did the viewer save?" from the saved_posts table. */
@Component
public class SavedPostLookupAdapter implements SavedPostLookup {

    private final SavedPostRepository saved;

    public SavedPostLookupAdapter(SavedPostRepository saved) {
        this.saved = saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Set<UUID> savedAmong(UUID userId, Collection<UUID> postIds) {
        if (userId == null || postIds.isEmpty()) return Set.of();
        return new HashSet<>(saved.findSavedPostIds(userId, postIds));
    }
}
