package com.autorentpro.identity.application;

import com.autorentpro.identity.domain.model.RoleCode;

import java.util.Objects;
import java.util.UUID;

public record UserRoleAssignmentView(
        UUID userId,
        RoleCode role
) {

    public UserRoleAssignmentView {
        Objects.requireNonNull(
                userId,
                "userId must not be null"
        );

        Objects.requireNonNull(
                role,
                "role must not be null"
        );
    }
}