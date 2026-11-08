package com.hakye.mediahub.api.collection;

import com.hakye.mediahub.api.collection.domain.PostCollection;
import com.hakye.mediahub.api.collection.domain.SavedCollection;
import com.hakye.mediahub.api.collection.dto.CollectionSearchResponse;
import com.hakye.mediahub.api.collection.repository.PostCollectionRepository;
import com.hakye.mediahub.api.collection.repository.SavedCollectionRepository;
import com.hakye.mediahub.api.common.dto.CursorPage;
import com.hakye.mediahub.api.common.exceptions.BadRequestException;
import com.hakye.mediahub.api.common.exceptions.NotFoundException;
import com.hakye.mediahub.api.common.utils.PageCursor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Saved collections are private: every method is scoped to the caller's own user id. */
@Service
public class SavedCollectionService {

    private static final String NOT_FOUND = "Collection not found";

    private final SavedCollectionRepository saved;
    private final PostCollectionRepository collections;
    private final CollectionService collectionService;

    public SavedCollectionService(SavedCollectionRepository saved, PostCollectionRepository collections,
                                  CollectionService collectionService) {
        this.saved = saved;
        this.collections = collections;
        this.collectionService = collectionService;
    }

    /** Idempotent. Private collections look nonexistent to everyone but their owner. */
    @Transactional
    public void save(UUID userId, UUID collectionId) {
        PostCollection collection = collections.findById(collectionId)
                .filter(c -> c.isVisibleTo(userId))
                .orElseThrow(() -> new NotFoundException(NOT_FOUND));
        if (collection.getUserId().equals(userId)) {
            throw new BadRequestException("You can't save your own collection");
        }
        try {
            saved.saveIfAbsent(userId, collectionId);
        } catch (DataIntegrityViolationException e) {
            throw new NotFoundException(NOT_FOUND); // deleted between the check and the insert
        }
    }

    /** Idempotent. */
    @Transactional
    public void unsave(UUID userId, UUID collectionId) {
        saved.unsave(userId, collectionId);
    }

    /** The caller's saved collections, most recently saved first. */
    @Transactional(readOnly = true)
    public CursorPage<CollectionSearchResponse> list(UUID userId, String cursor, int size) {
        int limit = PageCursor.clampSize(size);
        PageCursor after = PageCursor.decode(cursor);
        List<SavedCollection> rows = saved.findPage(userId, after.createdAt(), after.id(), limit + 1);

        return CursorPage.of(rows, limit,
                s -> new PageCursor(s.getCreatedAt(), s.getCollectionId()),
                page -> {
                    List<UUID> ids = page.stream().map(SavedCollection::getCollectionId).toList();
                    Map<UUID, PostCollection> byId = collections.findAllById(ids).stream()
                            .collect(Collectors.toMap(PostCollection::getId, Function.identity()));
                    List<PostCollection> ordered = ids.stream().map(byId::get).filter(Objects::nonNull).toList();
                    return collectionService.withOwners(ordered, userId);
                });
    }
}
