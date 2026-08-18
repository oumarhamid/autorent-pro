package com.autorentpro.identity.security;

import com.autorentpro.identity.application.InitialAdminBootstrapService;
import com.autorentpro.identity.application.InitialAdminBootstrapService.BootstrapResult;
import com.autorentpro.identity.domain.model.RoleCode;
import com.autorentpro.identity.domain.model.UserAccount;
import com.autorentpro.identity.infrastructure.persistence.UserAccountRepository;
import com.autorentpro.identity.infrastructure.persistence.UserRoleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        properties = {
                "autorent.bootstrap.admin.enabled=true",
                "autorent.bootstrap.admin.email=bootstrap.admin@example.com",
                "autorent.bootstrap.admin.password=Bootstrap secure password 2026!"
        }
)
@ActiveProfiles("integration-test")
@Testcontainers
class InitialAdminBootstrapIntegrationTest {

    private static final String EMAIL =
            "bootstrap.admin@example.com";

    private static final String PASSWORD =
            "Bootstrap secure password 2026!";

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(
                    "postgres:17-alpine"
            );

    @Autowired
    UserAccountRepository userAccountRepository;

    @Autowired
    UserRoleRepository userRoleRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    InitialAdminBootstrapService bootstrapService;

    @Test
    void enabledBootstrapCreatesInitialAdministrator() {
        UserAccount administrator =
                userAccountRepository
                        .findByEmail(EMAIL)
                        .orElseThrow();

        assertThat(
                administrator.isActive()
        ).isTrue();

        assertThat(
                administrator.isMustChangePassword()
        ).isTrue();

        assertThat(
                passwordEncoder.matches(
                        PASSWORD,
                        administrator.getPasswordHash()
                )
        ).isTrue();

        assertThat(
                userRoleRepository
                        .findRoleCodesByUserId(
                                administrator.getId()
                        )
        ).containsExactly(
                RoleCode.ADMIN
        );

        assertThat(
                userRoleRepository
                        .countByRoleCode(
                                RoleCode.ADMIN
                        )
        ).isEqualTo(1);
    }

    @Test
    void bootstrapIsIdempotentWhenAdministratorAlreadyExists() {
        BootstrapResult result =
                bootstrapService.bootstrap(
                        "another.admin@example.com",
                        "Another secure bootstrap password 2026!"
                );

        assertThat(result)
                .isEqualTo(
                        BootstrapResult.ALREADY_EXISTS
                );

        assertThat(
                userAccountRepository.existsByEmail(
                        "another.admin@example.com"
                )
        ).isFalse();

        assertThat(
                userRoleRepository
                        .countByRoleCode(
                                RoleCode.ADMIN
                        )
        ).isEqualTo(1);
    }
}