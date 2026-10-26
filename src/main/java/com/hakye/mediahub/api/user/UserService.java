package com.hakye.mediahub.api.user;

import com.hakye.mediahub.api.common.exceptions.NotFoundException;
import com.hakye.mediahub.api.common.exceptions.UnauthorizedException;
import com.hakye.mediahub.api.common.storage.ObjectStorageService;
import com.hakye.mediahub.api.user.dto.UserResponse;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final ObjectStorageService storage;


    public UserService(UserRepository userRepository, ObjectStorageService storage) {
        this.userRepository = userRepository;
        this.storage = storage;
    }

    public boolean isEmailAvailable(String email) {
        return !userRepository.existsByEmailIgnoreCase(UserValidator.normalizeString(email));
    }

    public boolean isUsernameAvailable(String username) {
        String normalized = UserValidator.normalizeString(username);
        return UserValidator.isUsernameValidPattern(normalized)
                && !UserValidator.isUsernameReserved(normalized)
                && !userRepository.existsByUsernameIgnoreCase(normalized);
    }

    public User getById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UnauthorizedException("Account no longer exists"));
    }

    public User getByUsernameOrThrow(String username) {
        return userRepository.findByUsernameIgnoreCase(UserValidator.normalizeString(username))
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    public UserResponse toResponse(User user) {
        return UserResponse.from(user, storage.publicUrl(user.getAvatarKey()));
    }
}
