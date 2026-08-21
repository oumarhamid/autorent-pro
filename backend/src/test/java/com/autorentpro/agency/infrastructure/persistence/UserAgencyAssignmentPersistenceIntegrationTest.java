package com.autorentpro.agency.infrastructure.persistence;

import com.autorentpro.agency.domain.model.Agency;
import com.autorentpro.agency.domain.model.UserAgencyAssignment;
import com.autorentpro.agency.domain.repository.AgencyRepository;
import com.autorentpro.agency.domain.repository.UserAgencyAssignmentRepository;
import com.autorentpro.identity.domain.model.UserAccount;
import com.autorentpro.identity.infrastructure.persistence.UserAccountRepository;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("integration-test")
@Testcontainers
class UserAgencyAssignmentPersistenceIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    UserAgencyAssignmentRepository assignmentRepository;

    @Autowired
    AgencyRepository agencyRepository;

    @Autowired
    UserAccountRepository userAccountRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @PersistenceContext
    EntityManager entityManager;

    @Test
    void userAgencyAssignmentMigrationIsApplied() {
        Boolean success = jdbcTemplate.queryForObject(
                """
                SELECT success
                FROM flyway_schema_history
                WHERE version = '6'
                """,
                Boolean.class
        );

        assertThat(success).isTrue();
    }

    @Test
    @Transactional
    void assignmentCanBePersistedAndReloaded() {
        UserAccount user = createUser(
                "assignment.persist@example.com"
        );

        Agency agency = createAgency(
                "ASSIGN-PERSIST",
                "Assignment Persistence"
        );

        UserAgencyAssignment assignment =
                UserAgencyAssignment.assign(
                        user.getId(),
                        agency.getId()
                );

        UserAgencyAssignment saved =
                assignmentRepository.saveAndFlush(
                        assignment
                );

        entityManager.clear();

        Optional<UserAgencyAssignment> result =
                assignmentRepository.findByUserIdAndAgencyId(
                        user.getId(),
                        agency.getId()
                );

        assertThat(result).isPresent();

        UserAgencyAssignment reloaded =
                result.orElseThrow();

        assertThat(reloaded.getId())
                .isEqualTo(saved.getId());

        assertThat(reloaded.getUserId())
                .isEqualTo(user.getId());

        assertThat(reloaded.getAgencyId())
                .isEqualTo(agency.getId());

        assertThat(reloaded.isActive()).isTrue();
        assertThat(reloaded.getEndedAt()).isNull();

        assertThat(
                assignmentRepository
                        .existsActiveByUserIdAndAgencyId(
                                user.getId(),
                                agency.getId()
                        )
        ).isTrue();
    }

    @Test
    @Transactional
    void inactiveAssignmentIsExcludedFromActiveQueries() {
        UserAccount user = createUser(
                "assignment.inactive@example.com"
        );

        Agency agency = createAgency(
                "ASSIGN-INACTIVE",
                "Assignment Inactive"
        );

        UserAgencyAssignment assignment =
                UserAgencyAssignment.assign(
                        user.getId(),
                        agency.getId()
                );

        assignmentRepository.saveAndFlush(
                assignment
        );

        assignment.deactivate();

        assignmentRepository.saveAndFlush(
                assignment
        );

        entityManager.clear();

        assertThat(
                assignmentRepository
                        .existsActiveByUserIdAndAgencyId(
                                user.getId(),
                                agency.getId()
                        )
        ).isFalse();

        List<UserAgencyAssignment> activeAssignments =
                assignmentRepository
                        .findAllActiveByAgencyId(
                                agency.getId()
                        );

        assertThat(activeAssignments).isEmpty();
    }

    @Test
    @Transactional
    void assignmentCanBeLoadedForUpdate() {
        UserAccount user = createUser(
                "assignment.lock@example.com"
        );

        Agency agency = createAgency(
                "ASSIGN-LOCK",
                "Assignment Lock"
        );

        UserAgencyAssignment assignment =
                UserAgencyAssignment.assign(
                        user.getId(),
                        agency.getId()
                );

        assignmentRepository.saveAndFlush(
                assignment
        );

        entityManager.clear();

        Optional<UserAgencyAssignment> result =
                assignmentRepository
                        .findForUpdateByUserIdAndAgencyId(
                                user.getId(),
                                agency.getId()
                        );

        assertThat(result).isPresent();

        assertThat(result.orElseThrow().getId())
                .isEqualTo(assignment.getId());
    }

    @Test
    @Transactional
    void databaseRejectsDuplicateUserAgencyAssignment() {
        UserAccount user = createUser(
                "assignment.duplicate@example.com"
        );

        Agency agency = createAgency(
                "ASSIGN-DUPLICATE",
                "Assignment Duplicate"
        );

        UserAgencyAssignment first =
                UserAgencyAssignment.assign(
                        user.getId(),
                        agency.getId()
                );

        UserAgencyAssignment second =
                UserAgencyAssignment.assign(
                        user.getId(),
                        agency.getId()
                );

        assignmentRepository.saveAndFlush(first);

        assertThatThrownBy(
                () -> assignmentRepository.saveAndFlush(second)
        ).isInstanceOf(
                DataIntegrityViolationException.class
        );
    }

    private UserAccount createUser(
            String email
    ) {
        UserAccount user = UserAccount.create(
                email,
                "integration-test-password-hash",
                false,
                Instant.now()
        );

        return userAccountRepository.saveAndFlush(
                user
        );
    }

    private Agency createAgency(
            String code,
            String name
    ) {
        Agency agency = Agency.create(
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

        return agencyRepository.saveAndFlush(
                agency
        );
    }
}