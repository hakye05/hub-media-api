package com.hakye.mediahub.api.user;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public class UserValidator {

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-z0-9_.]{3,30}$");

    public static final Set<String> RESERVED_USERNAMES = Set.of(
            "login", "signin", "signup", "start", "logout", "about", "home",
            "post", "posts", "collections", "profile", "settings", "search",
            "explore", "api", "admin", "help", "support", "me", "hub", "hakye", "www");

    public static String normalizeString(String string) {
        if (string == null) return "";
        return string.trim().toLowerCase(Locale.ROOT);
    }

    public static boolean isUsernameValidPattern(String normalizedUsername) {
        return USERNAME_PATTERN.matcher(normalizedUsername).matches();
    }

    public static boolean isUsernameReserved(String normalizedUsername) {
        return RESERVED_USERNAMES.contains(normalizedUsername);
    }
}
