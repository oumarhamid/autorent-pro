package com.autorentpro.identity.api;

import com.autorentpro.identity.application.UserView;
import com.autorentpro.identity.domain.model.RoleCode;
import com.autorentpro.identity.domain.model.UserStatus;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record UserResponse(
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

    public static UserResponse from(
            UserView view
    ) {
        return new UserResponse(
                view.id(),
                view.email(),
                view.status(),
                view.mustChangePassword(),
                view.roles(),
                view.lockedUntil(),
                view.lastLoginAt(),
                view.passwordChangedAt(),
                view.createdAt(),
                view.updatedAt()
        );
    }
}