package com.hakye.mediahub.api.user;

import com.hakye.mediahub.api.common.exceptions.NotFoundException;
import com.hakye.mediahub.api.common.exceptions.UnauthorizedException;
import com.hakye.mediahub.api.user.domain.User;
import com.hakye.mediahub.api.user.domain.UserValidator;
import com.hakye.mediahub.api.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Account lookups and availability checks used across features. */
@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public boolean isEmailAvailable(String email) {
        return !userRepository.existsByEmail(UserValidator.normalizeString(email));
    }

    public boolean isUsernameAvailable(String username) {
        String normalized = UserValidator.normalizeString(username);
        return UserValidator.isUsernameValidPattern(normalized)
                && !UserValidator.isUsernameReserved(normalized)
                && !userRepository.existsByUsername(normalized);
    }

    /** The authenticated caller. A valid token for a deleted account is a 401, not a 404. */
    public User getCurrent(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UnauthorizedException("Account no longer exists"));
    }

    /** Any user by id, e.g. the author of a post. */
    public User getById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    public User getByUsername(String username) {
        return userRepository.findByUsername(UserValidator.normalizeString(username))
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    /** Users for the given ids, in that order. */
    public List<User> getAllInOrder(List<UUID> ids) {
        return userRepository.findAllInOrder(ids);
    }

    /** Points the user at a new avatar object and returns the key it replaced (null if none). */
    @Transactional
    public String replaceAvatarKey(UUID userId, String newKey) {
        User user = getCurrent(userId);
        String old = user.getAvatarKey();
        user.setAvatarKey(newKey);
        return old;
    }
}
