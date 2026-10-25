package com.hakye.mediahub.api.post;

import com.hakye.mediahub.api.collection.domain.SavedPostId;
import com.hakye.mediahub.api.collection.repository.SavedPostRepository;
import com.hakye.mediahub.api.common.dto.CursorPage;
import com.hakye.mediahub.api.common.exceptions.BadRequestException;
import com.hakye.mediahub.api.common.exceptions.ConflictException;
import com.hakye.mediahub.api.common.exceptions.NotFoundException;
import com.hakye.mediahub.api.common.utils.PageCursor;
import com.hakye.mediahub.api.follow.FollowId;
import com.hakye.mediahub.api.follow.FollowRepository;
import com.hakye.mediahub.api.common.storage.ObjectStorageService;
import com.hakye.mediahub.api.media.Upload;
import com.hakye.mediahub.api.media.UploadKind;
import com.hakye.mediahub.api.media.UploadService;
import com.hakye.mediahub.api.post.dto.CreatePostRequest;
import com.hakye.mediahub.api.post.dto.PostDetailResponse;
import com.hakye.mediahub.api.post.dto.PostResponse;
import com.hakye.mediahub.api.post.dto.UpdatePostRequest;
import com.hakye.mediahub.api.user.User;
import com.hakye.mediahub.api.user.UserService;
import com.hakye.mediahub.api.user.dto.ProfileSummaryResponse;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class PostService {

    private static final int MAX_PAGE_SIZE = 50;

    private final PostRepository posts;
    private final UploadService uploadService;
    private final UserService userService;
    private final FollowRepository followRepository;
    private final ObjectStorageService storage;
    private final TransactionTemplate tx;
    private final SavedPostRepository savedPostRepository;

    public PostService(PostRepository posts, UploadService uploadService, UserService userService,
                       FollowRepository followRepository, SavedPostRepository savedPostRepository,
                       ObjectStorageService storage, TransactionTemplate tx) {
        this.posts = posts;
        this.uploadService = uploadService;
        this.userService = userService;
        this.followRepository = followRepository;
        this.savedPostRepository = savedPostRepository;
        this.storage = storage;
        this.tx = tx;
    }

    @Transactional
    public PostResponse create(UUID userId, CreatePostRequest req) {
        Upload upload = uploadService.getFinalizedUpload(userId, req.uploadId(), UploadKind.POST);
        Post post = new Post(userId, req.title().trim(), blankToNull(req.description()),
                upload.getMediaKey(), upload.getWidth(), upload.getHeight());
        try {
            posts.saveAndFlush(post);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("This upload is already attached to a post");
        }
        return toResponse(post, false);
    }

    @Transactional
    public PostResponse update(UUID userId, UUID postId, UpdatePostRequest req) {
        Post post = posts.findByIdAndUserId(postId, userId)
                .orElseThrow(() -> new NotFoundException("Post not found"));

        if (req.title() != null) {
            String title = req.title().trim();
            if (title.isEmpty()) {
                throw new BadRequestException("Title cannot be blank");
            }
            post.setTitle(title);
        }
        if (req.description() != null) {
            post.setDescription(blankToNull(req.description()));
        }
        // Flush so @PreUpdate runs and the response carries the new updatedAt.
        posts.saveAndFlush(post);
        boolean saved = savedPostRepository.existsById(new SavedPostId(userId, postId));
        return toResponse(post, saved);
    }

    /** DB row in a short transaction, R2 object after commit. */
    public void delete(UUID userId, UUID postId) {
        String imageKey = tx.execute(status -> {
            Post post = posts.findByIdAndUserId(postId, userId)
                    .orElseThrow(() -> new NotFoundException("Post not found"));
            posts.delete(post);
            return post.getImageKey();
        });
        storage.deleteQuietly(imageKey);
    }

    /** Posts page: the post plus its author. */
    @Transactional(readOnly = true)
    public PostDetailResponse getDetail(UUID postId, UUID viewerId) {
        Post post = posts.findById(postId)
                .orElseThrow(() -> new NotFoundException("Post not found"));
        User author = userService.getById(post.getUserId());

        boolean isSelf = viewerId != null && viewerId.equals(author.getId());
        boolean isFollowing = viewerId != null && !isSelf
                && followRepository.existsById(new FollowId(viewerId, author.getId()));
        boolean savedByViewer = viewerId != null
                && savedPostRepository.existsById(new SavedPostId(viewerId, postId));

        return new PostDetailResponse(
                post.getId(), post.getTitle(), post.getDescription(),
                storage.publicUrl(post.getImageKey()), post.getWidth(), post.getHeight(),
                post.getCreatedAt(), post.getUpdatedAt(),
                ProfileSummaryResponse.of(author, storage.publicUrl(author.getAvatarKey()),
                        isSelf, isFollowing),
                savedByViewer);
    }

    /** Global feed, newest first. */
    @Transactional(readOnly = true)
    public CursorPage<PostResponse> feed(String cursor, int size, UUID viewerId) {
        int limit = clamp(size);
        PageCursor c = PageCursor.decode(cursor);
        Pageable fetch = PageRequest.of(0, limit + 1);
        List<Post> rows = c == null
                ? posts.findAllByOrderByCreatedAtDescIdDesc(fetch)
                : posts.findFeedAfter(c.createdAt(), c.id(), fetch);
        return toPage(rows, limit, viewerId);
    }

    /** One user's posts, newest first. */
    @Transactional(readOnly = true)
    public CursorPage<PostResponse> byUsername(String username, String cursor, int size, UUID viewerId) {
        User user = userService.getByUsernameOrThrow(username);
        int limit = clamp(size);
        PageCursor c = PageCursor.decode(cursor);
        Pageable fetch = PageRequest.of(0, limit + 1);
        List<Post> rows = c == null
                ? posts.findByUserIdOrderByCreatedAtDescIdDesc(user.getId(), fetch)
                : posts.findByUserAfter(user.getId(), c.createdAt(), c.id(), fetch);
        return toPage(rows, limit, viewerId);
    }

    private CursorPage<PostResponse> toPage(List<Post> rows, int limit, UUID viewerId) {
        boolean hasMore = rows.size() > limit;
        List<Post> page = hasMore ? rows.subList(0, limit) : rows;
        String next = hasMore ? PageCursor.encode(page.get(page.size() - 1)) : null;
        return new CursorPage<>(toResponses(page, viewerId), next);
    }

    /** Guests and empty pages skip the query entirely. */
    private Set<UUID> savedIds(UUID viewerId, List<Post> page) {
        if (viewerId == null || page.isEmpty()) return Set.of();
        return new HashSet<>(savedPostRepository.findSavedPostIds(
                viewerId, page.stream().map(Post::getId).toList()));
    }

    public PostResponse toResponse(Post p, boolean savedByViewer) {
        return new PostResponse(p.getId(), p.getUserId(), p.getTitle(), p.getDescription(),
                storage.publicUrl(p.getImageKey()), p.getWidth(), p.getHeight(),
                p.getCreatedAt(), p.getUpdatedAt(), savedByViewer);
    }

    /** Maps posts to responses, resolving the viewer's saved flags in a single query. */
    public List<PostResponse> toResponses(List<Post> page, UUID viewerId) {
        Set<UUID> saved = savedIds(viewerId, page);
        return page.stream().map(p -> toResponse(p, saved.contains(p.getId()))).toList();
    }

    private static int clamp(int size) {
        return Math.clamp(size, 1, MAX_PAGE_SIZE);
    }

    private static String blankToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
