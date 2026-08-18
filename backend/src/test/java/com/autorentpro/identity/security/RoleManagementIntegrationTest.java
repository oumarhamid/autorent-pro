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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration-test")
@Testcontainers
class RoleManagementIntegrationTest {

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

    @Test
    void adminCanAssignRole()
            throws Exception {

        UserAccount admin =
                createUserWithRoles(
                        "assign-admin",
                        RoleCode.ADMIN
                );

        UserAccount target =
                createUserWithRoles(
                        "assign-target",
                        RoleCode.CLIENT
                );

        MockHttpSession adminSession =
                login(admin.getEmail());

        performAssignRole(
                adminSession,
                target.getId(),
                RoleCode.MANAGER
        )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.roles")
                                .value(
                                        hasItem(
                                                RoleCode.MANAGER.name()
                                        )
                                )
                );

        assertThat(
                userRoleRepository
                        .findRoleCodesByUserId(
                                target.getId()
                        )
        ).containsExactlyInAnyOrder(
                RoleCode.CLIENT,
                RoleCode.MANAGER
        );
    }

    @Test
    void assigningExistingRoleIsIdempotent()
            throws Exception {

        UserAccount admin =
                createUserWithRoles(
                        "assign-idempotent-admin",
                        RoleCode.ADMIN
                );

        UserAccount target =
                createUserWithRoles(
                        "assign-idempotent-target",
                        RoleCode.CLIENT,
                        RoleCode.MANAGER
                );

        MockHttpSession adminSession =
                login(admin.getEmail());

        performAssignRole(
                adminSession,
                target.getId(),
                RoleCode.MANAGER
        )
                .andExpect(status().isOk());

        performAssignRole(
                adminSession,
                target.getId(),
                RoleCode.MANAGER
        )
                .andExpect(status().isOk());

        assertThat(
                userRoleRepository
                        .findRoleCodesByUserId(
                                target.getId()
                        )
        ).containsExactlyInAnyOrder(
                RoleCode.CLIENT,
                RoleCode.MANAGER
        );
    }

    @Test
    void adminCanRemoveRole()
            throws Exception {

        UserAccount admin =
                createUserWithRoles(
                        "remove-admin",
                        RoleCode.ADMIN
                );

        UserAccount target =
                createUserWithRoles(
                        "remove-target",
                        RoleCode.CLIENT,
                        RoleCode.MANAGER
                );

        MockHttpSession adminSession =
                login(admin.getEmail());

        performRemoveRole(
                adminSession,
                target.getId(),
                RoleCode.MANAGER
        )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.roles")
                                .value(
                                        not(
                                                hasItem(
                                                        RoleCode.MANAGER.name()
                                                )
                                        )
                                )
                );

        assertThat(
                userRoleRepository
                        .findRoleCodesByUserId(
                                target.getId()
                        )
        ).containsExactly(
                RoleCode.CLIENT
        );
    }

    @Test
    void removingAbsentRoleIsIdempotent()
            throws Exception {

        UserAccount admin =
                createUserWithRoles(
                        "remove-idempotent-admin",
                        RoleCode.ADMIN
                );

        UserAccount target =
                createUserWithRoles(
                        "remove-idempotent-target",
                        RoleCode.CLIENT
                );

        MockHttpSession adminSession =
                login(admin.getEmail());

        performRemoveRole(
                adminSession,
                target.getId(),
                RoleCode.MANAGER
        )
                .andExpect(status().isOk());

        performRemoveRole(
                adminSession,
                target.getId(),
                RoleCode.MANAGER
        )
                .andExpect(status().isOk());

        assertThat(
                userRoleRepository
                        .findRoleCodesByUserId(
                                target.getId()
                        )
        ).containsExactly(
                RoleCode.CLIENT
        );
    }

    @Test
    void managerCannotAssignRole()
            throws Exception {

        UserAccount manager =
                createUserWithRoles(
                        "forbidden-assign-manager",
                        RoleCode.MANAGER
                );

        UserAccount target =
                createUserWithRoles(
                        "forbidden-assign-target",
                        RoleCode.CLIENT
                );

        MockHttpSession managerSession =
                login(manager.getEmail());

        performAssignRole(
                managerSession,
                target.getId(),
                RoleCode.ADMIN
        )
                .andExpect(
                        status().isForbidden()
                )
                .andExpect(
                        jsonPath("$.code")
                                .value("ACCESS_DENIED")
                );

        assertThat(
                userRoleRepository
                        .findRoleCodesByUserId(
                                target.getId()
                        )
        ).containsExactly(
                RoleCode.CLIENT
        );
    }

    @Test
    void managerCannotRemoveRole()
            throws Exception {

        UserAccount manager =
                createUserWithRoles(
                        "forbidden-remove-manager",
                        RoleCode.MANAGER
                );

        UserAccount target =
                createUserWithRoles(
                        "forbidden-remove-target",
                        RoleCode.CLIENT,
                        RoleCode.ADMIN
                );

        MockHttpSession managerSession =
                login(manager.getEmail());

        performRemoveRole(
                managerSession,
                target.getId(),
                RoleCode.ADMIN
        )
                .andExpect(
                        status().isForbidden()
                )
                .andExpect(
                        jsonPath("$.code")
                                .value("ACCESS_DENIED")
                );

        assertThat(
                userRoleRepository
                        .findRoleCodesByUserId(
                                target.getId()
                        )
        ).contains(
                RoleCode.ADMIN
        );
    }

    @Test
    void assigningRoleToUnknownUserReturns404()
            throws Exception {

        UserAccount admin =
                createUserWithRoles(
                        "unknown-user-admin",
                        RoleCode.ADMIN
                );

        MockHttpSession adminSession =
                login(admin.getEmail());

        performAssignRole(
                adminSession,
                UUID.randomUUID(),
                RoleCode.CLIENT
        )
                .andExpect(
                        status().isNotFound()
                )
                .andExpect(
                        jsonPath("$.code")
                                .value("USER_NOT_FOUND")
                );
    }

    @Test
    void adminCannotRemoveOwnAdminRole()
            throws Exception {

        UserAccount admin =
                createUserWithRoles(
                        "self-admin-removal",
                        RoleCode.ADMIN
                );

        MockHttpSession adminSession =
                login(admin.getEmail());

        performRemoveRole(
                adminSession,
                admin.getId(),
                RoleCode.ADMIN
        )
                .andExpect(
                        status().isConflict()
                )
                .andExpect(
                        jsonPath("$.code")
                                .value(
                                        "SELF_ADMIN_ROLE_REMOVAL_NOT_ALLOWED"
                                )
                );

        assertThat(
                userRoleRepository
                        .findRoleCodesByUserId(
                                admin.getId()
                        )
        ).contains(
                RoleCode.ADMIN
        );
    }

    @Test
    void revokedAdminRoleImmediatelyStopsExistingSessionAndRefreshesMe()
            throws Exception {

        UserAccount controllingAdmin =
                createUserWithRoles(
                        "revocation-controller",
                        RoleCode.ADMIN
                );

        UserAccount targetAdmin =
                createUserWithRoles(
                        "revocation-target",
                        RoleCode.ADMIN
                );

        MockHttpSession controllingSession =
                login(
                        controllingAdmin.getEmail()
                );

        MockHttpSession targetSession =
                login(
                        targetAdmin.getEmail()
                );

        mockMvc.perform(
                        get("/api/users")
                                .session(targetSession)
                )
                .andExpect(
                        status().isOk()
                );

        mockMvc.perform(
                        get("/api/auth/me")
                                .session(targetSession)
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.roles")
                                .value(
                                        hasItem(
                                                RoleCode.ADMIN.name()
                                        )
                                )
                );

        performRemoveRole(
                controllingSession,
                targetAdmin.getId(),
                RoleCode.ADMIN
        )
                .andExpect(
                        status().isOk()
                );

        mockMvc.perform(
                        get("/api/users")
                                .session(targetSession)
                )
                .andExpect(
                        status().isForbidden()
                )
                .andExpect(
                        jsonPath("$.code")
                                .value("ACCESS_DENIED")
                );

        mockMvc.perform(
                        get("/api/auth/me")
                                .session(targetSession)
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.roles")
                                .value(
                                        not(
                                                hasItem(
                                                        RoleCode.ADMIN.name()
                                                )
                                        )
                                )
                );

        assertThat(
                userRoleRepository
                        .findRoleCodesByUserId(
                                targetAdmin.getId()
                        )
        ).doesNotContain(
                RoleCode.ADMIN
        );
    }

    @Test
    void assignedAdminRoleImmediatelyAuthorizesExistingSessionAndRefreshesMe()
            throws Exception {

        UserAccount controllingAdmin =
                createUserWithRoles(
                        "grant-controller",
                        RoleCode.ADMIN
                );

        UserAccount target =
                createUserWithRoles(
                        "grant-target",
                        RoleCode.CLIENT
                );

        MockHttpSession controllingSession =
                login(
                        controllingAdmin.getEmail()
                );

        MockHttpSession targetSession =
                login(
                        target.getEmail()
                );

        mockMvc.perform(
                        get("/api/users")
                                .session(targetSession)
                )
                .andExpect(
                        status().isForbidden()
                );

        mockMvc.perform(
                        get("/api/auth/me")
                                .session(targetSession)
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.roles")
                                .value(
                                        hasItem(
                                                RoleCode.CLIENT.name()
                                        )
                                )
                )
                .andExpect(
                        jsonPath("$.roles")
                                .value(
                                        not(
                                                hasItem(
                                                        RoleCode.ADMIN.name()
                                                )
                                        )
                                )
                );

        performAssignRole(
                controllingSession,
                target.getId(),
                RoleCode.ADMIN
        )
                .andExpect(
                        status().isOk()
                );

        mockMvc.perform(
                        get("/api/users")
                                .session(targetSession)
                )
                .andExpect(
                        status().isOk()
                );

        mockMvc.perform(
                        get("/api/auth/me")
                                .session(targetSession)
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.roles")
                                .value(
                                        hasItems(
                                                RoleCode.CLIENT.name(),
                                                RoleCode.ADMIN.name()
                                        )
                                )
                );

        assertThat(
                userRoleRepository
                        .findRoleCodesByUserId(
                                target.getId()
                        )
        ).containsExactlyInAnyOrder(
                RoleCode.CLIENT,
                RoleCode.ADMIN
        );
    }

    private UserAccount createUserWithRoles(
            String emailPrefix,
            RoleCode... roleCodes
    ) {
        String email =
                emailPrefix
                        + "-"
                        + UUID.randomUUID()
                        + "@example.com";

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

        for (RoleCode roleCode : roleCodes) {
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
        }

        return user;
    }

    private MockHttpSession login(
            String email
    ) throws Exception {

        CsrfState csrf =
                obtainCsrf(null);

        MvcResult result =
                mockMvc.perform(
                                post("/api/auth/login")
                                        .session(csrf.session())
                                        .header(
                                                csrf.headerName(),
                                                csrf.token()
                                        )
                                        .contentType(
                                                MediaType.APPLICATION_JSON
                                        )
                                        .content(
                                                objectMapper
                                                        .writeValueAsString(
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

    private org.springframework.test.web.servlet.ResultActions
    performAssignRole(
            MockHttpSession session,
            UUID userId,
            RoleCode roleCode
    ) throws Exception {

        CsrfState csrf =
                obtainCsrf(session);

        return mockMvc.perform(
                put(
                        "/api/users/{userId}/roles/{role}",
                        userId,
                        roleCode.name()
                )
                        .session(csrf.session())
                        .header(
                                csrf.headerName(),
                                csrf.token()
                        )
        );
    }

    private org.springframework.test.web.servlet.ResultActions
    performRemoveRole(
            MockHttpSession session,
            UUID userId,
            RoleCode roleCode
    ) throws Exception {

        CsrfState csrf =
                obtainCsrf(session);

        return mockMvc.perform(
                delete(
                        "/api/users/{userId}/roles/{role}",
                        userId,
                        roleCode.name()
                )
                        .session(csrf.session())
                        .header(
                                csrf.headerName(),
                                csrf.token()
                        )
        );
    }

    private CsrfState obtainCsrf(
            MockHttpSession session
    ) throws Exception {

        MockHttpServletRequestBuilder request =
                get("/api/auth/csrf");

        if (session != null) {
            request.session(session);
        }

        MvcResult result =
                mockMvc.perform(request)
                        .andExpect(
                                status().isOk()
                        )
                        .andReturn();

        JsonNode body =
                objectMapper.readTree(
                        result.getResponse()
                                .getContentAsString()
                );

        MockHttpSession resultingSession =
                (MockHttpSession)
                        result.getRequest()
                                .getSession(false);

        return new CsrfState(
                resultingSession,
                body.get("headerName").asText(),
                body.get("token").asText()
        );
    }

    private record CsrfState(
            MockHttpSession session,
            String headerName,
            String token
    ) {
    }
}