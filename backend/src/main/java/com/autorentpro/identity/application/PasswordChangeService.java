package com.autorentpro.identity.application;

import com.autorentpro.identity.domain.model.UserAccount;
import com.autorentpro.identity.infrastructure.persistence.UserAccountRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Service
public class PasswordChangeService {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicy passwordPolicy;
    private final Clock clock;

    public PasswordChangeService(
            UserAccountRepository userAccountRepository,
            PasswordEncoder passwordEncoder,
            PasswordPolicy passwordPolicy,
            Clock clock
    ) {
        this.userAccountRepository =
                userAccountRepository;

        this.passwordEncoder =
                passwordEncoder;

        this.passwordPolicy =
                passwordPolicy;

        this.clock =
                clock;
    }

    @Transactional
    public void changePassword(
            UUID userId,
            String currentPassword,
            String newPassword
    ) {
        Objects.requireNonNull(
                userId,
                "userId must not be null"
        );

        UserAccount user =
                userAccountRepository
                        .findForUpdateById(userId)
                        .orElseThrow(
                                () ->
                                        new PasswordChangeException(
                                                "ACCOUNT_UNAVAILABLE",
                                                "The account is unavailable."
                                        )
                        );

        if (!user.isActive()) {
            throw new PasswordChangeException(
                    "ACCOUNT_UNAVAILABLE",
                    "The account is unavailable."
            );
        }

        if (!passwordEncoder.matches(
                currentPassword,
                user.getPasswordHash()
        )) {
            throw new PasswordChangeException(
                    "CURRENT_PASSWORD_INVALID",
                    "The current password is invalid."
            );
        }

        passwordPolicy.validate(
                newPassword
        );

        if (passwordEncoder.matches(
                newPassword,
                user.getPasswordHash()
        )) {
            throw new PasswordChangeException(
                    "PASSWORD_REUSE_NOT_ALLOWED",
                    "The new password must be different from the current password."
            );
        }

        String newPasswordHash =
                passwordEncoder.encode(
                        newPassword
                );

        user.changePasswordHash(
                newPasswordHash,
                false,
                Instant.now(clock)
        );
    }
}