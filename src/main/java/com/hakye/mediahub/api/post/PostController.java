package com.hakye.mediahub.api.post;

import com.hakye.mediahub.api.common.dto.CursorPage;
import com.hakye.mediahub.api.common.utils.CurrentUser;
import com.hakye.mediahub.api.post.dto.CreatePostRequest;
import com.hakye.mediahub.api.post.dto.PostDetailResponse;
import com.hakye.mediahub.api.post.dto.PostResponse;
import com.hakye.mediahub.api.post.dto.UpdatePostRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("api")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @PostMapping("/posts")
    @ResponseStatus(HttpStatus.CREATED)
    public PostResponse create(@AuthenticationPrincipal Jwt jwt,
                               @Valid @RequestBody CreatePostRequest request) {
        return postService.create(CurrentUser.requireId(jwt), request);
    }

    @GetMapping("/posts")
    public CursorPage<PostResponse> feed(@RequestParam(required = false) String cursor,
                                         @RequestParam(defaultValue = "20") int size,
                                         @AuthenticationPrincipal Jwt jwt) {
        return postService.feed(cursor, size, CurrentUser.optionalId(jwt));
    }

    @GetMapping("/posts/{id}")
    public PostDetailResponse get(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        return postService.getDetail(id, CurrentUser.optionalId(jwt));
    }

    @PatchMapping("/posts/{id}")
    public PostResponse update(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt,
                               @Valid @RequestBody UpdatePostRequest request) {
        return postService.update(CurrentUser.requireId(jwt), id, request);
    }

    @DeleteMapping("/posts/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        postService.delete(CurrentUser.requireId(jwt), id);
    }

    @GetMapping("/users/{username}/posts")
    public CursorPage<PostResponse> userPosts(@PathVariable String username,
                                              @RequestParam(required = false) String cursor,
                                              @RequestParam(defaultValue = "20") int size,
                                              @AuthenticationPrincipal Jwt jwt) {
        return postService.byUsername(username, cursor, size, CurrentUser.optionalId(jwt));
    }
}
