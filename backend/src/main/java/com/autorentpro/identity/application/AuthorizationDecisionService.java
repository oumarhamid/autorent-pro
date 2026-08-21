package com.autorentpro.identity.application;

import com.autorentpro.identity.domain.model.PermissionCode;
import com.autorentpro.identity.domain.model.PermissionScope;
import com.autorentpro.identity.domain.model.RoleCode;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.UUID;

@Service("authorizationDecisionService")
public class AuthorizationDecisionService {

    private final IdentityAccessService
            identityAccessService;

    private final AgencyScopeMembershipResolver
            agencyScopeMembershipResolver;

    public AuthorizationDecisionService(
            IdentityAccessService identityAccessService,
            AgencyScopeMembershipResolver
                    agencyScopeMembershipResolver
    ) {
        this.identityAccessService =
                identityAccessService;

        this.agencyScopeMembershipResolver =
                agencyScopeMembershipResolver;
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

        /*
         * GLOBAL always dominates an agency-scoped
         * membership requirement.
         */
        if (hasGrant(
                access,
                permission,
                PermissionScope.GLOBAL
        )) {
            return true;
        }

        /*
         * Membership alone never grants access.
         * The user must first possess the requested
         * permission with AGENCY scope.
         */
        if (!hasGrant(
                access,
                permission,
                PermissionScope.AGENCY
        )) {
            return false;
        }

        /*
         * Agency membership is resolved from the
         * current source of truth.
         *
         * Authorization must fail closed if the
         * membership resolver cannot provide a
         * reliable answer.
         */
        try {
            return agencyScopeMembershipResolver
                    .hasActiveMembership(
                            authenticatedUserId,
                            agencyId
                    );
        } catch (RuntimeException exception) {
            return false;
        }
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