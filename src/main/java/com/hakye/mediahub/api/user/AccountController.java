package com.hakye.mediahub.api.user;

import com.hakye.mediahub.api.common.utils.CurrentUser;
import com.hakye.mediahub.api.user.dto.DeleteAccountRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class AccountController {

    private final AccountDeletionService accountDeletion;

    public AccountController(AccountDeletionService accountDeletion) {
        this.accountDeletion = accountDeletion;
    }

    @DeleteMapping("/users/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody DeleteAccountRequest request) {
        accountDeletion.delete(CurrentUser.requireId(jwt), request.currentPassword());
    }
}
