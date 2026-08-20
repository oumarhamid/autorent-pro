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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration-test")
@Testcontainers
class AuthenticationIntegrationTest {

    private static final String VALID_PASSWORD =
            "correct horse battery staple";

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    UserAccountRepository userAccountRepository;

    @Autowired
    RoleRepository roleRepository;

    @Autowired
    UserRoleRepository userRoleRepository;

    @Test
    void loginCreatesAuthenticatedSessionAndMeReturnsIdentity()
            throws Exception {

        UserAccount user = createUser(
                "auth-admin@example.com",
                false
        );

        assignRole(
                user,
                RoleCode.ADMIN
        );

        CsrfSession csrf = obtainCsrf();

        String sessionIdBeforeLogin =
                csrf.session().getId();

        MvcResult loginResult = mockMvc.perform(
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
                                        objectMapper.writeValueAsString(
                                                Map.of(
                                                        "email",
                                                        "AUTH-ADMIN@EXAMPLE.COM",
                                                        "password",
                                                        VALID_PASSWORD
                                                )
                                        )
                                )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.email")
                                .value(
                                        "auth-admin@example.com"
                                )
                )
                .andExpect(
                        jsonPath("$.roles[0]")
                                .value("ADMIN")
                )
                .andExpect(
                        jsonPath("$.permissions")
                                .isArray()
                )
                .andReturn();

        MockHttpSession authenticatedSession =
                (MockHttpSession)
                        loginResult
                                .getRequest()
                                .getSession(false);

        assertThat(authenticatedSession)
                .isNotNull();

        assertThat(authenticatedSession.getId())
                .isNotEqualTo(sessionIdBeforeLogin);

