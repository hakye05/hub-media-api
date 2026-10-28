package com.hakye.mediahub.api.collection;

import com.hakye.mediahub.api.common.dto.CursorPage;
import com.hakye.mediahub.api.common.utils.CurrentUser;
import com.hakye.mediahub.api.common.utils.PageCursor;
import com.hakye.mediahub.api.post.dto.PostResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api")
public class SavedPostController {

    private final SavedPostService savedPostService;

    public SavedPostController(SavedPostService savedPostService) {
        this.savedPostService = savedPostService;
    }

    @PutMapping("/posts/{id}/save")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void save(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        savedPostService.save(CurrentUser.requireId(jwt), id);
    }

    @DeleteMapping("/posts/{id}/save")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unsave(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        savedPostService.unsave(CurrentUser.requireId(jwt), id);
    }

    @GetMapping("/saved-posts")
    public CursorPage<PostResponse> list(@AuthenticationPrincipal Jwt jwt,
                                         @RequestParam(required = false) String cursor,
                                         @RequestParam(defaultValue = PageCursor.DEFAULT_SIZE_PARAM) int size) {
        return savedPostService.list(CurrentUser.requireId(jwt), cursor, size);
    }
}
