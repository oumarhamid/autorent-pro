package com.autorentpro.identity.application;

import com.autorentpro.identity.domain.model.PermissionCode;
import com.autorentpro.identity.domain.model.PermissionScope;
import com.autorentpro.identity.domain.model.RoleCode;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.UUID;

@Service("authorizationDecisionService")
public class AuthorizationDecisionService {

    private final IdentityAccessService identityAccessService;

    public AuthorizationDecisionService(
            IdentityAccessService identityAccessService
    ) {
        this.identityAccessService =
                identityAccessService;
    }

    public boolean hasPermission(
            UUID authenticatedUserId,
            PermissionCode permission
    ) {
        if (authenticatedUserId == null
                || permission == null) {
            return false;
        }

        ResolvedIdentityAccess access =
                identityAccessService.resolveForUser(
                        authenticatedUserId
                );

        return access.permissions()
                .stream()
                .anyMatch(
                        grant ->
                                grant.permission()
                                        == permission
                );
    }

    public boolean canAccessSelf(
            UUID authenticatedUserId,
            PermissionCode permission,
            UUID requestedUserId
    ) {
        if (authenticatedUserId == null
                || permission == null
                || requestedUserId == null) {
            return false;
        }

        if (!Objects.equals(
                authenticatedUserId,
                requestedUserId
        )) {
            return false;
        }

        ResolvedIdentityAccess access =
                identityAccessService.resolveForUser(
                        authenticatedUserId
                );

        return hasGrant(
                access,
                permission,
                PermissionScope.SELF
        ) || hasGrant(
                access,
                permission,
                PermissionScope.GLOBAL
        );
    }

    public boolean canAccessGlobal(
            UUID authenticatedUserId,
            PermissionCode permission
    ) {
        if (authenticatedUserId == null
                || permission == null) {
            return false;
        }

        ResolvedIdentityAccess access =
                identityAccessService.resolveForUser(
                        authenticatedUserId
                );

        return hasGrant(
                access,
                permission,
                PermissionScope.GLOBAL
        );
    }

    public boolean canAccessAgency(
            UUID authenticatedUserId,
            PermissionCode permission,
            UUID agencyId
    ) {
        if (authenticatedUserId == null
                || permission == null
                || agencyId == null) {
            return false;
        }

        ResolvedIdentityAccess access =
                identityAccessService.resolveForUser(
                        authenticatedUserId
                );

        if (hasGrant(
                access,
                permission,
                PermissionScope.GLOBAL
        )) {
            return true;
        }

        /*
         * UserAgencyAssignment will be implemented when
         * the real Agency aggregate is introduced in Phase 3.
         *
         * Until then, AGENCY access remains fail-closed.
         */
        return false;
    }

    public boolean hasRole(
            UUID authenticatedUserId,
            RoleCode role
    ) {
        if (authenticatedUserId == null
                || role == null) {
            return false;
        }

        ResolvedIdentityAccess access =
                identityAccessService.resolveForUser(
                        authenticatedUserId
                );

        return access.hasRole(role);
    }

    private boolean hasGrant(
            ResolvedIdentityAccess access,
            PermissionCode permission,
            PermissionScope scope
    ) {
        if (access == null
                || permission == null
                || scope == null) {
            return false;
        }

        return access.hasPermission(
                new PermissionGrant(
                        permission,
                        scope
                )
        );
    }
}