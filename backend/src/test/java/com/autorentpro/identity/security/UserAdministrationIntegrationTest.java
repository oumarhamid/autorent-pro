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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration-test")
@Testcontainers
class UserAdministrationIntegrationTest {

    private static final String PASSWORD =
            "correct horse battery staple";

    private static final String TEMP_PASSWORD =
            "temporary secure rental password";

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

    @Test
    void adminCanCreateUserWithInitialRole()
            throws Exception {

        MockHttpSession adminSession =
                createAndLogin(
                        "create-admin@example.com",
                        RoleCode.ADMIN
                );

        CsrfSession csrf =
                obtainCsrf(adminSession);

        MvcResult result =
                mockMvc.perform(
                                post("/api/users")
                                        .session(adminSession)
                                        .header(
                                                csrf.headerName(),
                                                csrf.token()
                                        )
                                        .contentType(
                                                MediaType.APPLICATION_JSON
                                        )
                                        .content(
                                                objectMapper.writeValueAsString(
                                                        Map.of(
                                                                "email",
                                                                "  NEW.USER@EXAMPLE.COM  ",
                                                                "temporaryPassword",
                                                                TEMP_PASSWORD,
                                                                "initialRole",
                                                                "CLIENT"
                                                        )
                                                )
                                        )
                        )
                        .andExpect(
                                status().isCreated()
                        )
                        .andExpect(
                                jsonPath("$.email")
                                        .value(
                                                "new.user@example.com"
                                        )
                        )
                        .andExpect(
                                jsonPath("$.status")
                                        .value("ACTIVE")
                        )
                        .andExpect(
                                jsonPath("$.mustChangePassword")
                                        .value(true)
                        )
                        .andExpect(
                                jsonPath("$.roles",
                                        hasItem("CLIENT"))
                        )
                        .andExpect(
                                jsonPath("$.passwordHash")
                                        .doesNotExist()
                        )
                        .andReturn();

        JsonNode json =
                objectMapper.readTree(
                        result.getResponse()
                                .getContentAsString()
                );

        UUID createdUserId =
                UUID.fromString(
                        json.get("id").asText()
                );

        UserAccount persisted =
                userAccountRepository
                        .findById(createdUserId)
                        .orElseThrow();

        assertThat(
                passwordEncoder.matches(
                        TEMP_PASSWORD,
                        persisted.getPasswordHash()
                )
        ).isTrue();

        assertThat(
                userRoleRepository
                        .findRoleCodesByUserId(
                                createdUserId
                        )
        ).containsExactly(
                RoleCode.CLIENT
        );
    }

    @Test
    void duplicateEmailReturns409()
            throws Exception {

        createUser(
                "duplicate-user@example.com",
                RoleCode.CLIENT
        );

        MockHttpSession adminSession =
                createAndLogin(
                        "duplicate-admin@example.com",
                        RoleCode.ADMIN
                );

        CsrfSession csrf =
                obtainCsrf(adminSession);

        mockMvc.perform(
                        post("/api/users")
                                .session(adminSession)
                                .header(
                                        csrf.headerName(),
                                        csrf.token()
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        objectMapper.writeValueAsString(
                                                Map.of(
                                                        "email",
                                                        "DUPLICATE-USER@EXAMPLE.COM",
                                                        "temporaryPassword",
                                                        TEMP_PASSWORD,
                                                        "initialRole",
                                                        "CLIENT"
                                                )
                                        )
                                )
                )
                .andExpect(
                        status().isConflict()
                )
                .andExpect(
                        jsonPath("$.code")
                                .value(
                                        "EMAIL_ALREADY_IN_USE"
                                )
                );
    }

    @Test
    void managerCanReadAnyUser()
            throws Exception {

        UserAccount target =
                createUser(
                        "read-target@example.com",
                        RoleCode.CLIENT
                );

        MockHttpSession managerSession =
                createAndLogin(
                        "read-manager@example.com",
                        RoleCode.MANAGER
                );

        mockMvc.perform(
                        get(
                                "/api/users/{userId}",
                                target.getId()
                        )
                                .session(managerSession)
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.id")
                                .value(
                                        target.getId().toString()
                                )
                )
                .andExpect(
                        jsonPath("$.email")
                                .value(
                                        "read-target@example.com"
                                )
                )
                .andExpect(
                        jsonPath("$.passwordHash")
                                .doesNotExist()
                );
    }

    @Test
    void clientCannotReadAnotherUser()
            throws Exception {

        UserAccount target =
                createUser(
                        "forbidden-target@example.com",
                        RoleCode.CLIENT
                );

        MockHttpSession clientSession =
                createAndLogin(
                        "forbidden-client@example.com",
                        RoleCode.CLIENT
                );

        mockMvc.perform(
                        get(
                                "/api/users/{userId}",
                                target.getId()
                        )
                                .session(clientSession)
                )
                .andExpect(
                        status().isForbidden()
                )
                .andExpect(
                        jsonPath("$.code")
                                .value("ACCESS_DENIED")
                );
    }

