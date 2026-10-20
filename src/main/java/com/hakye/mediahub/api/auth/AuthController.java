package com.hakye.mediahub.api.auth;

import com.hakye.mediahub.api.auth.dto.AuthResponse;
import com.hakye.mediahub.api.auth.dto.AvailabilityResponse;
import com.hakye.mediahub.api.auth.dto.LoginRequest;
import com.hakye.mediahub.api.auth.dto.RegisterRequest;
import com.hakye.mediahub.api.user.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    public AuthController(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/check-email")
    public AvailabilityResponse checkEmail(@RequestParam String email) {
        return new AvailabilityResponse(userService.isEmailAvailable(email));
    }

    @GetMapping("/check-username")
    public AvailabilityResponse checkUsername(@RequestParam String username) {
        return new AvailabilityResponse(userService.isUsernameAvailable(username));
    }
}
