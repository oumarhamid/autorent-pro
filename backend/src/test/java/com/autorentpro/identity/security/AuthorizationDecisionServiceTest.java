package com.autorentpro.identity.security;

import com.autorentpro.identity.application.AgencyScopeMembershipResolver;
import com.autorentpro.identity.application.AuthorizationDecisionService;
import com.autorentpro.identity.application.IdentityAccessService;
import com.autorentpro.identity.application.PermissionGrant;
import com.autorentpro.identity.application.ResolvedIdentityAccess;
import com.autorentpro.identity.domain.model.PermissionCode;
import com.autorentpro.identity.domain.model.PermissionScope;
import com.autorentpro.identity.domain.model.RoleCode;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AuthorizationDecisionServiceTest {

    private final IdentityAccessService identityAccessService =
            mock(IdentityAccessService.class);

    private final AgencyScopeMembershipResolver
            agencyScopeMembershipResolver =
            mock(AgencyScopeMembershipResolver.class);

    private final AuthorizationDecisionService service =
            new AuthorizationDecisionService(
                    identityAccessService,
                    agencyScopeMembershipResolver
            );

    @Test
    void selfPermissionAllowsOwnResource() {
        UUID userId =
                UUID.randomUUID();

        when(
                identityAccessService.resolveForUser(
                        userId
                )
        ).thenReturn(
                access(
                        Set.of(RoleCode.CLIENT),
                        new PermissionGrant(
                                PermissionCode.ACCOUNT_READ,
                                PermissionScope.SELF
                        )
                )
        );

        assertThat(
                service.canAccessSelf(
                        userId,
                        PermissionCode.ACCOUNT_READ,
                        userId
                )
        ).isTrue();
    }

    @Test
    void selfPermissionRejectsAnotherUserResource() {
        UUID authenticatedUserId =
                UUID.randomUUID();

        when(
                identityAccessService.resolveForUser(
                        authenticatedUserId
                )
        ).thenReturn(
                access(
                        Set.of(RoleCode.CLIENT),
                        new PermissionGrant(
                                PermissionCode.ACCOUNT_READ,
                                PermissionScope.SELF
                        )
                )
        );

        assertThat(
                service.canAccessSelf(
                        authenticatedUserId,
                        PermissionCode.ACCOUNT_READ,
                        UUID.randomUUID()
                )
        ).isFalse();
    }

    @Test
    void globalPermissionAllowsGlobalAccess() {
        UUID userId =
                UUID.randomUUID();

        when(
                identityAccessService.resolveForUser(
                        userId
                )
        ).thenReturn(
                access(
                        Set.of(RoleCode.MANAGER),
                        new PermissionGrant(
                                PermissionCode.USER_READ,
                                PermissionScope.GLOBAL
                        )
                )
        );

        assertThat(
                service.canAccessGlobal(
                        userId,
                        PermissionCode.USER_READ
                )
        ).isTrue();
    }

    @Test
    void agencyPermissionAllowsActiveMembership() {
        UUID userId =
                UUID.randomUUID();

        UUID agencyId =
                UUID.randomUUID();

        when(
                identityAccessService.resolveForUser(
                        userId
                )
        ).thenReturn(
                access(
                        Set.of(RoleCode.AGENCY_MANAGER),
                        new PermissionGrant(
                                PermissionCode.USER_READ,
                                PermissionScope.AGENCY
                        )
                )
        );

        when(
                agencyScopeMembershipResolver
                        .hasActiveMembership(
                                userId,
                                agencyId
                        )
        ).thenReturn(true);

        assertThat(
                service.canAccessAgency(
                        userId,
                        PermissionCode.USER_READ,
                        agencyId
                )
        ).isTrue();
    }

    @Test
    void agencyPermissionRejectsMissingMembership() {
        UUID userId =
                UUID.randomUUID();

        UUID agencyId =
                UUID.randomUUID();

        when(
                identityAccessService.resolveForUser(
                        userId
                )
        ).thenReturn(
                access(
                        Set.of(RoleCode.AGENCY_MANAGER),
                        new PermissionGrant(
                                PermissionCode.USER_READ,
                                PermissionScope.AGENCY
                        )
                )
        );

        when(
                agencyScopeMembershipResolver
                        .hasActiveMembership(
                                userId,
                                agencyId
                        )
        ).thenReturn(false);

        assertThat(
                service.canAccessAgency(
                        userId,
                        PermissionCode.USER_READ,
                        agencyId
                )
        ).isFalse();
    }

    @Test
    void globalPermissionAllowsAgencyResourceWithoutMembershipLookup() {
        UUID userId =
                UUID.randomUUID();

        when(
                identityAccessService.resolveForUser(
                        userId
                )
        ).thenReturn(
                access(
                        Set.of(RoleCode.MANAGER),
                        new PermissionGrant(
                                PermissionCode.USER_READ,
                                PermissionScope.GLOBAL
                        )
                )
        );

        assertThat(
                service.canAccessAgency(
                        userId,
                        PermissionCode.USER_READ,
                        UUID.randomUUID()
                )
        ).isTrue();

        verifyNoInteractions(
                agencyScopeMembershipResolver
        );
    }

    @Test
    void membershipWithoutAgencyPermissionIsDenied() {
        UUID userId =
                UUID.randomUUID();

        when(
                identityAccessService.resolveForUser(
                        userId
                )
        ).thenReturn(
                access(
                        Set.of(RoleCode.AGENCY_MANAGER)
                )
        );

        assertThat(
                service.canAccessAgency(
                        userId,
                        PermissionCode.USER_READ,
                        UUID.randomUUID()
                )
        ).isFalse();

        verifyNoInteractions(
                agencyScopeMembershipResolver
        );
    }

    @Test
    void membershipResolutionFailureIsDenied() {
        UUID userId =
                UUID.randomUUID();

        UUID agencyId =
                UUID.randomUUID();

        when(
                identityAccessService.resolveForUser(
                        userId
                )
        ).thenReturn(
                access(
                        Set.of(RoleCode.AGENCY_MANAGER),
                        new PermissionGrant(
                                PermissionCode.USER_READ,
                                PermissionScope.AGENCY
                        )
                )
        );

        when(
                agencyScopeMembershipResolver
                        .hasActiveMembership(
                                userId,
                                agencyId
                        )
        ).thenThrow(
                new IllegalStateException(
                        "membership lookup unavailable"
                )
        );

        assertThat(
                service.canAccessAgency(
                        userId,
                        PermissionCode.USER_READ,
                        agencyId
                )
        ).isFalse();
    }

    @Test
    void missingPermissionIsDenied() {
        UUID userId =
                UUID.randomUUID();

        when(
                identityAccessService.resolveForUser(
                        userId
                )
        ).thenReturn(
                access(
                        Set.of(RoleCode.CLIENT)
                )
        );

        assertThat(
                service.canAccessGlobal(
                        userId,
                        PermissionCode.USER_DISABLE
                )
        ).isFalse();
    }

    @Test
    void nullAuthenticatedUserIsDenied() {
        assertThat(
                service.canAccessGlobal(
                        null,
                        PermissionCode.USER_READ
                )
        ).isFalse();
    }

    private ResolvedIdentityAccess access(
            Set<RoleCode> roles,
            PermissionGrant... permissions
    ) {
        return new ResolvedIdentityAccess(
                roles,
                Set.of(permissions)
        );
    }
}