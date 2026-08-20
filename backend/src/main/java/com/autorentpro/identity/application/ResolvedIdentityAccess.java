package com.autorentpro.identity.application;

import com.autorentpro.identity.domain.model.RoleCode;

import java.util.Objects;
import java.util.Set;

public record ResolvedIdentityAccess(
        Set<RoleCode> roles,
        Set<PermissionGrant> permissions
) {

    public ResolvedIdentityAccess {
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

    public boolean hasRole(RoleCode role) {
        return roles.contains(role);
    }

    public boolean hasPermission(PermissionGrant grant) {
        return permissions.contains(grant);
    }
}