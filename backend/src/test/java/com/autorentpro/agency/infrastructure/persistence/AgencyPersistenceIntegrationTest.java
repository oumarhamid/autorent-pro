package com.autorentpro.agency.infrastructure.persistence;

import com.autorentpro.agency.domain.model.Agency;
import com.autorentpro.agency.domain.repository.AgencyRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("integration-test")
@Testcontainers
class AgencyPersistenceIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    AgencyRepository agencyRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @PersistenceContext
    EntityManager entityManager;

    @Test
    void agencyMigrationIsApplied() {
        Boolean success = jdbcTemplate.queryForObject(
                """
                SELECT success
                FROM flyway_schema_history
                WHERE version = '3'
                """,
                Boolean.class
        );

        assertThat(success).isTrue();
    }

    @Test
    @Transactional
    void agencyCanBePersistedAndReloadedThroughDomainRepository() {
        Agency agency = Agency.create(
                " casa-centre ",
                "Casablanca Centre",
                "1 Avenue Hassan II",
                null,
                "Casablanca",
                "20000",
                "ma",
                "+212500000000",
                " CENTRE@AUTORENT.MA ",
                "Africa/Casablanca"
        );

        Agency saved = agencyRepository.save(agency);

        entityManager.flush();
        entityManager.clear();

        Optional<Agency> result =
                agencyRepository.findById(saved.getId());

        assertThat(result).isPresent();

        Agency reloaded = result.orElseThrow();

        assertThat(reloaded.getId())
                .isEqualTo(saved.getId());

        assertThat(reloaded.getCode())
                .isEqualTo("CASA-CENTRE");

        assertThat(reloaded.getCountryCode())
                .isEqualTo("MA");

        assertThat(reloaded.getEmail())
                .isEqualTo("centre@autorent.ma");

        assertThat(reloaded.getCreatedAt())
                .isNotNull();

        assertThat(reloaded.getUpdatedAt())
                .isNotNull();
    }

    @Test
    @Transactional
    void agencyCanBeFoundByBusinessCode() {
        Agency agency = createAgency(
                "RABAT-AGDAL",
                "Rabat Agdal"
        );

        Agency saved = agencyRepository.save(agency);

        entityManager.flush();
        entityManager.clear();

        Optional<Agency> result =
                agencyRepository.findByCode("RABAT-AGDAL");

        assertThat(result)
                .isPresent();

        assertThat(result.orElseThrow().getId())
                .isEqualTo(saved.getId());

        assertThat(
                agencyRepository.existsByCode("RABAT-AGDAL")
        ).isTrue();
    }

    @Test
    @Transactional
    void databaseRejectsDuplicateAgencyCode() {
        Agency first = createAgency(
                "CASA-AIN-DIAB",
                "Casablanca Aïn Diab"
        );

        Agency second = createAgency(
                "CASA-AIN-DIAB",
                "Casablanca Aïn Diab Bis"
        );

        agencyRepository.save(first);
        entityManager.flush();

        agencyRepository.save(second);

        assertThatThrownBy(entityManager::flush)
                .isInstanceOf(
                        ConstraintViolationException.class
                );
    }

    private static Agency createAgency(
            String code,
            String name
    ) {
        return Agency.create(
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
    }
}