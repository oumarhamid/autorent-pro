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
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration-test")
@Testcontainers
class UserLifecycleIntegrationTest {

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
    void adminCanDisableUser()
            throws Exception {

        UserAccount target =
                createUser(
                        "lifecycle-disable-target@example.com",
                        RoleCode.CLIENT
                );

        LoggedInUser admin =
                createAndLogin(
                        "lifecycle-disable-admin@example.com",
                        RoleCode.ADMIN
                );

        performLifecycleAction(
                admin.session(),
                target.getId(),
                "disable"
        )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.status")
                                .value("DISABLED")
                );

        UserAccount persisted =
                userAccountRepository
                        .findById(target.getId())
                        .orElseThrow();

        assertThat(
                persisted.isActive()
        ).isFalse();
    }

    @Test
    void adminCanEnableDisabledUser()
            throws Exception {

        UserAccount target =
                createUser(
                        "lifecycle-enable-target@example.com",
                        RoleCode.CLIENT
                );

        target.disable();

        userAccountRepository
                .saveAndFlush(target);

        LoggedInUser admin =
                createAndLogin(
                        "lifecycle-enable-admin@example.com",
                        RoleCode.ADMIN
                );

        performLifecycleAction(
                admin.session(),
                target.getId(),
                "enable"
        )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.status")
                                .value("ACTIVE")
                );

        UserAccount persisted =
                userAccountRepository
                        .findById(target.getId())
                        .orElseThrow();

        assertThat(
                persisted.isActive()
        ).isTrue();
    }

    @Test
    void managerCannotDisableUser()
            throws Exception {

        UserAccount target =
                createUser(
                        "lifecycle-forbidden-target@example.com",
                        RoleCode.CLIENT
                );

        LoggedInUser manager =
                createAndLogin(
                        "lifecycle-manager@example.com",
                        RoleCode.MANAGER
                );

        performLifecycleAction(
                manager.session(),
                target.getId(),
                "disable"
        )
                .andExpect(status().isForbidden())
                .andExpect(
                        jsonPath("$.code")
                                .value("ACCESS_DENIED")
                );

        UserAccount persisted =
                userAccountRepository
                        .findById(target.getId())
                        .orElseThrow();

        assertThat(
                persisted.isActive()
        ).isTrue();
    }

    @Test
    void adminCannotDisableOwnAccount()
            throws Exception {

        LoggedInUser admin =
                createAndLogin(
                        "lifecycle-self-admin@example.com",
                        RoleCode.ADMIN
                );

        performLifecycleAction(
                admin.session(),
                admin.userId(),
                "disable"
        )
                .andExpect(status().isConflict())
                .andExpect(
                        jsonPath("$.code")
                                .value(
                                        "SELF_DISABLE_NOT_ALLOWED"
                                )
                );

        UserAccount persisted =
                userAccountRepository
                        .findById(admin.userId())
                        .orElseThrow();

        assertThat(
                persisted.isActive()
        ).isTrue();
    }

    @Test
    void unknownUserReturns404WhenDisabling()
            throws Exception {

        LoggedInUser admin =
                createAndLogin(
                        "lifecycle-not-found-admin@example.com",
                        RoleCode.ADMIN
                );

        performLifecycleAction(
                admin.session(),
                UUID.randomUUID(),
                "disable"
        )
                .andExpect(status().isNotFound())
                .andExpect(
                        jsonPath("$.code")
                                .value("USER_NOT_FOUND")
                );
    }

    @Test
    void disabledAccountCannotLogin()
            throws Exception {

        UserAccount target =
                createUser(
                        "lifecycle-login-disabled@example.com",
                        RoleCode.CLIENT
                );

        LoggedInUser admin =
                createAndLogin(
                        "lifecycle-login-admin@example.com",
                        RoleCode.ADMIN
                );

        performLifecycleAction(
                admin.session(),
                target.getId(),
                "disable"
        )
                .andExpect(status().isOk());

        CsrfSession loginCsrf =
                obtainCsrf();

        mockMvc.perform(
                        post("/api/auth/login")
                                .session(
                                        loginCsrf.session()
                                )
                                .header(
                                        loginCsrf.headerName(),
                                        loginCsrf.token()
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        objectMapper.writeValueAsString(
                                                Map.of(
                                                        "email",
                                                        target.getEmail(),
                                                        "password",
                                                        PASSWORD
                                                )
                                        )
                                )
                )
                .andExpect(
                        status().isUnauthorized()
                )
                .andExpect(
                        jsonPath("$.code")
                                .value(
                                        "AUTHENTICATION_FAILED"
                                )
                );
    }

    @Test
    void disablingAccountInvalidatesExistingSessionOnNextRequest()
            throws Exception {

        LoggedInUser target =
                createAndLogin(
                        "lifecycle-session-target@example.com",
                        RoleCode.CLIENT
                );

        LoggedInUser admin =
                createAndLogin(
                        "lifecycle-session-admin@example.com",
                        RoleCode.ADMIN
                );

        performLifecycleAction(
                admin.session(),
                target.userId(),
                "disable"
        )
                .andExpect(status().isOk());

        mockMvc.perform(
                        get("/api/auth/me")
                                .session(
                                        target.session()
                                )
                )
                .andExpect(
                        status().isUnauthorized()
                )
                .andExpect(
                        jsonPath("$.code")
                                .value("UNAUTHENTICATED")
                );
    }

    @Test
    void reenabledAccountCanLoginAgain()
            throws Exception {

        UserAccount target =
                createUser(
                        "lifecycle-reenabled@example.com",
                        RoleCode.CLIENT
                );

        LoggedInUser admin =
                createAndLogin(
                        "lifecycle-reenable-admin@example.com",
                        RoleCode.ADMIN
                );

        performLifecycleAction(
                admin.session(),
                target.getId(),
                "disable"
        )
                .andExpect(status().isOk());

        performLifecycleAction(
                admin.session(),
                target.getId(),
                "enable"
        )
                .andExpect(status().isOk());

        CsrfSession loginCsrf =
                obtainCsrf();

        mockMvc.perform(
                        post("/api/auth/login")
                                .session(
                                        loginCsrf.session()
                                )
                                .header(
                                        loginCsrf.headerName(),
                                        loginCsrf.token()
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        objectMapper.writeValueAsString(
                                                Map.of(
                                                        "email",
                                                        target.getEmail(),
                                                        "password",
                                                        PASSWORD
                                                )
                                        )
                                )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.email")
                                .value(target.getEmail())
                );
    }

    private ResultActions performLifecycleAction(
            MockHttpSession actorSession,
            UUID userId,
            String action
    ) throws Exception {

        CsrfSession csrf =
                obtainCsrf(
                        actorSession
                );

        return mockMvc.perform(
                post(
                        "/api/users/{userId}/{action}",
                        userId,
                        action
                )
                        .session(actorSession)
                        .header(
                                csrf.headerName(),
                                csrf.token()
                        )
        );
    }

    private LoggedInUser createAndLogin(
            String email,
            RoleCode role
    ) throws Exception {

        UserAccount user =
                createUser(
                        email,
                        role
                );

        MockHttpSession session =
                login(email);

        return new LoggedInUser(
                user.getId(),
                session
        );
    }

    private MockHttpSession login(
            String email
    ) throws Exception {

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

        return csrfFromResult(
                result
        );
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

        return csrfFromResult(
                result
        );
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

    private record LoggedInUser(
            UUID userId,
            MockHttpSession session
    ) {
    }

    private record CsrfSession(
            MockHttpSession session,
            String headerName,
            String token
    ) {
    }
}