package com.autorentpro.identity.application;

import com.autorentpro.identity.domain.model.RoleCode;
import com.autorentpro.identity.domain.model.UserStatus;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record UserView(
        UUID id,
        String email,
        UserStatus status,
        boolean mustChangePassword,
        Set<RoleCode> roles,
        Instant lockedUntil,
        Instant lastLoginAt,
        Instant passwordChangedAt,
        Instant createdAt,
        Instant updatedAt
) {

    public UserView {
        roles = Set.copyOf(roles);
    }
}