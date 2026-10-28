package com.hakye.mediahub.api.collection;

import com.hakye.mediahub.api.collection.domain.SavedPost;
import com.hakye.mediahub.api.collection.repository.SavedPostRepository;
import com.hakye.mediahub.api.common.dto.CursorPage;
import com.hakye.mediahub.api.common.exceptions.NotFoundException;
import com.hakye.mediahub.api.common.utils.PageCursor;
import com.hakye.mediahub.api.post.PostMapper;
import com.hakye.mediahub.api.post.domain.Post;
import com.hakye.mediahub.api.post.repository.PostRepository;
import com.hakye.mediahub.api.post.dto.PostResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Saved posts are private: every method is scoped to the caller's own user id. */
@Service
public class SavedPostService {

    private final SavedPostRepository saved;
    private final PostRepository posts;
    private final PostMapper postMapper;

    public SavedPostService(SavedPostRepository saved, PostRepository posts, PostMapper postMapper) {
        this.saved = saved;
        this.posts = posts;
        this.postMapper = postMapper;
    }

    /** Idempotent. */
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

    /** Idempotent: unsaving something that isn't saved is fine. Also drops the post from the user's collections. */
    @Transactional
    public void unsave(UUID userId, UUID postId) {
        saved.unsave(userId, postId);
    }

    /** The caller's saved posts, most recently saved first. */
    @Transactional(readOnly = true)
    public CursorPage<PostResponse> list(UUID userId, String cursor, int size) {
        int limit = PageCursor.clampSize(size);
        PageCursor after = PageCursor.decode(cursor);
        List<SavedPost> rows = saved.findPage(userId, after.createdAt(), after.id(), limit + 1);

        return CursorPage.of(rows, limit,
                s -> new PageCursor(s.getCreatedAt(), s.getPostId()),
                page -> {
                    List<Post> ordered = posts.findAllInOrder(page.stream().map(SavedPost::getPostId).toList());
                    return ordered.stream().map(p -> postMapper.toResponse(p, true)).toList();
                });
    }
}
