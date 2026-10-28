package com.hakye.mediahub.api.collection;

import com.hakye.mediahub.api.collection.domain.CollectionPost;
import com.hakye.mediahub.api.collection.domain.PostCollection;
import com.hakye.mediahub.api.collection.domain.SavedPostId;
import com.hakye.mediahub.api.collection.dto.*;
import com.hakye.mediahub.api.collection.repository.CollectionPostRepository;
import com.hakye.mediahub.api.collection.repository.PostCollectionRepository;
import com.hakye.mediahub.api.collection.repository.SavedPostRepository;
import com.hakye.mediahub.api.common.dto.CursorPage;
import com.hakye.mediahub.api.common.exceptions.BadRequestException;
import com.hakye.mediahub.api.common.exceptions.ConflictException;
import com.hakye.mediahub.api.common.exceptions.NotFoundException;
import com.hakye.mediahub.api.post.PostMapper;
import com.hakye.mediahub.api.storage.ObjectStorageService;
import com.hakye.mediahub.api.common.utils.PageCursor;
import com.hakye.mediahub.api.post.repository.PostRepository;
import com.hakye.mediahub.api.post.dto.PostResponse;
import com.hakye.mediahub.api.user.ProfileSummaryMapper;
import com.hakye.mediahub.api.user.domain.User;
import com.hakye.mediahub.api.user.UserService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class CollectionService {

    private static final String NOT_FOUND = "Collection not found";
    private static final String DUPLICATE_TITLE = "You already have a collection with this title";

    /** Reserved for saving other users' collections; always false until that feature exists. */
    private static final boolean SAVED_BY_VIEWER = false;

    private final PostCollectionRepository collections;
    private final CollectionPostRepository collectionPosts;
    private final SavedPostRepository savedPosts;
    private final PostRepository posts;
    private final PostMapper postMapper;
    private final UserService userService;
    private final ProfileSummaryMapper profileMapper;
    private final ObjectStorageService storage;

    public CollectionService(PostCollectionRepository collections, CollectionPostRepository collectionPosts,
                             SavedPostRepository savedPosts, PostRepository posts, PostMapper postMapper,
                             UserService userService, ProfileSummaryMapper profileMapper,
                             ObjectStorageService storage) {
        this.collections = collections;
        this.collectionPosts = collectionPosts;
        this.savedPosts = savedPosts;
        this.posts = posts;
        this.postMapper = postMapper;
        this.userService = userService;
        this.profileMapper = profileMapper;
        this.storage = storage;
    }

    @Transactional
    public CollectionResponse create(UUID userId, CreateCollectionRequest req) {
        PostCollection collection = new PostCollection(userId, req.title().trim(),
                Boolean.TRUE.equals(req.isPublic()));
        flushOrConflict(() -> collections.saveAndFlush(collection));
        return toResponse(collection, CollectionStats.EMPTY);
    }

    @Transactional
    public CollectionResponse update(UUID userId, UUID collectionId, UpdateCollectionRequest req) {
        PostCollection collection = collections.findByIdAndUserId(collectionId, userId)
                .orElseThrow(() -> new NotFoundException(NOT_FOUND));

        if (req.title() != null) {
            String title = req.title().trim();
            if (title.isEmpty()) {
                throw new BadRequestException("Title cannot be blank");
            }
            collection.setTitle(title);
        }
        if (req.isPublic() != null) {
            collection.setPublic(req.isPublic());
        }
        // Flush so @PreUpdate runs, the unique index is checked, and the response has the new updatedAt.
        flushOrConflict(() -> collections.saveAndFlush(collection));
        CollectionStats stats = statsFor(List.of(collection.getId()))
                .getOrDefault(collection.getId(), CollectionStats.EMPTY);
        return toResponse(collection, stats);
    }

    /** Posts stay saved; only the collection and its memberships go (FK cascade). */
    @Transactional
    public void delete(UUID userId, UUID collectionId) {
        if (collections.deleteOwned(collectionId, userId) == 0) {
            throw new NotFoundException(NOT_FOUND);
        }
    }

    /** One collection plus its author. Private collections look nonexistent to everyone but the owner. */
    @Transactional(readOnly = true)
    public CollectionDetailResponse getDetail(UUID collectionId, UUID viewerId) {
        PostCollection collection = visibleCollection(collectionId, viewerId);
        User author = userService.getById(collection.getUserId());

        return new CollectionDetailResponse(
                collection.getId(), collection.getTitle(), collection.isPublic(), SAVED_BY_VIEWER,
                collection.getCreatedAt(), collection.getUpdatedAt(),
                profileMapper.toSummary(author, viewerId));
    }

    /** A user's collections, newest first. Owners see all of theirs; everyone else sees public ones. */
    @Transactional(readOnly = true)
    public CursorPage<CollectionResponse> byUsername(String username, String cursor, int size, UUID viewerId) {
        User owner = userService.getByUsername(username);
        boolean includePrivate = owner.getId().equals(viewerId);

        int limit = PageCursor.clampSize(size);
        PageCursor after = PageCursor.decode(cursor);
        List<PostCollection> rows =
                collections.findPage(owner.getId(), includePrivate, after.createdAt(), after.id(), limit + 1);

        return CursorPage.of(rows, limit,
                c -> new PageCursor(c.getCreatedAt(), c.getId()),
                page -> {
                    Map<UUID, CollectionStats> stats = statsFor(page.stream().map(PostCollection::getId).toList());
                    return page.stream()
                            .map(c -> toResponse(c, stats.getOrDefault(c.getId(), CollectionStats.EMPTY)))
                            .toList();
                });
    }

    /** The caller's collections that contain this post, most recently added first. */
    @Transactional(readOnly = true)
    public List<CollectionSummaryResponse> containingPost(UUID userId, UUID postId) {
        return collections.findContainingPost(userId, postId).stream()
                .map(c -> new CollectionSummaryResponse(c.getId(), c.getTitle(), c.isPublic()))
                .toList();
    }

    /** Posts in a collection, most recently added first. */
    @Transactional(readOnly = true)
    public CursorPage<PostResponse> listPosts(UUID collectionId, String cursor, int size, UUID viewerId) {
        visibleCollection(collectionId, viewerId);

        int limit = PageCursor.clampSize(size);
        PageCursor after = PageCursor.decode(cursor);
        List<CollectionPost> rows = collectionPosts.findPage(collectionId, after.createdAt(), after.id(), limit + 1);

        return CursorPage.of(rows, limit,
                cp -> new PageCursor(cp.getCreatedAt(), cp.getPostId()),
                page -> postMapper.toResponses(
                        posts.findAllInOrder(page.stream().map(CollectionPost::getPostId).toList()), viewerId));
    }

    /** Only the owner can add, and only posts they have saved. Idempotent. */
    @Transactional
    public void addPost(UUID userId, UUID collectionId, UUID postId) {
        requireOwned(userId, collectionId);
        if (!savedPosts.existsById(new SavedPostId(userId, postId))) {
            throw new ConflictException("Post is not saved. Save it before adding it to a collection");
        }
        try {
            collectionPosts.addIfAbsent(collectionId, userId, postId);
        } catch (DataIntegrityViolationException e) {
            // collection deleted or post unsaved between the checks and the insert
            throw new ConflictException("Collection or saved post no longer exists. Try again");
        }
    }

    /** Idempotent: removing a post that isn't in the collection is fine. */
    @Transactional
    public void removePost(UUID userId, UUID collectionId, UUID postId) {
        requireOwned(userId, collectionId);
        collectionPosts.remove(collectionId, postId);
    }

    private void requireOwned(UUID userId, UUID collectionId) {
        collections.findByIdAndUserId(collectionId, userId)
                .orElseThrow(() -> new NotFoundException(NOT_FOUND));
    }

    private PostCollection visibleCollection(UUID collectionId, UUID viewerId) {
        PostCollection collection = collections.findById(collectionId)
                .orElseThrow(() -> new NotFoundException(NOT_FOUND));
        if (!collection.isVisibleTo(viewerId)) {
            throw new NotFoundException(NOT_FOUND); // don't reveal that a private collection exists
        }
        return collection;
    }

    /** Runs a write whose unique index (title per user) may reject it, turning that into a 409. */
    private static void flushOrConflict(Runnable write) {
        try {
            write.run();
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException(DUPLICATE_TITLE);
        }
    }

    private record CollectionStats(long postCount, String coverImageKey) {
        static final CollectionStats EMPTY = new CollectionStats(0, null);
    }

    /** One grouped query for a whole page. Collections without posts are simply absent from the map. */
    private Map<UUID, CollectionStats> statsFor(List<UUID> collectionIds) {
        if (collectionIds.isEmpty()) return Map.of();
        Map<UUID, CollectionStats> stats = new HashMap<>();
        for (Object[] row : collectionPosts.findStats(collectionIds)) {
            stats.put((UUID) row[0], new CollectionStats(((Number) row[1]).longValue(), (String) row[2]));
        }
        return stats;
    }

    private CollectionResponse toResponse(PostCollection c, CollectionStats stats) {
        return new CollectionResponse(c.getId(), c.getUserId(), c.getTitle(), c.isPublic(), SAVED_BY_VIEWER,
                stats.postCount(), storage.publicUrl(stats.coverImageKey()),
                c.getCreatedAt(), c.getUpdatedAt());
    }
}
