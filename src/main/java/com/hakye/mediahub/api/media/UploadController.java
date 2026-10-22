package com.hakye.mediahub.api.media;

import com.hakye.mediahub.api.common.utils.CurrentUser;
import com.hakye.mediahub.api.media.dto.PresignUploadRequest;
import com.hakye.mediahub.api.media.dto.PresignedUploadResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/uploads")
public class UploadController {

    private final UploadService uploadService;

    public UploadController(UploadService uploadService) {
        this.uploadService = uploadService;
    }

    @PostMapping("/presign")
    public PresignedUploadResponse presign(@AuthenticationPrincipal Jwt jwt,
                                           @Valid @RequestBody PresignUploadRequest request) {
        return uploadService.presign(CurrentUser.requireId(jwt), request);
    }
}
