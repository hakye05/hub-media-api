package com.hakye.mediahub.api.collection;

import com.hakye.mediahub.api.collection.dto.CollectionDetailResponse;
import com.hakye.mediahub.api.collection.dto.CollectionResponse;
import com.hakye.mediahub.api.collection.dto.CreateCollectionRequest;
import com.hakye.mediahub.api.collection.dto.UpdateCollectionRequest;
import com.hakye.mediahub.api.common.dto.CursorPage;
import com.hakye.mediahub.api.common.utils.CurrentUser;
import com.hakye.mediahub.api.post.dto.PostResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("api")
public class CollectionController {

    private final CollectionService collectionService;

    public CollectionController(CollectionService collectionService) {
        this.collectionService = collectionService;
    }

    @PostMapping("/collections")
    @ResponseStatus(HttpStatus.CREATED)
    public CollectionResponse create(@AuthenticationPrincipal Jwt jwt,
                                     @Valid @RequestBody CreateCollectionRequest request) {
        return collectionService.create(CurrentUser.requireId(jwt), request);
    }

    @GetMapping("/collections/{id}")
    public CollectionDetailResponse get(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        return collectionService.getDetail(id, CurrentUser.optionalId(jwt));
    }

    @PatchMapping("/collections/{id}")
    public CollectionResponse update(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt,
                                     @Valid @RequestBody UpdateCollectionRequest request) {
        return collectionService.update(CurrentUser.requireId(jwt), id, request);
    }

    @DeleteMapping("/collections/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        collectionService.delete(CurrentUser.requireId(jwt), id);
    }

    @GetMapping("/users/{username}/collections")
    public CursorPage<CollectionResponse> userCollections(@PathVariable String username,
                                                          @RequestParam(required = false) String cursor,
                                                          @RequestParam(defaultValue = "20") int size,
                                                          @AuthenticationPrincipal Jwt jwt) {
        return collectionService.byUsername(username, cursor, size, CurrentUser.optionalId(jwt));
    }

    @GetMapping("/collections/{id}/posts")
    public CursorPage<PostResponse> posts(@PathVariable UUID id,
                                          @RequestParam(required = false) String cursor,
                                          @RequestParam(defaultValue = "20") int size,
                                          @AuthenticationPrincipal Jwt jwt) {
        return collectionService.listPosts(id, cursor, size, CurrentUser.optionalId(jwt));
    }

    @PutMapping("/collections/{id}/posts/{postId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void addPost(@PathVariable UUID id, @PathVariable UUID postId, @AuthenticationPrincipal Jwt jwt) {
        collectionService.addPost(CurrentUser.requireId(jwt), id, postId);
    }

    @DeleteMapping("/collections/{id}/posts/{postId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removePost(@PathVariable UUID id, @PathVariable UUID postId, @AuthenticationPrincipal Jwt jwt) {
        collectionService.removePost(CurrentUser.requireId(jwt), id, postId);
    }
}
