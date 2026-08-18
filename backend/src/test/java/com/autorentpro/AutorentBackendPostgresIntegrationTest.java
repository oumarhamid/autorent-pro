package com.autorentpro;

import com.autorentpro.identity.domain.model.UserAccount;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

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
        UserAccount user = UserAccount.create(
                "integration@example.com",
                "{bcrypt}integration-hash",
                false,
                Instant.parse("2026-08-18T12:00:00Z")
        );

        entityManager.persist(user);
        entityManager.flush();

        assertThat(user.getId()).isNotNull();
        assertThat(user.getCreatedAt()).isNotNull();
        assertThat(user.getUpdatedAt()).isNotNull();
    }

    @Test
    void postgresQueryWorks() {
        Integer value = jdbcTemplate.queryForObject(
                "SELECT 1",
                Integer.class
        );

        assertThat(value).isEqualTo(1);
    }
}