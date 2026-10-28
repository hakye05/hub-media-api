package com.hakye.mediahub.api.upload;

import com.hakye.mediahub.api.common.utils.CurrentUser;
import com.hakye.mediahub.api.upload.dto.CreateUploadRequest;
import com.hakye.mediahub.api.upload.dto.PresignedUploadResponse;
import com.hakye.mediahub.api.upload.dto.UploadResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/uploads")
public class UploadController {

    private final UploadService uploadService;

    public UploadController(UploadService uploadService) {
        this.uploadService = uploadService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PresignedUploadResponse create(@AuthenticationPrincipal Jwt jwt,
                                          @Valid @RequestBody CreateUploadRequest request) {
        return uploadService.create(CurrentUser.requireId(jwt), request);
    }

    @PostMapping("/{id}/complete")
    public UploadResponse complete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return uploadService.complete(CurrentUser.requireId(jwt), id);
    }
}
