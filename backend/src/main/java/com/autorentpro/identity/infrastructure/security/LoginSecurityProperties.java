package com.autorentpro.identity.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Objects;

@ConfigurationProperties(prefix = "autorent.security.login")
public record LoginSecurityProperties(
        int maxFailedAttempts,
        Duration lockDuration
) {

    public LoginSecurityProperties {
        if (maxFailedAttempts < 1) {
            throw new IllegalArgumentException(
                    "maxFailedAttempts must be greater than zero"
            );
        }

        Objects.requireNonNull(
                lockDuration,
                "lockDuration must not be null"
        );

        if (lockDuration.isZero() || lockDuration.isNegative()) {
            throw new IllegalArgumentException(
                    "lockDuration must be positive"
            );
        }
    }
}