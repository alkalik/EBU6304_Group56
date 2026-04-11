package com.recruitment.util;

import com.recruitment.service.UserService;

/**
 * Shared registration validation: password policy, username format and availability, email format and availability.
 */
public final class RegistrationRules {

    public static final int USERNAME_MIN_LEN = 3;
    public static final int USERNAME_MAX_LEN = 32;

    private RegistrationRules() {
    }

    /** Password rules: trim whitespace; at least 8 chars, one letter, one digit. */
    public static String passwordPolicyFailure(String rawPassword) {
        String password = rawPassword == null ? "" : rawPassword.trim();
        if (password.isEmpty()) {
            return "Please enter a password.";
        }
        if (password.length() < 8) {
            return "Use at least 8 characters.";
        }
        if (!password.matches(".*[A-Za-z].*")) {
            return "Include at least one letter.";
        }
        if (!password.matches(".*\\d.*")) {
            return "Include at least one number.";
        }
        return null;
    }

    public static String confirmPasswordFailure(String rawPassword, String rawConfirm) {
        String password = rawPassword == null ? "" : rawPassword.trim();
        String confirm = rawConfirm == null ? "" : rawConfirm.trim();
        if (confirm.isEmpty()) {
            return "Please confirm your password.";
        }
        if (!password.equals(confirm)) {
            return "Passwords do not match.";
        }
        return null;
    }

    /** Username format only (no DB check). Trim for length; pattern applies to trimmed value. */
    public static String usernameFormatFailure(String rawUsername) {
        String username = rawUsername == null ? "" : rawUsername.trim();
        if (username.isEmpty()) {
            return "Please enter a username.";
        }
        if (username.length() < USERNAME_MIN_LEN) {
            return "Username must be at least " + USERNAME_MIN_LEN + " characters.";
        }
        if (username.length() > USERNAME_MAX_LEN) {
            return "Username must be at most " + USERNAME_MAX_LEN + " characters.";
        }
        if (!username.matches("[a-zA-Z0-9][a-zA-Z0-9_.-]*")) {
            return "Use letters, numbers, dot, underscore or hyphen; start with a letter or number.";
        }
        return null;
    }

    /** Call only when {@link #usernameFormatFailure(String)} is null. */
    public static String usernameAvailabilityFailure(String trimmedUsername, UserService userService) {
        if (userService.isUsernameTaken(trimmedUsername)) {
            return "This username is already registered.";
        }
        return null;
    }

    public static String emailFormatFailure(String rawEmail) {
        String email = rawEmail == null ? "" : rawEmail.trim();
        if (email.isEmpty()) {
            return "Please enter your email.";
        }
        if (email.length() > 254) {
            return "Email is too long.";
        }
        if (!email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")) {
            return "Enter a valid email address.";
        }
        return null;
    }

    /** Call only when {@link #emailFormatFailure(String)} is null. */
    public static String emailAvailabilityFailure(String trimmedEmail, UserService userService) {
        if (userService.isEmailRegistered(trimmedEmail)) {
            return "This email is already registered.";
        }
        return null;
    }
}
