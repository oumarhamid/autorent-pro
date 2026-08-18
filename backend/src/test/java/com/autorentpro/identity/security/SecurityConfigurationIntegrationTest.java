package com.autorentpro.identity.security;

import com.autorentpro.identity.infrastructure.persistence.RolePermissionRepository;
import com.autorentpro.identity.infrastructure.persistence.UserAccountRepository;
import com.autorentpro.identity.infrastructure.persistence.UserRoleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@MockitoBean(types = {
        UserAccountRepository.class,
        UserRoleRepository.class,
        RolePermissionRepository.class
})
class SecurityConfigurationIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void healthEndpointIsPublic() throws Exception {
        mockMvc.perform(
                        get("/actuator/health")
                )
                .andExpect(
                        status().isOk()
                );
    }

    @Test
    void csrfEndpointIsPublic() throws Exception {
        mockMvc.perform(
                        get("/api/auth/csrf")
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        content().contentTypeCompatibleWith(
                                "application/json"
                        )
                )
                .andExpect(
                        jsonPath("$.token").isNotEmpty()
                )
                .andExpect(
                        jsonPath("$.headerName")
                                .value("X-CSRF-TOKEN")
                )
                .andExpect(
                        jsonPath("$.parameterName")
                                .value("_csrf")
                );
    }

    @Test
    void protectedRequestWithoutAuthenticationReturns401()
            throws Exception {

        mockMvc.perform(
                        get("/api/security/protected-probe")
                )
                .andExpect(
                        status().isUnauthorized()
                )
                .andExpect(
                        content().contentTypeCompatibleWith(
                                "application/json"
                        )
                )
                .andExpect(
                        jsonPath("$.status")
                                .value(401)
                )
                .andExpect(
                        jsonPath("$.code")
                                .value("UNAUTHENTICATED")
                );
    }

    @Test
    void unsafeRequestWithoutCsrfReturns403()
            throws Exception {

        mockMvc.perform(
                        post("/api/security/protected-probe")
                                .with(
                                        user("security-test")
                                )
                )
                .andExpect(
                        status().isForbidden()
                )
                .andExpect(
                        jsonPath("$.code")
                                .value("ACCESS_DENIED")
                );
    }
}