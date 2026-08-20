package com.autorentpro;

import com.autorentpro.identity.application.IdentityAccessService;
import com.autorentpro.identity.application.PermissionGrant;
import com.autorentpro.identity.application.ResolvedIdentityAccess;
import com.autorentpro.identity.domain.model.PermissionCode;
import com.autorentpro.identity.domain.model.PermissionScope;
import com.autorentpro.identity.domain.model.Role;
import com.autorentpro.identity.domain.model.RoleCode;
import com.autorentpro.identity.domain.model.UserAccount;
import com.autorentpro.identity.domain.model.UserRole;
import com.autorentpro.identity.infrastructure.persistence.RoleRepository;
import com.autorentpro.identity.infrastructure.persistence.UserAccountRepository;
import com.autorentpro.identity.infrastructure.persistence.UserRoleRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("integration-test")
@Testcontainers
class AutorentBackendPostgresIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    UserAccountRepository userAccountRepository;

    @Autowired
    RoleRepository roleRepository;

    @Autowired
    UserRoleRepository userRoleRepository;

    @Autowired
    IdentityAccessService identityAccessService;

    @PersistenceContext
    EntityManager entityManager;

    @Test
    void flywayBaselineIsApplied() {
        Boolean success = jdbcTemplate.queryForObject(
                """
                SELECT success
                FROM flyway_schema_history
                WHERE version = '1'
                """,
                Boolean.class
        );

        assertThat(success).isTrue();
    }

    @Test
    void identityMigrationIsApplied() {
        Boolean success = jdbcTemplate.queryForObject(
                """
                SELECT success
                FROM flyway_schema_history
                WHERE version = '2'
                """,
                Boolean.class
        );

        assertThat(success).isTrue();
    }

    @Test
    void identityReferenceDataIsSeeded() {
        Long roles = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM identity_roles",
                Long.class
        );

        Long permissions = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM identity_permissions",
                Long.class
        );

        Long rolePermissions = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM identity_role_permissions",
                Long.class
        );

        assertThat(roles).isEqualTo(6L);
        assertThat(permissions).isEqualTo(9L);
        assertThat(rolePermissions).isEqualTo(21L);
    }

    @Test
    @Transactional
    void userAccountCanBePersistedWithGeneratedUuid() {
        UserAccount user = createUser(
                "integration@example.com"
        );

        entityManager.persist(user);
        entityManager.flush();

        assertThat(user.getId()).isNotNull();
        assertThat(user.getCreatedAt()).isNotNull();
        assertThat(user.getUpdatedAt()).isNotNull();
    }

    @Test
    @Transactional
    void userCanBeFoundByNormalizedEmail() {
        UserAccount user = UserAccount.create(
                "  Repository@Test.COM  ",
                "{bcrypt}integration-hash",
                false,
                Instant.parse("2026-08-18T12:00:00Z")
        );

        userAccountRepository.saveAndFlush(user);

        Optional<UserAccount> result =
                userAccountRepository.findByEmail(
                        "repository@test.com"
                );

        assertThat(result)
                .isPresent();

        assertThat(result.orElseThrow().getId())
                .isEqualTo(user.getId());
    }

    @Test
    @Transactional
    void databaseRejectsDuplicateNormalizedEmail() {
        UserAccount first = createUser(
                "duplicate@example.com"
        );

        UserAccount second = UserAccount.create(
                "  DUPLICATE@EXAMPLE.COM ",
                "{bcrypt}another-hash",
                false,
                Instant.parse("2026-08-18T12:01:00Z")
        );

        userAccountRepository.saveAndFlush(first);

        assertThatThrownBy(
                () -> userAccountRepository.saveAndFlush(second)
        ).isInstanceOf(
                DataIntegrityViolationException.class
        );
    }

    @Test
    @Transactional
    void resolvesAdminRolesPermissionsAndScopes() {
        UserAccount user = createUser(
                "admin-access@example.com"
        );

        userAccountRepository.saveAndFlush(user);

        Role admin = roleRepository.findByCode(
                RoleCode.ADMIN
        ).orElseThrow();

        userRoleRepository.saveAndFlush(
                UserRole.assign(user, admin)
        );

        ResolvedIdentityAccess access =
                identityAccessService.resolveForUser(
                        user.getId()
                );

        assertThat(access.roles())
                .containsExactly(RoleCode.ADMIN);

        assertThat(access.permissions())
                .hasSize(9)
                .contains(
                        new PermissionGrant(
                                PermissionCode.ACCOUNT_READ,
                                PermissionScope.SELF
                        ),
                        new PermissionGrant(
                                PermissionCode.USER_READ,
                                PermissionScope.GLOBAL
                        ),
                        new PermissionGrant(
                                PermissionCode.USER_ROLE_ASSIGN,
                                PermissionScope.GLOBAL
                        )
                );
    }

    @Test
    @Transactional
    void resolvesAgencyManagerScopedPermission() {
        UserAccount user = createUser(
                "agency-manager@example.com"
        );

        userAccountRepository.saveAndFlush(user);

        Role manager = roleRepository.findByCode(
                RoleCode.AGENCY_MANAGER
        ).orElseThrow();

        userRoleRepository.saveAndFlush(
                UserRole.assign(user, manager)
        );

        ResolvedIdentityAccess access =
                identityAccessService.resolveForUser(
                        user.getId()
                );

        assertThat(access.roles())
                .containsExactly(
                        RoleCode.AGENCY_MANAGER
                );

        assertThat(access.permissions())
                .containsExactlyInAnyOrder(
                        new PermissionGrant(
                                PermissionCode.ACCOUNT_READ,
                                PermissionScope.SELF
                        ),
                        new PermissionGrant(
                                PermissionCode.ACCOUNT_CHANGE_PASSWORD,
                                PermissionScope.SELF
                        ),
                        new PermissionGrant(
                                PermissionCode.USER_READ,
                                PermissionScope.AGENCY
                        )
                );
    }

    @Test
    void postgresQueryWorks() {
        Integer value = jdbcTemplate.queryForObject(
                "SELECT 1",
                Integer.class
        );

        assertThat(value).isEqualTo(1);
    }

    private UserAccount createUser(String email) {
        return UserAccount.create(
                email,
                "{bcrypt}integration-hash",
                false,
                Instant.parse("2026-08-18T12:00:00Z")
        );
    }
}