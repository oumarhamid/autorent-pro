package com.autorentpro.identity.security;

import com.autorentpro.identity.application.AuthorizationDecisionService;
import com.autorentpro.identity.application.PermissionGrant;
import com.autorentpro.identity.domain.model.PermissionCode;
import com.autorentpro.identity.domain.model.PermissionScope;
import com.autorentpro.identity.domain.model.RoleCode;
import com.autorentpro.identity.infrastructure.security.AuthenticatedUserPrincipal;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AuthorizationDecisionServiceTest {

    private final AuthorizationDecisionService service =
            new AuthorizationDecisionService();

    @Test
    void selfPermissionAllowsOwnResource() {
        UUID userId = UUID.randomUUID();

        AuthenticatedUserPrincipal principal =
                principal(
                        userId,
                        new PermissionGrant(
                                PermissionCode.ACCOUNT_READ,
                                PermissionScope.SELF
                        )
                );

        assertThat(
                service.canAccessSelf(
                        principal,
                        PermissionCode.ACCOUNT_READ,
                        userId
                )
        ).isTrue();
    }

    @Test
    void selfPermissionRejectsAnotherUserResource() {
        AuthenticatedUserPrincipal principal =
                principal(
                        UUID.randomUUID(),
                        new PermissionGrant(
                                PermissionCode.ACCOUNT_READ,
                                PermissionScope.SELF
                        )
                );

        assertThat(
                service.canAccessSelf(
                        principal,
                        PermissionCode.ACCOUNT_READ,
                        UUID.randomUUID()
                )
        ).isFalse();
    }

    @Test
    void globalPermissionAllowsGlobalAccess() {
        AuthenticatedUserPrincipal principal =
                principal(
                        UUID.randomUUID(),
                        new PermissionGrant(
                                PermissionCode.USER_READ,
                                PermissionScope.GLOBAL
                        )
                );

        assertThat(
                service.canAccessGlobal(
                        principal,
                        PermissionCode.USER_READ
                )
        ).isTrue();
    }

    @Test
    void agencyPermissionDoesNotGrantAccessBeforeAgencyAssignmentsExist() {
        AuthenticatedUserPrincipal principal =
                principal(
                        UUID.randomUUID(),
                        new PermissionGrant(
                                PermissionCode.USER_READ,
                                PermissionScope.AGENCY
                        )
                );

        assertThat(
                service.canAccessAgency(
                        principal,
                        PermissionCode.USER_READ,
                        UUID.randomUUID()
                )
        ).isFalse();
    }

    @Test
    void globalPermissionAllowsAgencyResource() {
        AuthenticatedUserPrincipal principal =
                principal(
                        UUID.randomUUID(),
                        new PermissionGrant(
                                PermissionCode.USER_READ,
                                PermissionScope.GLOBAL
                        )
                );

        assertThat(
                service.canAccessAgency(
                        principal,
                        PermissionCode.USER_READ,
                        UUID.randomUUID()
                )
        ).isTrue();
    }

    @Test
    void missingPermissionIsDenied() {
        AuthenticatedUserPrincipal principal =
                principal(
                        UUID.randomUUID()
                );

        assertThat(
                service.canAccessGlobal(
                        principal,
                        PermissionCode.USER_DISABLE
                )
        ).isFalse();
    }

    @Test
    void nullPrincipalIsDenied() {
        assertThat(
                service.canAccessGlobal(
                        null,
                        PermissionCode.USER_READ
                )
        ).isFalse();
    }

    private AuthenticatedUserPrincipal principal(
            UUID userId,
            PermissionGrant... grants
    ) {
        return new AuthenticatedUserPrincipal(
                userId,
                "security-test@example.com",
                Set.of(RoleCode.CLIENT),
                Set.of(grants),
                false
        );
    }
}