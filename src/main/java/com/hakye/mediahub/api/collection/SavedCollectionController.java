package com.hakye.mediahub.api.collection;

import com.hakye.mediahub.api.collection.dto.CollectionSearchResponse;
import com.hakye.mediahub.api.common.dto.CursorPage;
import com.hakye.mediahub.api.common.utils.CurrentUser;
import com.hakye.mediahub.api.common.utils.PageCursor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api")
public class SavedCollectionController {

    private final SavedCollectionService savedCollectionService;

    public SavedCollectionController(SavedCollectionService savedCollectionService) {
        this.savedCollectionService = savedCollectionService;
    }

    @PutMapping("/collections/{id}/save")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void save(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        savedCollectionService.save(CurrentUser.requireId(jwt), id);
    }

    @DeleteMapping("/collections/{id}/save")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unsave(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        savedCollectionService.unsave(CurrentUser.requireId(jwt), id);
    }

    @GetMapping("/collections/saved-collections")
    public CursorPage<CollectionSearchResponse> list(@AuthenticationPrincipal Jwt jwt,
                                                     @RequestParam(required = false) String cursor,
                                                     @RequestParam(defaultValue = PageCursor.DEFAULT_SIZE_PARAM) int size) {
        return savedCollectionService.list(CurrentUser.requireId(jwt), cursor, size);
    }
}
