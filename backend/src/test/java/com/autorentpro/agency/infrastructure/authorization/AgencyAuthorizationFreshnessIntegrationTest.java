package com.autorentpro.agency.infrastructure.authorization;

import com.autorentpro.agency.domain.model.Agency;
import com.autorentpro.agency.domain.model.UserAgencyAssignment;
import com.autorentpro.agency.domain.repository.AgencyRepository;
import com.autorentpro.agency.domain.repository.UserAgencyAssignmentRepository;
import com.autorentpro.identity.application.AuthorizationDecisionService;
import com.autorentpro.identity.application.IdentityAccessService;
import com.autorentpro.identity.application.PermissionGrant;
import com.autorentpro.identity.application.ResolvedIdentityAccess;
import com.autorentpro.identity.domain.model.PermissionCode;
import com.autorentpro.identity.domain.model.PermissionScope;
import com.autorentpro.identity.domain.model.RoleCode;
import com.autorentpro.identity.domain.model.UserAccount;
import com.autorentpro.identity.infrastructure.persistence.UserAccountRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("integration-test")
@Testcontainers
class AgencyAuthorizationFreshnessIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(
                    "postgres:17-alpine"
            );

    @Autowired
    AuthorizationDecisionService
            authorizationDecisionService;

    @Autowired
    UserAgencyAssignmentRepository
            assignmentRepository;

    @Autowired
    AgencyRepository
            agencyRepository;

    @Autowired
    UserAccountRepository
            userAccountRepository;

    @MockBean
    IdentityAccessService
            identityAccessService;

    @PersistenceContext
    EntityManager entityManager;

    @Test
    @Transactional
    void agencyAuthorizationReflectsMembershipDeactivationImmediately() {
        UserAccount user =
                createUser(
                        "agency.authorization.freshness@example.com"
                );

        Agency agency =
                createAgency(
                        "AUTH-FRESHNESS",
                        "Authorization Freshness"
                );

        UUID userId =
                user.getId();

        UUID agencyId =
                agency.getId();

        when(
                identityAccessService.resolveForUser(
                        userId
                )
        ).thenReturn(
                new ResolvedIdentityAccess(
                        Set.of(
                                RoleCode.AGENCY_MANAGER
                        ),
                        Set.of(
                                new PermissionGrant(
                                        PermissionCode.USER_READ,
                                        PermissionScope.AGENCY
                                )
                        )
                )
        );

        UserAgencyAssignment assignment =
                UserAgencyAssignment.assign(
                        userId,
                        agencyId
                );

        assignmentRepository.saveAndFlush(
                assignment
        );

        entityManager.clear();

        assertThat(
                authorizationDecisionService
                        .canAccessAgency(
                                userId,
                                PermissionCode.USER_READ,
                                agencyId
                        )
        ).isTrue();

        UserAgencyAssignment persistedAssignment =
                assignmentRepository
                        .findByUserIdAndAgencyId(
                                userId,
                                agencyId
                        )
                        .orElseThrow();

        persistedAssignment.deactivate();

        assignmentRepository.saveAndFlush(
                persistedAssignment
        );

        entityManager.clear();

        assertThat(
                authorizationDecisionService
                        .canAccessAgency(
                                userId,
                                PermissionCode.USER_READ,
                                agencyId
                        )
        ).isFalse();
    }

    private UserAccount createUser(
            String email
    ) {
        UserAccount user =
                UserAccount.create(
                        email,
                        "integration-test-password-hash",
                        false,
                        Instant.now()
                );

        return userAccountRepository
                .saveAndFlush(
                        user
                );
    }

    private Agency createAgency(
            String code,
            String name
    ) {
        Agency agency =
                Agency.create(
                        code,
                        name,
                        "10 Avenue Principale",
                        null,
                        "Casablanca",
                        "20000",
                        "MA",
                        null,
                        null,
                        "Africa/Casablanca"
                );

        return agencyRepository
                .saveAndFlush(
                        agency
                );
    }
}