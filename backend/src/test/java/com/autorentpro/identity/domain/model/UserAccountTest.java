package com.autorentpro.identity.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class UserAccountTest {

    private static final Instant NOW =
            Instant.parse("2026-08-18T12:00:00Z");

    @Test
    void createsActiveUserWithNormalizedEmail() {
        UserAccount user = UserAccount.create(
                "  Admin@Example.COM  ",
                "{bcrypt}hash",
                true,
                NOW
        );

        assertThat(user.getEmail())
                .isEqualTo("admin@example.com");

        assertThat(user.getStatus())
                .isEqualTo(UserStatus.ACTIVE);

        assertThat(user.getFailedLoginAttempts())
                .isZero();

        assertThat(user.getLockedUntil())
                .isNull();

        assertThat(user.getPasswordChangedAt())
                .isEqualTo(NOW);

        assertThat(user.isMustChangePassword())
                .isTrue();
    }

    @Test
    void locksAccountAfterConfiguredFailedAttempts() {
        UserAccount user = UserAccount.create(
                "user@example.com",
                "{bcrypt}hash",
                false,
                NOW
        );

        Duration lockDuration = Duration.ofMinutes(15);

        for (int attempt = 0; attempt < 5; attempt++) {
            user.registerFailedLogin(
                    5,
                    lockDuration,
                    NOW.plusSeconds(attempt)
            );
        }

        assertThat(user.getFailedLoginAttempts())
                .isEqualTo(5);

        assertThat(
                user.isTemporarilyLocked(
                        NOW.plusSeconds(5)
                )
        ).isTrue();
    }

    @Test
    void successfulLoginClearsFailedAttemptsAndLock() {
        UserAccount user = UserAccount.create(
                "user@example.com",
                "{bcrypt}hash",
                false,
                NOW
        );

        for (int attempt = 0; attempt < 5; attempt++) {
            user.registerFailedLogin(
                    5,
                    Duration.ofMinutes(15),
                    NOW
            );
        }

        Instant successfulLoginAt =
                NOW.plus(Duration.ofMinutes(16));

        user.recordSuccessfulLogin(successfulLoginAt);

        assertThat(user.getFailedLoginAttempts())
                .isZero();

        assertThat(user.getLockedUntil())
                .isNull();

        assertThat(user.getLastLoginAt())
                .isEqualTo(successfulLoginAt);
    }

    @Test
    void passwordChangeResetsTemporarySecurityState() {
        UserAccount user = UserAccount.create(
                "user@example.com",
                "{bcrypt}old",
                false,
                NOW
        );

        user.registerFailedLogin(
                1,
                Duration.ofMinutes(15),
                NOW
        );

        Instant changedAt = NOW.plusSeconds(30);

        user.changePasswordHash(
                "{bcrypt}new",
                false,
                changedAt
        );

        assertThat(user.getPasswordHash())
                .isEqualTo("{bcrypt}new");

        assertThat(user.getPasswordChangedAt())
                .isEqualTo(changedAt);

        assertThat(user.getFailedLoginAttempts())
                .isZero();

        assertThat(user.getLockedUntil())
                .isNull();
    }

    @Test
    void disablesAndEnablesUser() {
        UserAccount user = UserAccount.create(
                "user@example.com",
                "{bcrypt}hash",
                false,
                NOW
        );

        user.disable();

        assertThat(user.getStatus())
                .isEqualTo(UserStatus.DISABLED);

        assertThat(user.isActive())
                .isFalse();

        user.enable();

        assertThat(user.getStatus())
                .isEqualTo(UserStatus.ACTIVE);

        assertThat(user.isActive())
                .isTrue();
    }
}