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
class AccountPasswordChangeIntegrationTest {

    private static final String CURRENT_PASSWORD =
            "correct horse battery staple";

    private static final String NEW_PASSWORD =
            "new secure rental passphrase";

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
    RoleRepository roleRepository;

    @Autowired
    UserRoleRepository userRoleRepository;

    @Test
    void successfulChangeUpdatesPasswordAndInvalidatesSession()
            throws Exception {

        UserAccount user =
                createUser(
                        "password-success@example.com",
                        true
                );

        assignRole(
                user,
                RoleCode.CLIENT
        );

        MockHttpSession session =
                login(
                        "password-success@example.com",
                        CURRENT_PASSWORD
                );

        CsrfSession csrf =
                obtainCsrf(session);

        Instant previousChangedAt =
                user.getPasswordChangedAt();

        mockMvc.perform(
                        post(
                                "/api/account/change-password"
                        )
                                .session(session)
                                .header(
                                        csrf.headerName(),
                                        csrf.token()
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        changePasswordBody(
                                                CURRENT_PASSWORD,
                                                NEW_PASSWORD
                                        )
                                )
                )
                .andExpect(
                        status().isNoContent()
                );

        assertThat(
                session.isInvalid()
        ).isTrue();

        UserAccount updated =
                userAccountRepository
                        .findByEmail(
                                "password-success@example.com"
                        )
                        .orElseThrow();

        assertThat(
                passwordEncoder.matches(
                        NEW_PASSWORD,
                        updated.getPasswordHash()
                )
        ).isTrue();

        assertThat(
                passwordEncoder.matches(
                        CURRENT_PASSWORD,
                        updated.getPasswordHash()
                )
        ).isFalse();

        assertThat(
                updated.isMustChangePassword()
        ).isFalse();

        assertThat(
                updated.getPasswordChangedAt()
        ).isAfter(previousChangedAt);

        performLogin(
                "password-success@example.com",
                CURRENT_PASSWORD
        )
                .andExpect(
                        status().isUnauthorized()
                );

        performLogin(
                "password-success@example.com",
                NEW_PASSWORD
        )
                .andExpect(
                        status().isOk()
                );
    }

    @Test
    void wrongCurrentPasswordIsRejected()
            throws Exception {

        UserAccount user =
                createUser(
                        "password-wrong@example.com",
                        false
                );

        assignRole(
                user,
                RoleCode.CLIENT
        );

        MockHttpSession session =
                login(
                        "password-wrong@example.com",
                        CURRENT_PASSWORD
                );

        CsrfSession csrf =
                obtainCsrf(session);

        mockMvc.perform(
                        post(
                                "/api/account/change-password"
                        )
                                .session(session)
                                .header(
                                        csrf.headerName(),
                                        csrf.token()
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        changePasswordBody(
                                                "wrong current password",
                                                NEW_PASSWORD
                                        )
                                )
                )
                .andExpect(
                        status().isBadRequest()
                )
                .andExpect(
                        jsonPath("$.code")
                                .value(
                                        "CURRENT_PASSWORD_INVALID"
                                )
                );

        UserAccount unchanged =
                userAccountRepository
                        .findByEmail(
                                "password-wrong@example.com"
                        )
                        .orElseThrow();

        assertThat(
                passwordEncoder.matches(
                        CURRENT_PASSWORD,
                        unchanged.getPasswordHash()
                )
        ).isTrue();
    }

    @Test
    void tooShortNewPasswordIsRejected()
            throws Exception {

        UserAccount user =
                createUser(
                        "password-short@example.com",
                        false
                );

        assignRole(
                user,
                RoleCode.CLIENT
        );

        MockHttpSession session =
                login(
                        "password-short@example.com",
                        CURRENT_PASSWORD
                );

        CsrfSession csrf =
                obtainCsrf(session);

        mockMvc.perform(
                        post(
                                "/api/account/change-password"
                        )
                                .session(session)
                                .header(
                                        csrf.headerName(),
                                        csrf.token()
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        changePasswordBody(
                                                CURRENT_PASSWORD,
                                                "too-short"
                                        )
                                )
                )
                .andExpect(
                        status().isBadRequest()
                )
                .andExpect(
                        jsonPath("$.code")
                                .value(
                                        "PASSWORD_TOO_SHORT"
                                )
                );
    }

