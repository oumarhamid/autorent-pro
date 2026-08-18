package com.autorentpro.identity.application;

import com.autorentpro.identity.domain.model.PermissionCode;
import com.autorentpro.identity.domain.model.PermissionScope;
import com.autorentpro.identity.infrastructure.security.AuthenticatedUserPrincipal;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.UUID;

@Service("authorizationDecisionService")
public class AuthorizationDecisionService {

    public boolean hasPermission(
            AuthenticatedUserPrincipal principal,
            PermissionCode permission
    ) {
        if (principal == null || permission == null) {
            return false;
        }

        return principal.permissions()
                .stream()
                .anyMatch(
                        grant ->
                                grant.permission()
                                        == permission
                );
    }

    public boolean canAccessSelf(
            AuthenticatedUserPrincipal principal,
            PermissionCode permission,
            UUID requestedUserId
    ) {
        if (principal == null
                || permission == null
                || requestedUserId == null) {
            return false;
        }

        if (!Objects.equals(
                principal.userId(),
                requestedUserId
        )) {
            return false;
        }

        return hasGrant(
                principal,
                permission,
                PermissionScope.SELF
        ) || hasGrant(
                principal,
                permission,
                PermissionScope.GLOBAL
        );
    }

    public boolean canAccessGlobal(
            AuthenticatedUserPrincipal principal,
            PermissionCode permission
    ) {
        return hasGrant(
                principal,
                permission,
                PermissionScope.GLOBAL
        );
    }

    public boolean canAccessAgency(
            AuthenticatedUserPrincipal principal,
            PermissionCode permission,
            UUID agencyId
    ) {
        if (principal == null
                || permission == null
                || agencyId == null) {
            return false;
        }

        if (hasGrant(
                principal,
                permission,
                PermissionScope.GLOBAL
        )) {
            return true;
        }

        /*
         * UserAgencyAssignment will be implemented when
         * the real Agency aggregate is introduced in Phase 3.
         *
         * Until then, AGENCY access must never be granted
         * implicitly.
         */
        return false;
    }

    private boolean hasGrant(
            AuthenticatedUserPrincipal principal,
            PermissionCode permission,
            PermissionScope scope
    ) {
        if (principal == null
                || permission == null
                || scope == null) {
            return false;
        }

        return principal.permissions()
                .contains(
                        new PermissionGrant(
                                permission,
                                scope
                        )
                );
    }
}