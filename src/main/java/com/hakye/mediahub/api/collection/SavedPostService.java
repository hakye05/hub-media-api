package com.hakye.mediahub.api.collection;

import com.hakye.mediahub.api.common.dto.CursorPage;
import com.hakye.mediahub.api.common.exceptions.NotFoundException;
import com.hakye.mediahub.api.common.utils.PageCursor;
import com.hakye.mediahub.api.post.Post;
import com.hakye.mediahub.api.post.PostRepository;
import com.hakye.mediahub.api.post.PostService;
import com.hakye.mediahub.api.post.dto.PostResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class SavedPostService {

    private static final int MAX_PAGE_SIZE = 20;

    private final SavedPostRepository saved;
    private final PostRepository posts;
    private final PostService postService;

    public SavedPostService(SavedPostRepository saved, PostRepository posts, PostService postService) {
        this.saved = saved;
        this.posts = posts;
        this.postService = postService;
    }

    @Transactional
    public void save(UUID userId, UUID postId) {
        if (!posts.existsById(postId)) {
            throw new NotFoundException("Post not found");
        }
        try {
            saved.saveIfAbsent(userId, postId);
        } catch (DataIntegrityViolationException e) {
            // post was deleted between the check and the insert (FK violation)
            throw new NotFoundException("Post not found");
        }
    }

    /** Idempotent: unsaving something that isn't saved is fine. */
    @Transactional
    public void unsave(UUID userId, UUID postId) {
        saved.unsave(userId, postId);
    }

    /** The caller's saved posts, most recently saved first. */
    @Transactional(readOnly = true)
    public CursorPage<PostResponse> list(UUID userId, String cursor, int size) {
        int limit = Math.clamp(size, 1, MAX_PAGE_SIZE);
        PageCursor c = PageCursor.decode(cursor);
        Pageable fetch = PageRequest.of(0, limit + 1); // one extra row tells us if there is a next page

        List<SavedPost> rows = c == null
                ? saved.findFirstPage(userId, fetch)
                : saved.findPageAfter(userId, c.createdAt(), c.id(), fetch);

        boolean hasMore = rows.size() > limit;
        List<SavedPost> page = hasMore ? rows.subList(0, limit) : rows;

        // One batched lookup, then restore the "saved at" order.
        Map<UUID, Post> byId = posts.findAllById(page.stream().map(SavedPost::getPostId).toList())
                .stream().collect(Collectors.toMap(Post::getId, Function.identity()));

        List<PostResponse> items = page.stream()
                .map(s -> byId.get(s.getPostId()))
                .filter(Objects::nonNull)
                .map(p -> postService.toResponse(p, true))
                .toList();

        String next = null;
        if (hasMore) {
            SavedPost last = page.get(page.size() - 1);
            next = PageCursor.encode(last.getCreatedAt(), last.getPostId());
        }
        return new CursorPage<>(items, next);
    }
}
