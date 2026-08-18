package com.autorentpro.identity.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;

@Entity
@Table(name = "identity_users")
public class UserAccount extends AbstractAuditableEntity {

    private static final int MAX_EMAIL_LENGTH = 320;
    private static final int MAX_PASSWORD_HASH_LENGTH = 255;

    @Column(name = "email", nullable = false, unique = true, length = MAX_EMAIL_LENGTH)
    private String email;

    @Column(name = "password_hash", nullable = false, length = MAX_PASSWORD_HASH_LENGTH)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private UserStatus status;

    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "password_changed_at", nullable = false)
    private Instant passwordChangedAt;

    @Column(name = "must_change_password", nullable = false)
    private boolean mustChangePassword;

    protected UserAccount() {
    }

    private UserAccount(
            String email,
            String passwordHash,
            boolean mustChangePassword,
            Instant passwordChangedAt
    ) {
        this.email = normalizeEmail(email);
        this.passwordHash = requireText(
                passwordHash,
                "passwordHash",
                MAX_PASSWORD_HASH_LENGTH
        );
        this.status = UserStatus.ACTIVE;
        this.failedLoginAttempts = 0;
        this.passwordChangedAt = Objects.requireNonNull(
                passwordChangedAt,
                "passwordChangedAt must not be null"
        );
        this.mustChangePassword = mustChangePassword;
    }

    public static UserAccount create(
            String email,
            String passwordHash,
            boolean mustChangePassword,
            Instant passwordChangedAt
    ) {
        return new UserAccount(
                email,
                passwordHash,
                mustChangePassword,
                passwordChangedAt
        );
    }

    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }

    public boolean isTemporarilyLocked(Instant now) {
        Objects.requireNonNull(now, "now must not be null");

        return lockedUntil != null && lockedUntil.isAfter(now);
    }

    public void registerFailedLogin(
            int maxAttempts,
            Duration lockDuration,
            Instant now
    ) {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException(
                    "maxAttempts must be greater than zero"
            );
        }

        if (lockDuration == null
                || lockDuration.isZero()
                || lockDuration.isNegative()) {
            throw new IllegalArgumentException(
                    "lockDuration must be positive"
            );
        }

        Objects.requireNonNull(now, "now must not be null");

        if (isTemporarilyLocked(now)) {
            return;
        }

        if (lockedUntil != null && !lockedUntil.isAfter(now)) {
            lockedUntil = null;
            failedLoginAttempts = 0;
        }

        failedLoginAttempts++;

        if (failedLoginAttempts >= maxAttempts) {
            lockedUntil = now.plus(lockDuration);
        }
    }

    public void recordSuccessfulLogin(Instant now) {
        Objects.requireNonNull(now, "now must not be null");

        failedLoginAttempts = 0;
        lockedUntil = null;
        lastLoginAt = now;
    }

    public void changePasswordHash(
            String newPasswordHash,
            boolean requirePasswordChange,
            Instant changedAt
    ) {
        this.passwordHash = requireText(
                newPasswordHash,
                "newPasswordHash",
                MAX_PASSWORD_HASH_LENGTH
        );

        this.passwordChangedAt = Objects.requireNonNull(
                changedAt,
                "changedAt must not be null"
        );

        this.mustChangePassword = requirePasswordChange;
        this.failedLoginAttempts = 0;
        this.lockedUntil = null;
    }

    public void requirePasswordChange() {
        mustChangePassword = true;
    }

    public void disable() {
        status = UserStatus.DISABLED;
    }

    public void enable() {
        status = UserStatus.ACTIVE;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public UserStatus getStatus() {
        return status;
    }

    public int getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    public Instant getLockedUntil() {
        return lockedUntil;
    }

    public Instant getLastLoginAt() {
        return lastLoginAt;
    }

    public Instant getPasswordChangedAt() {
        return passwordChangedAt;
    }

    public boolean isMustChangePassword() {
        return mustChangePassword;
    }

    private static String normalizeEmail(String email) {
        String normalized = Objects.requireNonNull(
                email,
                "email must not be null"
        ).trim().toLowerCase(Locale.ROOT);

        if (normalized.isBlank()) {
            throw new IllegalArgumentException(
                    "email must not be blank"
            );
        }

        if (normalized.length() > MAX_EMAIL_LENGTH) {
            throw new IllegalArgumentException(
                    "email must not exceed "
                            + MAX_EMAIL_LENGTH
                            + " characters"
            );
        }

        return normalized;
    }

    private static String requireText(
            String value,
            String fieldName,
            int maxLength
    ) {
        String validated = Objects.requireNonNull(
                value,
                fieldName + " must not be null"
        );

        if (validated.isBlank()) {
            throw new IllegalArgumentException(
                    fieldName + " must not be blank"
            );
        }

        if (validated.length() > maxLength) {
            throw new IllegalArgumentException(
                    fieldName
                            + " must not exceed "
                            + maxLength
                            + " characters"
            );
        }

        return validated;
    }
}