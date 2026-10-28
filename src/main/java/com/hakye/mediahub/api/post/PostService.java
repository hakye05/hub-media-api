package com.hakye.mediahub.api.post;

import com.hakye.mediahub.api.common.dto.CursorPage;
import com.hakye.mediahub.api.common.exceptions.BadRequestException;
import com.hakye.mediahub.api.common.exceptions.ConflictException;
import com.hakye.mediahub.api.common.exceptions.NotFoundException;
import com.hakye.mediahub.api.common.utils.PageCursor;
import com.hakye.mediahub.api.post.domain.Post;
import com.hakye.mediahub.api.post.repository.PostRepository;
import com.hakye.mediahub.api.post.repository.SavedPostLookup;
import com.hakye.mediahub.api.storage.ObjectStorageService;
import com.hakye.mediahub.api.upload.domain.Upload;
import com.hakye.mediahub.api.upload.domain.UploadKind;
import com.hakye.mediahub.api.upload.UploadService;
import com.hakye.mediahub.api.post.dto.CreatePostRequest;
import com.hakye.mediahub.api.post.dto.PostDetailResponse;
import com.hakye.mediahub.api.post.dto.PostResponse;
import com.hakye.mediahub.api.post.dto.UpdatePostRequest;
import com.hakye.mediahub.api.user.ProfileSummaryMapper;
import com.hakye.mediahub.api.user.domain.User;
import com.hakye.mediahub.api.user.UserService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.UUID;

@Service
public class PostService {

    private static final int MAX_QUERY_LENGTH = 100;

    private final PostRepository posts;
    private final PostMapper mapper;
    private final UploadService uploadService;
    private final UserService userService;
    private final ProfileSummaryMapper profileMapper;
    private final SavedPostLookup savedPosts;
    private final ObjectStorageService storage;
    private final TransactionTemplate tx;

    public PostService(PostRepository posts, PostMapper mapper, UploadService uploadService,
                       UserService userService, ProfileSummaryMapper profileMapper,
                       SavedPostLookup savedPosts, ObjectStorageService storage, TransactionTemplate tx) {
        this.posts = posts;
        this.mapper = mapper;
        this.uploadService = uploadService;
        this.userService = userService;
        this.profileMapper = profileMapper;
        this.savedPosts = savedPosts;
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
        return mapper.toResponse(post, false);
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
        return mapper.toResponse(post, isSavedBy(userId, postId));
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

    /** The post plus its author. */
    @Transactional(readOnly = true)
    public PostDetailResponse getDetail(UUID postId, UUID viewerId) {
        Post post = posts.findById(postId)
                .orElseThrow(() -> new NotFoundException("Post not found"));
        User author = userService.getById(post.getUserId());
        return mapper.toDetail(post, profileMapper.toSummary(author, viewerId), isSavedBy(viewerId, postId));
    }

    /** Global feed, newest first. */
    @Transactional(readOnly = true)
    public CursorPage<PostResponse> feed(String cursor, int size, UUID viewerId) {
        int limit = PageCursor.clampSize(size);
        PageCursor after = PageCursor.decode(cursor);
        return page(posts.findFeed(after.createdAt(), after.id(), limit + 1), limit, viewerId);
    }

    /** One user's posts, newest first. */
    @Transactional(readOnly = true)
    public CursorPage<PostResponse> byUsername(String username, String cursor, int size, UUID viewerId) {
        User user = userService.getByUsername(username);
        int limit = PageCursor.clampSize(size);
        PageCursor after = PageCursor.decode(cursor);
        return page(posts.findByUser(user.getId(), after.createdAt(), after.id(), limit + 1), limit, viewerId);
    }

    /** Posts from users the viewer follows, newest first. Signed-in users only. */
    @Transactional(readOnly = true)
    public CursorPage<PostResponse> following(UUID viewerId, String cursor, int size) {
        int limit = PageCursor.clampSize(size);
        PageCursor after = PageCursor.decode(cursor);
        return page(posts.findFollowingFeed(viewerId, after.createdAt(), after.id(), limit + 1), limit, viewerId);
    }

    /** Case-insensitive substring match on title or description, newest first. */
    @Transactional(readOnly = true)
    public CursorPage<PostResponse> search(String query, String cursor, int size, UUID viewerId) {
        String q = query == null ? "" : query.trim();
        if (q.isEmpty()) {
            throw new BadRequestException("Search query cannot be blank");
        }
        if (q.length() > MAX_QUERY_LENGTH) {
            throw new BadRequestException("Search query is too long");
        }
        int limit = PageCursor.clampSize(size);
        PageCursor after = PageCursor.decode(cursor);
        return page(posts.search(likePattern(q), after.createdAt(), after.id(), limit + 1), limit, viewerId);
    }

    private CursorPage<PostResponse> page(List<Post> fetched, int limit, UUID viewerId) {
        return CursorPage.of(fetched, limit,
                p -> new PageCursor(p.getCreatedAt(), p.getId()),
                rows -> mapper.toResponses(rows, viewerId));
    }

    private boolean isSavedBy(UUID userId, UUID postId) {
        return savedPosts.savedAmong(userId, List.of(postId)).contains(postId);
    }

    /** Escapes LIKE wildcards so user input like "100%" or "a_b" matches literally. */
    private static String likePattern(String q) {
        String escaped = q.replace("!", "!!").replace("%", "!%").replace("_", "!_");
        return "%" + escaped + "%";
    }

    private static String blankToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
