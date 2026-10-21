package com.hakye.mediahub.api.user;

import com.hakye.mediahub.api.auth.dto.AvailabilityResponse;
import com.hakye.mediahub.api.common.dto.PageResponse;
import com.hakye.mediahub.api.common.utils.CurrentUser;
import com.hakye.mediahub.api.follow.FollowService;
import com.hakye.mediahub.api.user.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/users")
public class UserController {

    private final UserService userService;
    private final UserProfileService profileService;
    private final FollowService followService;

    public UserController(UserService userService, UserProfileService profileService, FollowService followService) {
        this.userService = userService;
        this.profileService = profileService;
        this.followService = followService;
    }

    @GetMapping("/check-username")
    public AvailabilityResponse checkUsername(@RequestParam String username) {
        return new AvailabilityResponse(userService.isUsernameAvailable(username));
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal Jwt jwt) {
        User user = userService.getById(CurrentUser.requireId(jwt));
        return UserResponse.from(user);
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

    @PutMapping("/me/avatar")
    public UserResponse setAvatar(@AuthenticationPrincipal Jwt jwt,
                                  @Valid @RequestBody ChangeAvatarRequest request) {
        return profileService.setAvatar(CurrentUser.requireId(jwt), request.avatarUrl());
    }

    @DeleteMapping("/me/avatar")
    public UserResponse removeAvatar(@AuthenticationPrincipal Jwt jwt) {
        return profileService.removeAvatar(CurrentUser.requireId(jwt));
    }

    // Get Public Profile
    @GetMapping("/{username}")
    public PublicProfileResponse profile(@PathVariable String username,
                                         @AuthenticationPrincipal Jwt jwt) {
        return profileService.getPublicProfile(username, CurrentUser.optionalId(jwt));
    }

    @PostMapping("/{username}/follow")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void follow(@PathVariable String username, @AuthenticationPrincipal Jwt jwt) {
        followService.follow(CurrentUser.requireId(jwt), username);
    }

    @DeleteMapping("/{username}/follow")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unfollow(@PathVariable String username, @AuthenticationPrincipal Jwt jwt) {
        followService.unfollow(CurrentUser.requireId(jwt), username);
    }

    @GetMapping("/{username}/followers")
    public PageResponse<ProfileSummaryResponse> followers(@PathVariable String username,
                                                          @RequestParam(defaultValue = "0") int page,
                                                          @RequestParam(defaultValue = "20") int size,
                                                          @AuthenticationPrincipal Jwt jwt) {
        return followService.followers(username, CurrentUser.optionalId(jwt), page, size);
    }

    @GetMapping("/{username}/following")
    public PageResponse<ProfileSummaryResponse> following(@PathVariable String username,
                                                          @RequestParam(defaultValue = "0") int page,
                                                          @RequestParam(defaultValue = "20") int size,
                                                          @AuthenticationPrincipal Jwt jwt) {
        return followService.following(username, CurrentUser.optionalId(jwt), page, size);
    }
}
