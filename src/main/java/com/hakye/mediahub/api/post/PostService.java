package com.hakye.mediahub.api.post;

import com.hakye.mediahub.api.common.dto.CursorPage;
import com.hakye.mediahub.api.common.exceptions.BadRequestException;
import com.hakye.mediahub.api.common.exceptions.ConflictException;
import com.hakye.mediahub.api.common.exceptions.NotFoundException;
import com.hakye.mediahub.api.follow.FollowId;
import com.hakye.mediahub.api.follow.FollowRepository;
import com.hakye.mediahub.api.media.ObjectStorageService;
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

import java.util.List;
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

    public PostService(PostRepository posts, UploadService uploadService, UserService userService,
                       FollowRepository followRepository, ObjectStorageService storage, TransactionTemplate tx) {
        this.posts = posts;
        this.uploadService = uploadService;
        this.userService = userService;
        this.followRepository = followRepository;
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
        return toResponse(post);
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
        return toResponse(post);
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

        return new PostDetailResponse(
                post.getId(), post.getTitle(), post.getDescription(),
                storage.publicUrl(post.getImageKey()), post.getWidth(), post.getHeight(),
                post.getCreatedAt(), post.getUpdatedAt(),
                ProfileSummaryResponse.of(author, storage.publicUrl(author.getAvatarKey()),
                        isSelf, isFollowing));
    }

    /** Global feed, newest first. */
    @Transactional(readOnly = true)
    public CursorPage<PostResponse> feed(String cursor, int size) {
        int limit = clamp(size);
        PostCursor c = PostCursor.decode(cursor);
        Pageable fetch = PageRequest.of(0, limit + 1); // one extra row tells us if there is a next page
        List<Post> rows = c == null
                ? posts.findAllByOrderByCreatedAtDescIdDesc(fetch)
                : posts.findFeedAfter(c.createdAt(), c.id(), fetch);
        return toPage(rows, limit);
    }

    /** One user's posts, newest first. */
    @Transactional(readOnly = true)
    public CursorPage<PostResponse> byUsername(String username, String cursor, int size) {
        User user = userService.getByUsernameOrThrow(username);
        int limit = clamp(size);
        PostCursor c = PostCursor.decode(cursor);
        Pageable fetch = PageRequest.of(0, limit + 1);
        List<Post> rows = c == null
                ? posts.findByUserIdOrderByCreatedAtDescIdDesc(user.getId(), fetch)
                : posts.findByUserAfter(user.getId(), c.createdAt(), c.id(), fetch);
        return toPage(rows, limit);
    }

    private CursorPage<PostResponse> toPage(List<Post> rows, int limit) {
        boolean hasMore = rows.size() > limit;
        List<Post> page = hasMore ? rows.subList(0, limit) : rows;
        String next = hasMore ? PostCursor.encode(page.get(page.size() - 1)) : null;
        return new CursorPage<>(page.stream().map(this::toResponse).toList(), next);
    }

    private PostResponse toResponse(Post p) {
        return new PostResponse(p.getId(), p.getUserId(), p.getTitle(), p.getDescription(),
                storage.publicUrl(p.getImageKey()), p.getWidth(), p.getHeight(),
                p.getCreatedAt(), p.getUpdatedAt());
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
