package com.hakye.mediahub.api.user.domain;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/** Single source for the account field rules shared by request DTOs, services and the entity. */
public final class UserValidator {

    public static final int DISPLAY_NAME_MAX = 50;
    public static final int BIO_MAX = 500;

    public static final String USERNAME_REGEX = "^[A-Za-z0-9_.]{3,30}$";
    public static final String USERNAME_MESSAGE = "Username must be 3-30 characters: letters, numbers, dots, underscores";

    private static final Pattern USERNAME_PATTERN = Pattern.compile(USERNAME_REGEX);

    public static final Set<String> RESERVED_USERNAMES = Set.of(
            "login", "signin", "signup", "start", "logout", "about", "home", "staff",
            "post", "posts", "collections", "profile", "settings", "search", "company",
            "explore", "api", "admin", "help", "support", "me", "hub", "hakye", "www");

    private UserValidator() {
    }

    public static String normalizeString(String string) {
        if (string == null) return "";
        return string.trim().toLowerCase(Locale.ROOT);
    }

    public static boolean isUsernameValidPattern(String username) {
        return USERNAME_PATTERN.matcher(username).matches();
    }

    public static boolean isUsernameReserved(String normalizedUsername) {
        return RESERVED_USERNAMES.contains(normalizedUsername);
    }
}
