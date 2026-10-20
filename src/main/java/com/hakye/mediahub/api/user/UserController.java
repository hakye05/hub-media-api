package com.hakye.mediahub.api.user;

import com.hakye.mediahub.api.auth.dto.AvailabilityResponse;
import com.hakye.mediahub.api.follow.FollowService;
import com.hakye.mediahub.api.user.dto.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("api/users")
public class UserController {

    private final UserService userService;
    private final FollowService followService;

    public UserController(UserService userService, FollowService followService) {
        this.userService = userService;
        this.followService = followService;
    }

    @GetMapping("/check-username")
    public AvailabilityResponse checkUsername(@RequestParam String username) {
        return new AvailabilityResponse(userService.isUsernameAvailable(username));
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal Jwt jwt) {
        User user = userService.getById(UUID.fromString(jwt.getSubject()));
        return UserResponse.from(user);
    }

    @PostMapping("/{username}/follow")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void follow(@PathVariable String username, @AuthenticationPrincipal Jwt jwt) {
        followService.follow(UUID.fromString(jwt.getSubject()), username);
    }

    @DeleteMapping("/{username}/follow")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unfollow(@PathVariable String username, @AuthenticationPrincipal Jwt jwt) {
        followService.unfollow(UUID.fromString(jwt.getSubject()), username);
    }
}
