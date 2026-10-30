package com.hakye.mediahub.api.user;

import com.hakye.mediahub.api.auth.AuthService;
import com.hakye.mediahub.api.auth.RefreshCookieFactory;
import com.hakye.mediahub.api.auth.dto.AuthResponse;
import com.hakye.mediahub.api.auth.dto.AuthResult;
import com.hakye.mediahub.api.common.dto.CursorPage;
import com.hakye.mediahub.api.common.utils.CurrentUser;
import com.hakye.mediahub.api.common.utils.PageCursor;
import com.hakye.mediahub.api.user.dto.*;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserProfileService profileService;
    private final AuthService authService;
    private final RefreshCookieFactory cookies;

    public UserController(UserProfileService profileService, AuthService authService, RefreshCookieFactory cookies) {
        this.profileService = profileService;
        this.authService = authService;
        this.cookies = cookies;
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal Jwt jwt) {
        return profileService.getMe(CurrentUser.requireId(jwt));
    }

    @PatchMapping("/me")
    public UserResponse updateProfile(@AuthenticationPrincipal Jwt jwt,
                                      @Valid @RequestBody UpdateProfileRequest request) {
        return profileService.updateProfile(CurrentUser.requireId(jwt), request);
    }

    @PatchMapping("/me/username")
    public UserResponse changeUsername(@AuthenticationPrincipal Jwt jwt,
                                       @Valid @RequestBody ChangeUsernameRequest request) {
        return profileService.changeUsername(CurrentUser.requireId(jwt), request.username());
    }

    @PatchMapping("/me/email")
    public UserResponse changeEmail(@AuthenticationPrincipal Jwt jwt,
                                    @Valid @RequestBody ChangeEmailRequest request) {
        return profileService.changeEmail(CurrentUser.requireId(jwt), request);
    }

    @PatchMapping("/me/password")
    public AuthResponse changePassword(@AuthenticationPrincipal Jwt jwt,
                                       @Valid @RequestBody ChangePasswordRequest request,
                                       HttpServletResponse response) {
        AuthResult result = authService.changePassword(CurrentUser.requireId(jwt), request);
        if (result.refreshToken() != null) {
            response.addHeader(HttpHeaders.SET_COOKIE, cookies.create(result.refreshToken()).toString());
        }
        return result.body();
    }

    @PatchMapping("/me/avatar")
    public UserResponse changeAvatar(@AuthenticationPrincipal Jwt jwt,
                                     @Valid @RequestBody ChangeAvatarRequest request) {
        return profileService.changeAvatar(CurrentUser.requireId(jwt), request.uploadId());
    }

    @DeleteMapping("/me/avatar")
    public UserResponse removeAvatar(@AuthenticationPrincipal Jwt jwt) {
        return profileService.removeAvatar(CurrentUser.requireId(jwt));
    }

    @GetMapping("/{username}")
    public PublicProfileResponse profile(@PathVariable String username, @AuthenticationPrincipal Jwt jwt) {
        return profileService.getPublicProfile(username, CurrentUser.optionalId(jwt));
    }

    @PostMapping("/{username}/follow")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void follow(@PathVariable String username, @AuthenticationPrincipal Jwt jwt) {
        profileService.follow(CurrentUser.requireId(jwt), username);
    }

    @DeleteMapping("/{username}/follow")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unfollow(@PathVariable String username, @AuthenticationPrincipal Jwt jwt) {
        profileService.unfollow(CurrentUser.requireId(jwt), username);
    }

    @GetMapping("/{username}/followers")
    public CursorPage<ProfileSummaryResponse> followers(@PathVariable String username,
                                                        @RequestParam(required = false) String cursor,
                                                        @RequestParam(defaultValue = PageCursor.DEFAULT_SIZE_PARAM) int size,
                                                        @AuthenticationPrincipal Jwt jwt) {
        return profileService.followers(username, CurrentUser.optionalId(jwt), cursor, size);
    }

    @GetMapping("/{username}/following")
    public CursorPage<ProfileSummaryResponse> following(@PathVariable String username,
                                                        @RequestParam(required = false) String cursor,
                                                        @RequestParam(defaultValue = PageCursor.DEFAULT_SIZE_PARAM) int size,
                                                        @AuthenticationPrincipal Jwt jwt) {
        return profileService.following(username, CurrentUser.optionalId(jwt), cursor, size);
    }
}
