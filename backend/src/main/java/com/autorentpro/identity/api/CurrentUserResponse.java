package com.autorentpro.identity.api;

import com.autorentpro.identity.application.PermissionGrant;
import com.autorentpro.identity.application.ResolvedIdentityAccess;
import com.autorentpro.identity.domain.model.RoleCode;
import com.autorentpro.identity.infrastructure.security.AuthenticatedUserPrincipal;

import java.util.Set;
import java.util.UUID;

public record CurrentUserResponse(
        UUID userId,
        String email,
        Set<RoleCode> roles,
        Set<PermissionGrant> permissions,
        boolean mustChangePassword
) {

    public CurrentUserResponse {
        roles = Set.copyOf(roles);
        permissions = Set.copyOf(permissions);
    }

    public static CurrentUserResponse from(
            AuthenticatedUserPrincipal principal,
            ResolvedIdentityAccess access
    ) {
        return new CurrentUserResponse(
                principal.userId(),
                principal.email(),
                access.roles(),
                access.permissions(),
                principal.mustChangePassword()
        );
    }
}