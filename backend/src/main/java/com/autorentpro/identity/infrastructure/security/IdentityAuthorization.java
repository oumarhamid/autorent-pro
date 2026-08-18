package com.autorentpro.identity.infrastructure.security;

import com.autorentpro.identity.application.AuthorizationDecisionService;
import com.autorentpro.identity.domain.model.PermissionCode;
import com.autorentpro.identity.domain.model.RoleCode;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("identityAuthorization")
public class IdentityAuthorization {

    private final AuthorizationDecisionService authorizationDecisionService;

    public IdentityAuthorization(
            AuthorizationDecisionService authorizationDecisionService
    ) {
        this.authorizationDecisionService =
                authorizationDecisionService;
    }

    public boolean hasPermission(
            Authentication authentication,
            String permission
    ) {
        UUID authenticatedUserId =
                extractUserId(authentication);

        PermissionCode permissionCode =
                parsePermission(permission);

        return authenticatedUserId != null
                && permissionCode != null
                && authorizationDecisionService
                        .hasPermission(
                                authenticatedUserId,
                                permissionCode
                        );
    }

    public boolean canAccessSelf(
            Authentication authentication,
            String permission,
            UUID requestedUserId
    ) {
        UUID authenticatedUserId =
                extractUserId(authentication);

        PermissionCode permissionCode =
                parsePermission(permission);

        return authenticatedUserId != null
                && permissionCode != null
                && authorizationDecisionService
                        .canAccessSelf(
                                authenticatedUserId,
                                permissionCode,
                                requestedUserId
                        );
    }

    public boolean canAccessGlobal(
            Authentication authentication,
            String permission
    ) {
        UUID authenticatedUserId =
                extractUserId(authentication);

        PermissionCode permissionCode =
                parsePermission(permission);

        return authenticatedUserId != null
                && permissionCode != null
                && authorizationDecisionService
                        .canAccessGlobal(
                                authenticatedUserId,
                                permissionCode
                        );
    }

    public boolean canAccessAgency(
            Authentication authentication,
            String permission,
            UUID agencyId
    ) {
        UUID authenticatedUserId =
                extractUserId(authentication);

        PermissionCode permissionCode =
                parsePermission(permission);

        return authenticatedUserId != null
                && permissionCode != null
                && authorizationDecisionService
                        .canAccessAgency(
                                authenticatedUserId,
                                permissionCode,
                                agencyId
                        );
    }

    public boolean hasRole(
            Authentication authentication,
            String role
    ) {
        UUID authenticatedUserId =
                extractUserId(authentication);

        RoleCode roleCode =
                parseRole(role);

        return authenticatedUserId != null
                && roleCode != null
                && authorizationDecisionService
                        .hasRole(
                                authenticatedUserId,
                                roleCode
                        );
    }

    private UUID extractUserId(
            Authentication authentication
    ) {
        if (authentication == null
                || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal =
                authentication.getPrincipal();

        if (!(principal
                instanceof AuthenticatedUserPrincipal authenticatedUser)) {
            return null;
        }

        return authenticatedUser.userId();
    }

    private PermissionCode parsePermission(
            String permission
    ) {
        if (permission == null
                || permission.isBlank()) {
            return null;
        }

        try {
            return PermissionCode.valueOf(
                    permission.trim()
            );
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private RoleCode parseRole(
            String role
    ) {
        if (role == null
                || role.isBlank()) {
            return null;
        }

        try {
            return RoleCode.valueOf(
                    role.trim()
            );
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}