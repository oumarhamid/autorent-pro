package com.autorentpro;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.containers.PostgreSQLContainer;

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

    @Test
    void flywayBaselineIsApplied() {
        Boolean success = jdbcTemplate.queryForObject(
                "SELECT success FROM flyway_schema_history WHERE version = '1'",
                Boolean.class
        );

        assertThat(success).isTrue();
    }

    @Test
    void postgresQueryWorks() {
        Integer value = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        assertThat(value).isEqualTo(1);
    }
}
