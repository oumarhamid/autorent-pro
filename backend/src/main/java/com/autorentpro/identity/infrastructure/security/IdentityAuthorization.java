package com.autorentpro.identity.infrastructure.security;

import com.autorentpro.identity.application.AuthorizationDecisionService;
import com.autorentpro.identity.domain.model.PermissionCode;
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
        AuthenticatedUserPrincipal principal =
                extractPrincipal(authentication);

        PermissionCode permissionCode =
                parsePermission(permission);

        return principal != null
                && permissionCode != null
                && authorizationDecisionService
                        .hasPermission(
                                principal,
                                permissionCode
                        );
    }

    public boolean canAccessSelf(
            Authentication authentication,
            String permission,
            UUID requestedUserId
    ) {
        AuthenticatedUserPrincipal principal =
                extractPrincipal(authentication);

        PermissionCode permissionCode =
                parsePermission(permission);

        return principal != null
                && permissionCode != null
                && authorizationDecisionService
                        .canAccessSelf(
                                principal,
                                permissionCode,
                                requestedUserId
                        );
    }

    public boolean canAccessGlobal(
            Authentication authentication,
            String permission
    ) {
        AuthenticatedUserPrincipal principal =
                extractPrincipal(authentication);

        PermissionCode permissionCode =
                parsePermission(permission);

        return principal != null
                && permissionCode != null
                && authorizationDecisionService
                        .canAccessGlobal(
                                principal,
                                permissionCode
                        );
    }

    private AuthenticatedUserPrincipal extractPrincipal(
            Authentication authentication
    ) {
        if (authentication == null
                || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();

        if (!(principal
                instanceof AuthenticatedUserPrincipal authenticatedUser)) {
            return null;
        }

        return authenticatedUser;
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
}