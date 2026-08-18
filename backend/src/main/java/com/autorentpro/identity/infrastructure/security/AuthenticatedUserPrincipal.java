package com.autorentpro.identity.infrastructure.security;

import com.autorentpro.identity.application.PermissionGrant;
import com.autorentpro.identity.domain.model.RoleCode;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public record AuthenticatedUserPrincipal(
        UUID userId,
        String email,
        Set<RoleCode> roles,
        Set<PermissionGrant> permissions,
        boolean mustChangePassword
) {

    public AuthenticatedUserPrincipal {
        Objects.requireNonNull(
                userId,
                "userId must not be null"
        );

        Objects.requireNonNull(
                email,
                "email must not be null"
        );

        Objects.requireNonNull(
                roles,
                "roles must not be null"
        );

        Objects.requireNonNull(
                permissions,
                "permissions must not be null"
        );

        roles = Set.copyOf(roles);
        permissions = Set.copyOf(permissions);
    }
}