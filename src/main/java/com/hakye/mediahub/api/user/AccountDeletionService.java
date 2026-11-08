package com.hakye.mediahub.api.user;

import com.hakye.mediahub.api.common.exceptions.BadRequestException;
import com.hakye.mediahub.api.storage.ObjectStorageService;
import com.hakye.mediahub.api.upload.UploadService;
import com.hakye.mediahub.api.user.domain.User;
import com.hakye.mediahub.api.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.UUID;

@Service
public class AccountDeletionService {

    private final UserService userService;
    private final UserRepository users;          // adjust to your repository's name/package
    private final UploadService uploadService;
    private final PasswordEncoder passwordEncoder;
    private final ObjectStorageService storage;

    public AccountDeletionService(UserService userService, UserRepository users, UploadService uploadService,
                                  PasswordEncoder passwordEncoder, ObjectStorageService storage) {
        this.userService = userService;
        this.users = users;
        this.uploadService = uploadService;
        this.passwordEncoder = passwordEncoder;
        this.storage = storage;
    }

    /** Confirms the password, deletes the account (DB cascades), then removes its images from R2. */
    @Transactional
    public void delete(UUID userId, String currentPassword) {
        User user = userService.getById(userId);
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) { // adjust getter
            // 400, not 401: a 401 would make the web client try a token refresh
            throw new BadRequestException("Current password is incorrect");
        }

        List<String> keys = uploadService.mediaKeysOf(userId);
        users.deleteAccount(userId);

        // Only touch R2 once the row is really gone; deleteQuietly never fails the request
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                keys.forEach(storage::deleteQuietly);
            }
        });
    }
}