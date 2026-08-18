package com.autorentpro.identity.security;

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
import static org.mockito.Mockito.when;

class AuthorizationDecisionServiceTest {

    private final IdentityAccessService identityAccessService =
            mock(IdentityAccessService.class);

    private final AuthorizationDecisionService service =
            new AuthorizationDecisionService(
                    identityAccessService
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
    void agencyPermissionDoesNotGrantAccessBeforeAgencyAssignmentsExist() {
        UUID userId =
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

        assertThat(
                service.canAccessAgency(
                        userId,
                        PermissionCode.USER_READ,
                        UUID.randomUUID()
                )
        ).isFalse();
    }

    @Test
    void globalPermissionAllowsAgencyResource() {
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
            PermissionGrant... grants
    ) {
        return new ResolvedIdentityAccess(
                roles,
                Set.of(grants)
        );
    }
}