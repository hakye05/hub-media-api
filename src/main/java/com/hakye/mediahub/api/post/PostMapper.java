package com.hakye.mediahub.api.post;

import com.hakye.mediahub.api.post.domain.Post;
import com.hakye.mediahub.api.post.dto.PostDetailResponse;
import com.hakye.mediahub.api.post.dto.PostResponse;
import com.hakye.mediahub.api.post.repository.SavedPostLookup;
import com.hakye.mediahub.api.storage.ObjectStorageService;
import com.hakye.mediahub.api.user.dto.ProfileSummaryResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
public class PostMapper {

    private final ObjectStorageService storage;
    private final SavedPostLookup savedPosts;

    public PostMapper(ObjectStorageService storage, SavedPostLookup savedPosts) {
        this.storage = storage;
        this.savedPosts = savedPosts;
    }

    public PostResponse toResponse(Post p, boolean savedByViewer) {
        return new PostResponse(p.getId(), p.getUserId(), p.getTitle(), p.getDescription(),
                storage.publicUrl(p.getImageKey()), p.getWidth(), p.getHeight(),
                p.getCreatedAt(), p.getUpdatedAt(), savedByViewer);
    }

    /** Resolves the viewer's saved flags for the whole page in a single query. viewerId is null for guests. */
    public List<PostResponse> toResponses(List<Post> posts, UUID viewerId) {
        Set<UUID> saved = savedPosts.savedAmong(viewerId, posts.stream().map(Post::getId).toList());
        return posts.stream().map(p -> toResponse(p, saved.contains(p.getId()))).toList();
    }

    public PostDetailResponse toDetail(Post p, ProfileSummaryResponse author, boolean savedByViewer) {
        return new PostDetailResponse(p.getId(), p.getTitle(), p.getDescription(),
                storage.publicUrl(p.getImageKey()), p.getWidth(), p.getHeight(),
                p.getCreatedAt(), p.getUpdatedAt(), author, savedByViewer);
    }
}