        mockMvc.perform(
                        get("/api/auth/me")
                                .session(
                                        authenticatedSession
                                )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.email")
                                .value(
                                        "auth-admin@example.com"
                                )
                )
                .andExpect(
                        jsonPath("$.roles")
                                .isArray()
                )
                .andExpect(
                        jsonPath("$.permissions")
                                .isArray()
                );
    }

    @Test
    void invalidPasswordReturnsGeneric401AndSuccessfulLoginResetsFailures()
            throws Exception {

        createUser(
                "auth-failure@example.com",
                false
        );

        CsrfSession csrf = obtainCsrf();

        performLogin(
                csrf,
                "auth-failure@example.com",
                "incorrect password"
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

        UserAccount afterFailure =
                userAccountRepository
                        .findByEmail(
                                "auth-failure@example.com"
                        )
                        .orElseThrow();

        assertThat(
                afterFailure.getFailedLoginAttempts()
        ).isEqualTo(1);

        performLogin(
                csrf,
                "auth-failure@example.com",
                VALID_PASSWORD
        )
                .andExpect(
                        status().isOk()
                );

        UserAccount afterSuccess =
                userAccountRepository
                        .findByEmail(
                                "auth-failure@example.com"
                        )
                        .orElseThrow();

        assertThat(
                afterSuccess.getFailedLoginAttempts()
        ).isZero();

        assertThat(
                afterSuccess.getLockedUntil()
        ).isNull();

        assertThat(
                afterSuccess.getLastLoginAt()
        ).isNotNull();
    }

    @Test
    void accountIsLockedAfterFiveFailedAttempts()
            throws Exception {

        createUser(
                "auth-locked@example.com",
                false
        );

        CsrfSession csrf = obtainCsrf();

        for (int attempt = 0; attempt < 5; attempt++) {
            performLogin(
                    csrf,
                    "auth-locked@example.com",
                    "incorrect password"
            )
                    .andExpect(
                            status().isUnauthorized()
                    );
        }

        UserAccount lockedUser =
                userAccountRepository
                        .findByEmail(
                                "auth-locked@example.com"
                        )
                        .orElseThrow();

        assertThat(
                lockedUser.getFailedLoginAttempts()
        ).isEqualTo(5);

        assertThat(
                lockedUser.getLockedUntil()
        ).isNotNull();

        performLogin(
                csrf,
                "auth-locked@example.com",
                VALID_PASSWORD
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
    void disabledAccountReturnsGeneric401()
            throws Exception {

        createUser(
                "auth-disabled@example.com",
                true
        );

        CsrfSession csrf = obtainCsrf();

        performLogin(
                csrf,
                "auth-disabled@example.com",
                VALID_PASSWORD
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
    void unknownAccountReturnsGeneric401()
            throws Exception {

        CsrfSession csrf = obtainCsrf();

        performLogin(
                csrf,
                "unknown@example.com",
                VALID_PASSWORD
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
    void logoutInvalidatesAuthenticatedSession()
            throws Exception {

        UserAccount user = createUser(
                "logout-user@example.com",
                false
        );

        assignRole(
                user,
                RoleCode.CLIENT
        );

        CsrfSession initialCsrf = obtainCsrf();

        MvcResult loginResult = mockMvc.perform(
                        post("/api/auth/login")
                                .session(initialCsrf.session())
                                .header(
                                        initialCsrf.headerName(),
                                        initialCsrf.token()
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        objectMapper.writeValueAsString(
                                                Map.of(
                                                        "email",
                                                        "logout-user@example.com",
                                                        "password",
                                                        VALID_PASSWORD
                                                )
                                        )
                                )
                )
                .andExpect(
                        status().isOk()
                )
                .andReturn();

        MockHttpSession authenticatedSession =
                (MockHttpSession)
                        loginResult
                                .getRequest()
                                .getSession(false);

        assertThat(authenticatedSession)
                .isNotNull();

        MvcResult csrfResult = mockMvc.perform(
                        get("/api/auth/csrf")
                                .session(authenticatedSession)
                )
                .andExpect(
                        status().isOk()
                )
                .andReturn();

        JsonNode csrfJson = objectMapper.readTree(
                csrfResult
                        .getResponse()
                        .getContentAsString()
        );

        String csrfHeader =
                csrfJson
                        .get("headerName")
                        .asText();

        String csrfToken =
                csrfJson
                        .get("token")
                        .asText();

        mockMvc.perform(
                        post("/api/auth/logout")
                                .session(authenticatedSession)
                                .header(
                                        csrfHeader,
                                        csrfToken
                                )
                )
                .andExpect(
                        status().isNoContent()
                );

        assertThat(
                authenticatedSession.isInvalid()
        ).isTrue();

        mockMvc.perform(
                        get("/api/auth/me")
                )
                .andExpect(
                        status().isUnauthorized()
                )
                .andExpect(
                        jsonPath("$.code")
                                .value(
                                        "UNAUTHENTICATED"
                                )
                );
    }

    private UserAccount createUser(
            String email,
            boolean disabled
    ) {
        UserAccount user = UserAccount.create(
                email,
                passwordEncoder.encode(
                        VALID_PASSWORD
                ),
                false,
                Instant.now()
        );

        if (disabled) {
            user.disable();
        }

        return userAccountRepository
                .saveAndFlush(user);
    }

    private void assignRole(
            UserAccount user,
            RoleCode roleCode
    ) {
        Role role = roleRepository
                .findByCode(roleCode)
                .orElseThrow();

        userRoleRepository.saveAndFlush(
                UserRole.assign(
                        user,
                        role
                )
        );
    }

    private CsrfSession obtainCsrf()
            throws Exception {

        MvcResult result = mockMvc.perform(
                        get("/api/auth/csrf")
                )
                .andExpect(
                        status().isOk()
                )
                .andReturn();

        JsonNode json = objectMapper.readTree(
                result
                        .getResponse()
                        .getContentAsString()
        );

        MockHttpSession session =
                (MockHttpSession)
                        result
                                .getRequest()
                                .getSession(false);

        return new CsrfSession(
                session,
                json.get("headerName").asText(),
                json.get("token").asText()
        );
    }

    private org.springframework.test.web.servlet.ResultActions
    performLogin(
            CsrfSession csrf,
            String email,
            String password
    ) throws Exception {

        return mockMvc.perform(
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
                                objectMapper.writeValueAsString(
                                        Map.of(
                                                "email",
                                                email,
                                                "password",
                                                password
                                        )
                                )
                        )
        );
    }

    private record CsrfSession(
            MockHttpSession session,
            String headerName,
            String token
    ) {
    }
}