    @Test
    void agencyManagerCannotUseAgencyPermissionAsGlobal()
            throws Exception {

        UserAccount target =
                createUser(
                        "agency-target@example.com",
                        RoleCode.CLIENT
                );

        MockHttpSession agencyManagerSession =
                createAndLogin(
                        "agency-manager@example.com",
                        RoleCode.AGENCY_MANAGER
                );

        mockMvc.perform(
                        get(
                                "/api/users/{userId}",
                                target.getId()
                        )
                                .session(
                                        agencyManagerSession
                                )
                )
                .andExpect(
                        status().isForbidden()
                );
    }

    @Test
    void managerCanListUsersWithPagination()
            throws Exception {

        createUser(
                "list-user-one@example.com",
                RoleCode.CLIENT
        );

        createUser(
                "list-user-two@example.com",
                RoleCode.CLIENT
        );

        MockHttpSession managerSession =
                createAndLogin(
                        "list-manager@example.com",
                        RoleCode.MANAGER
                );

        mockMvc.perform(
                        get("/api/users")
                                .param("page", "0")
                                .param("size", "20")
                                .session(managerSession)
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.items")
                                .isArray()
                )
                .andExpect(
                        jsonPath("$.page")
                                .value(0)
                )
                .andExpect(
                        jsonPath("$.size")
                                .value(20)
                )
                .andExpect(
                        jsonPath("$.totalElements")
                                .isNumber()
                )
                .andExpect(
                        jsonPath(
                                "$.items[0].passwordHash"
                        ).doesNotExist()
                );
    }

    @Test
    void nonAdminCannotCreateUser()
            throws Exception {

        MockHttpSession managerSession =
                createAndLogin(
                        "create-forbidden-manager@example.com",
                        RoleCode.MANAGER
                );

        CsrfSession csrf =
                obtainCsrf(managerSession);

        mockMvc.perform(
                        post("/api/users")
                                .session(managerSession)
                                .header(
                                        csrf.headerName(),
                                        csrf.token()
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        objectMapper.writeValueAsString(
                                                Map.of(
                                                        "email",
                                                        "should-not-exist@example.com",
                                                        "temporaryPassword",
                                                        TEMP_PASSWORD,
                                                        "initialRole",
                                                        "CLIENT"
                                                )
                                        )
                                )
                )
                .andExpect(
                        status().isForbidden()
                );

        assertThat(
                userAccountRepository.existsByEmail(
                        "should-not-exist@example.com"
                )
        ).isFalse();
    }

    @Test
    void unknownUserReturns404ForAuthorizedManager()
            throws Exception {

        MockHttpSession managerSession =
                createAndLogin(
                        "not-found-manager@example.com",
                        RoleCode.MANAGER
                );

        mockMvc.perform(
                        get(
                                "/api/users/{userId}",
                                UUID.randomUUID()
                        )
                                .session(managerSession)
                )
                .andExpect(
                        status().isNotFound()
                )
                .andExpect(
                        jsonPath("$.code")
                                .value("USER_NOT_FOUND")
                );
    }

    private MockHttpSession createAndLogin(
            String email,
            RoleCode role
    ) throws Exception {

        createUser(
                email,
                role
        );

        CsrfSession csrf =
                obtainCsrf();

        MvcResult result =
                mockMvc.perform(
                                post("/api/auth/login")
                                        .session(
                                                csrf.session()
                                        )
                                        .header(
                                                csrf.headerName(),
                                                csrf.token()
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
                result.getRequest()
                        .getSession(false);
    }

    private UserAccount createUser(
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

    private CsrfSession obtainCsrf()
            throws Exception {

        MvcResult result =
                mockMvc.perform(
                                get("/api/auth/csrf")
                        )
                        .andExpect(
                                status().isOk()
                        )
                        .andReturn();

        return csrfFromResult(result);
    }

    private CsrfSession obtainCsrf(
            MockHttpSession session
    ) throws Exception {

        MvcResult result =
                mockMvc.perform(
                                get("/api/auth/csrf")
                                        .session(session)
                        )
                        .andExpect(
                                status().isOk()
                        )
                        .andReturn();

        return csrfFromResult(result);
    }

    private CsrfSession csrfFromResult(
            MvcResult result
    ) throws Exception {

        JsonNode json =
                objectMapper.readTree(
                        result.getResponse()
                                .getContentAsString()
                );

        MockHttpSession session =
                (MockHttpSession)
                        result.getRequest()
                                .getSession(false);

        return new CsrfSession(
                session,
                json.get("headerName").asText(),
                json.get("token").asText()
        );
    }

    private record CsrfSession(
            MockHttpSession session,
            String headerName,
            String token
    ) {
    }
}