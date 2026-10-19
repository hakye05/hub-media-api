package com.hakye.mediahub.api.user;

import com.hakye.mediahub.api.common.UnauthorizedException;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
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
}