    @Test
    void samePasswordCannotBeReused()
            throws Exception {

        UserAccount user =
                createUser(
                        "password-reuse@example.com",
                        false
                );

        assignRole(
                user,
                RoleCode.CLIENT
        );

        MockHttpSession session =
                login(
                        "password-reuse@example.com",
                        CURRENT_PASSWORD
                );

        CsrfSession csrf =
                obtainCsrf(session);

        mockMvc.perform(
                        post(
                                "/api/account/change-password"
                        )
                                .session(session)
                                .header(
                                        csrf.headerName(),
                                        csrf.token()
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        changePasswordBody(
                                                CURRENT_PASSWORD,
                                                CURRENT_PASSWORD
                                        )
                                )
                )
                .andExpect(
                        status().isBadRequest()
                )
                .andExpect(
                        jsonPath("$.code")
                                .value(
                                        "PASSWORD_REUSE_NOT_ALLOWED"
                                )
                );
    }

    @Test
    void commonPasswordIsRejected()
            throws Exception {

        UserAccount user =
                createUser(
                        "password-common@example.com",
                        false
                );

        assignRole(
                user,
                RoleCode.CLIENT
        );

        MockHttpSession session =
                login(
                        "password-common@example.com",
                        CURRENT_PASSWORD
                );

        CsrfSession csrf =
                obtainCsrf(session);

        mockMvc.perform(
                        post(
                                "/api/account/change-password"
                        )
                                .session(session)
                                .header(
                                        csrf.headerName(),
                                        csrf.token()
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        changePasswordBody(
                                                CURRENT_PASSWORD,
                                                "passwordpassword"
                                        )
                                )
                )
                .andExpect(
                        status().isBadRequest()
                )
                .andExpect(
                        jsonPath("$.code")
                                .value(
                                        "PASSWORD_TOO_COMMON"
                                )
                );
    }

    @Test
    void userWithoutChangePasswordPermissionReceives403()
            throws Exception {

        createUser(
                "password-no-role@example.com",
                false
        );

        MockHttpSession session =
                login(
                        "password-no-role@example.com",
                        CURRENT_PASSWORD
                );

        CsrfSession csrf =
                obtainCsrf(session);

        mockMvc.perform(
                        post(
                                "/api/account/change-password"
                        )
                                .session(session)
                                .header(
                                        csrf.headerName(),
                                        csrf.token()
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        changePasswordBody(
                                                CURRENT_PASSWORD,
                                                NEW_PASSWORD
                                        )
                                )
                )
                .andExpect(
                        status().isForbidden()
                )
                .andExpect(
                        jsonPath("$.code")
                                .value(
                                        "ACCESS_DENIED"
                                )
                );
    }

    @Test
    void unauthenticatedRequestReceives401()
            throws Exception {

        CsrfSession csrf =
                obtainCsrf();

        mockMvc.perform(
                        post(
                                "/api/account/change-password"
                        )
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
                                        changePasswordBody(
                                                CURRENT_PASSWORD,
                                                NEW_PASSWORD
                                        )
                                )
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
            boolean mustChangePassword
    ) {
        UserAccount user =
                UserAccount.create(
                        email,
                        passwordEncoder.encode(
                                CURRENT_PASSWORD
                        ),
                        mustChangePassword,
                        Instant.now()
                                .minusSeconds(3600)
                );

        return userAccountRepository
                .saveAndFlush(user);
    }

    private void assignRole(
            UserAccount user,
            RoleCode roleCode
    ) {
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

    private MockHttpSession login(
            String email,
            String password
    ) throws Exception {

        MvcResult result =
                performLogin(
                        email,
                        password
                )
                        .andExpect(
                                status().isOk()
                        )
                        .andReturn();

        return (MockHttpSession)
                result
                        .getRequest()
                        .getSession(false);
    }

    private org.springframework.test.web.servlet.ResultActions
    performLogin(
            String email,
            String password
    ) throws Exception {

        CsrfSession csrf =
                obtainCsrf();

        return mockMvc.perform(
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
                                                password
                                        )
                                )
                        )
        );
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

    private String changePasswordBody(
            String currentPassword,
            String newPassword
    ) throws Exception {

        return objectMapper.writeValueAsString(
                Map.of(
                        "currentPassword",
                        currentPassword,
                        "newPassword",
                        newPassword
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