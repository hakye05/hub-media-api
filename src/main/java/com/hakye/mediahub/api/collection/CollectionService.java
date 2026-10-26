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
import com.hakye.mediahub.api.common.storage.ObjectStorageService;
import com.hakye.mediahub.api.common.utils.PageCursor;
import com.hakye.mediahub.api.follow.FollowId;
import com.hakye.mediahub.api.follow.FollowRepository;
import com.hakye.mediahub.api.post.Post;
import com.hakye.mediahub.api.post.PostRepository;
import com.hakye.mediahub.api.post.PostService;
import com.hakye.mediahub.api.post.dto.PostResponse;
import com.hakye.mediahub.api.user.User;
import com.hakye.mediahub.api.user.UserService;
import com.hakye.mediahub.api.user.dto.ProfileSummaryResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CollectionService {

    private static final int MAX_PAGE_SIZE = 20;
    private static final String NOT_FOUND = "Collection not found";
    private static final String DUPLICATE_TITLE = "You already have a collection with this title";

    private final PostCollectionRepository collections;
    private final CollectionPostRepository collectionPosts;
    private final SavedPostRepository savedPosts;
    private final PostRepository posts;
    private final PostService postService;
    private final UserService userService;
    private final FollowRepository followRepository;
    private final ObjectStorageService storage;

    public CollectionService(PostCollectionRepository collections, CollectionPostRepository collectionPosts,
                             SavedPostRepository savedPosts, PostRepository posts, PostService postService,
                             UserService userService, FollowRepository followRepository,
                             ObjectStorageService storage) {
        this.collections = collections;
        this.collectionPosts = collectionPosts;
        this.savedPosts = savedPosts;
        this.posts = posts;
        this.postService = postService;
        this.userService = userService;
        this.followRepository = followRepository;
        this.storage = storage;
    }

    @Transactional
    public CollectionResponse create(UUID userId, CreateCollectionRequest req) {
        PostCollection collection = new PostCollection(userId, req.title().trim(),
                Boolean.TRUE.equals(req.isPublic()));
        try {
            collections.saveAndFlush(collection);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException(DUPLICATE_TITLE);
        }
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
        try {
            // Flush so @PreUpdate runs, the unique index is checked, and the response has the new updatedAt.
            collections.saveAndFlush(collection);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException(DUPLICATE_TITLE);
        }
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

        boolean isSelf = viewerId != null && viewerId.equals(author.getId());
        boolean isFollowing = viewerId != null && !isSelf
                && followRepository.existsById(new FollowId(viewerId, author.getId()));

        return new CollectionDetailResponse(
                collection.getId(), collection.getTitle(), collection.isPublic(),
                false, // savedByViewer: reserved for saving other users' collections
                collection.getCreatedAt(), collection.getUpdatedAt(),
                ProfileSummaryResponse.of(author, storage.publicUrl(author.getAvatarKey()),
                        isSelf, isFollowing));
    }

    /** A user's collections, newest first. Owners see all of theirs; everyone else sees public ones. */
    @Transactional(readOnly = true)
    public CursorPage<CollectionResponse> byUsername(String username, String cursor, int size, UUID viewerId) {
        User owner = userService.getByUsernameOrThrow(username);
        boolean includePrivate = owner.getId().equals(viewerId);

        int limit = Math.clamp(size, 1, MAX_PAGE_SIZE);
        PageCursor c = PageCursor.decode(cursor);
        Pageable fetch = PageRequest.of(0, limit + 1); // one extra row tells us if there is a next page

        List<PostCollection> rows = c == null
                ? collections.findFirstPage(owner.getId(), includePrivate, fetch)
                : collections.findPageAfter(owner.getId(), includePrivate, c.createdAt(), c.id(), fetch);

        boolean hasMore = rows.size() > limit;
        List<PostCollection> page = hasMore ? rows.subList(0, limit) : rows;

        String next = null;
        if (hasMore) {
            PostCollection last = page.get(page.size() - 1);
            next = PageCursor.encode(last.getCreatedAt(), last.getId());
        }
        Map<UUID, CollectionStats> stats = statsFor(page.stream().map(PostCollection::getId).toList());
        return new CursorPage<>(
                page.stream()
                        .map(p -> toResponse(p, stats.getOrDefault(p.getId(), CollectionStats.EMPTY)))
                        .toList(),
                next);
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

        int limit = Math.clamp(size, 1, MAX_PAGE_SIZE);
        PageCursor c = PageCursor.decode(cursor);
        Pageable fetch = PageRequest.of(0, limit + 1);

        List<CollectionPost> rows = c == null
                ? collectionPosts.findFirstPage(collectionId, fetch)
                : collectionPosts.findPageAfter(collectionId, c.createdAt(), c.id(), fetch);

        boolean hasMore = rows.size() > limit;
        List<CollectionPost> page = hasMore ? rows.subList(0, limit) : rows;

        // One batched lookup, then restore the "added at" order.
        Map<UUID, Post> byId = posts.findAllById(page.stream().map(CollectionPost::getPostId).toList())
                .stream().collect(Collectors.toMap(Post::getId, Function.identity()));
        List<Post> ordered = page.stream()
                .map(cp -> byId.get(cp.getPostId()))
                .filter(Objects::nonNull)
                .toList();

        String next = null;
        if (hasMore) {
            CollectionPost last = page.get(page.size() - 1);
            next = PageCursor.encode(last.getCreatedAt(), last.getPostId());
        }
        return new CursorPage<>(postService.toResponses(ordered, viewerId), next);
    }

    /** Only the owner can add, and only posts they have saved. Idempotent. */
    @Transactional
    public void addPost(UUID userId, UUID collectionId, UUID postId) {
        collections.findByIdAndUserId(collectionId, userId)
                .orElseThrow(() -> new NotFoundException(NOT_FOUND));
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
        collections.findByIdAndUserId(collectionId, userId)
                .orElseThrow(() -> new NotFoundException(NOT_FOUND));
        collectionPosts.remove(collectionId, postId);
    }

    private PostCollection visibleCollection(UUID collectionId, UUID viewerId) {
        PostCollection collection = collections.findById(collectionId)
                .orElseThrow(() -> new NotFoundException(NOT_FOUND));
        if (!collection.isVisibleTo(viewerId)) {
            throw new NotFoundException(NOT_FOUND); // don't reveal that a private collection exists
        }
        return collection;
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
        return new CollectionResponse(c.getId(), c.getUserId(), c.getTitle(), c.isPublic(),
                false, // savedByViewer: reserved for saving other users' collections
                stats.postCount(),
                stats.coverImageKey() == null ? null : storage.publicUrl(stats.coverImageKey()),
                c.getCreatedAt(), c.getUpdatedAt());
    }
}
