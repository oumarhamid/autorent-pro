package com.autorentpro.identity.api;

import com.autorentpro.identity.application.PermissionGrant;
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

    public static CurrentUserResponse from(
            AuthenticatedUserPrincipal principal
    ) {
        return new CurrentUserResponse(
                principal.userId(),
                principal.email(),
                principal.roles(),
                principal.permissions(),
                principal.mustChangePassword()
        );
    }
}