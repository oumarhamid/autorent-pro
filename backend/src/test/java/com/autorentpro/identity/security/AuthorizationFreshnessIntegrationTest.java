package com.autorentpro.identity.security;

import com.autorentpro.identity.domain.model.Role;
import com.autorentpro.identity.domain.model.RoleCode;
import com.autorentpro.identity.domain.model.UserAccount;
import com.autorentpro.identity.domain.model.UserRole;
import com.autorentpro.identity.infrastructure.persistence.RoleRepository;
import com.autorentpro.identity.infrastructure.persistence.UserAccountRepository;
import com.autorentpro.identity.infrastructure.persistence.UserRoleRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration-test")
@Testcontainers
class AuthorizationFreshnessIntegrationTest {

    private static final String PASSWORD =
            "correct horse battery staple";

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(
                    "postgres:17-alpine"
            );

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    UserAccountRepository userAccountRepository;

    @Autowired
    UserRoleRepository userRoleRepository;

    @Autowired
    RoleRepository roleRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void revokedRoleStopsAuthorizingExistingSessionImmediately()
            throws Exception {

        UserAccount admin =
                createUserWithRole(
                        "fresh-authorization-admin@example.com",
                        RoleCode.ADMIN
                );

        MockHttpSession session =
                login(admin.getEmail());

        mockMvc.perform(
                        get("/api/users")
                                .param("page", "0")
                                .param("size", "1")
                                .session(session)
                )
                .andExpect(
                        status().isOk()
                );

        int deleted =
                jdbcTemplate.update(
                        """
                        delete from identity_user_roles
                        where user_id = ?
                        """,
                        admin.getId()
                );

        assertThat(deleted)
                .isEqualTo(1);

        mockMvc.perform(
                        get("/api/users")
                                .param("page", "0")
                                .param("size", "1")
                                .session(session)
                )
                .andExpect(
                        status().isForbidden()
                )
                .andExpect(
                        jsonPath("$.code")
                                .value("ACCESS_DENIED")
                );
    }

    private UserAccount createUserWithRole(
            String email,
            RoleCode roleCode
    ) {
        UserAccount user =
                UserAccount.create(
                        email,
                        passwordEncoder.encode(
                                PASSWORD
                        ),
                        false,
                        Instant.now()
                );

        userAccountRepository
                .saveAndFlush(user);

        Role role =
                roleRepository
                        .findByCode(roleCode)
                        .orElseThrow();

        userRoleRepository
                .saveAndFlush(
                        UserRole.assign(
                                user,
                                role
                        )
                );

        return user;
    }

    private MockHttpSession login(
            String email
    ) throws Exception {

        MvcResult csrfResult =
                mockMvc.perform(
                                get("/api/auth/csrf")
                        )
                        .andExpect(
                                status().isOk()
                        )
                        .andReturn();

        JsonNode csrf =
                objectMapper.readTree(
                        csrfResult
                                .getResponse()
                                .getContentAsString()
                );

        MockHttpSession session =
                (MockHttpSession)
                        csrfResult
                                .getRequest()
                                .getSession(false);

        MvcResult loginResult =
                mockMvc.perform(
                                post("/api/auth/login")
                                        .session(session)
                                        .header(
                                                csrf.get(
                                                        "headerName"
                                                ).asText(),
                                                csrf.get(
                                                        "token"
                                                ).asText()
                                        )
                                        .contentType(
                                                MediaType.APPLICATION_JSON
                                        )
                                        .content(
                                                objectMapper.writeValueAsString(
                                                        Map.of(
                                                                "email",
                                                                email,
                                                                "password",
                                                                PASSWORD
                                                        )
                                                )
                                        )
                        )
                        .andExpect(
                                status().isOk()
                        )
                        .andReturn();

        return (MockHttpSession)
                loginResult
                        .getRequest()
                        .getSession(false);
    }
}