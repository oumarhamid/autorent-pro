package com.autorentpro.identity.application;

import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Set;

@Component
public class PasswordPolicy {

    public static final int MIN_LENGTH = 15;
    public static final int MAX_LENGTH = 128;

    private static final Set<String> BLOCKED_PASSWORDS =
            Set.of(
                    "123456789012345",
                    "passwordpassword",
                    "password123456",
                    "qwertyqwertyqwerty",
                    "letmeinletmein123",
                    "adminadminadmin123",
                    "autorentautorent",
                    "autorentproautorent"
            );

    public void validate(
            String password
    ) {
        if (password == null || password.isBlank()) {
            throw new PasswordChangeException(
                    "PASSWORD_REQUIRED",
                    "A password is required."
            );
        }

        int length = password.codePointCount(
                0,
                password.length()
        );

        if (length < MIN_LENGTH) {
            throw new PasswordChangeException(
                    "PASSWORD_TOO_SHORT",
                    "The password must contain at least "
                            + MIN_LENGTH
                            + " characters."
            );
        }

        if (length > MAX_LENGTH) {
            throw new PasswordChangeException(
                    "PASSWORD_TOO_LONG",
                    "The password must not exceed "
                            + MAX_LENGTH
                            + " characters."
            );
        }

        String normalized =
                password
                        .trim()
                        .toLowerCase(Locale.ROOT);

        if (BLOCKED_PASSWORDS.contains(normalized)) {
            throw new PasswordChangeException(
                    "PASSWORD_TOO_COMMON",
                    "The password is too common."
            );
        }
    }
}