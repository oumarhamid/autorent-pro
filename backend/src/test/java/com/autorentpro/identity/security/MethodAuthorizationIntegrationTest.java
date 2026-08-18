package com.autorentpro.identity.security;

import com.autorentpro.identity.application.PermissionGrant;
import com.autorentpro.identity.domain.model.PermissionCode;
import com.autorentpro.identity.domain.model.PermissionScope;
import com.autorentpro.identity.domain.model.RoleCode;
import com.autorentpro.identity.infrastructure.persistence.RolePermissionRepository;
import com.autorentpro.identity.infrastructure.persistence.RoleRepository;
import com.autorentpro.identity.infrastructure.persistence.UserAccountRepository;
import com.autorentpro.identity.infrastructure.persistence.UserRoleRepository;
import com.autorentpro.identity.infrastructure.security.AuthenticatedUserPrincipal;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(MethodAuthorizationIntegrationTest.AuthorizationTestConfiguration.class)
@MockitoBean(types = {
        UserAccountRepository.class,
        UserRoleRepository.class,
        RolePermissionRepository.class,
        RoleRepository.class
})
class MethodAuthorizationIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void unauthenticatedRequestReturns401()
            throws Exception {

        mockMvc.perform(
                        get(
                                "/api/test/authorization/account/{userId}",
                                UUID.randomUUID()
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
    void selfPermissionAllowsOwnResource()
            throws Exception {

        UUID userId = UUID.randomUUID();

        Authentication authentication =
                createAuthentication(
                        userId,
                        Set.of(RoleCode.CLIENT),
                        Set.of(
                                new PermissionGrant(
                                        PermissionCode.ACCOUNT_READ,
                                        PermissionScope.SELF
                                )
                        )
                );

        mockMvc.perform(
                        get(
                                "/api/test/authorization/account/{userId}",
                                userId
                        )
                                .with(
                                        authentication(authentication)
                                )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        content().string(
                                "account:" + userId
                        )
                );
    }

    @Test
    void selfPermissionRejectsAnotherUsersUuid()
            throws Exception {

        UUID authenticatedUserId =
                UUID.randomUUID();

        UUID anotherUserId =
                UUID.randomUUID();

        Authentication authentication =
                createAuthentication(
                        authenticatedUserId,
                        Set.of(RoleCode.CLIENT),
                        Set.of(
                                new PermissionGrant(
                                        PermissionCode.ACCOUNT_READ,
                                        PermissionScope.SELF
                                )
                        )
                );

        mockMvc.perform(
                        get(
                                "/api/test/authorization/account/{userId}",
                                anotherUserId
                        )
                                .with(
                                        authentication(authentication)
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

    @Test
    void globalUserReadPermissionAllowsAnyUserResource()
            throws Exception {

        UUID managerId =
                UUID.randomUUID();

        UUID requestedUserId =
                UUID.randomUUID();

        Authentication authentication =
                createAuthentication(
                        managerId,
                        Set.of(RoleCode.MANAGER),
                        Set.of(
                                new PermissionGrant(
                                        PermissionCode.USER_READ,
                                        PermissionScope.GLOBAL
                                )
                        )
                );

        mockMvc.perform(
                        get(
                                "/api/test/authorization/users/{userId}",
                                requestedUserId
                        )
                                .with(
                                        authentication(authentication)
                                )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        content().string(
                                "user:" + requestedUserId
                        )
                );
    }

    @Test
    void agencyScopedPermissionDoesNotEscalateToGlobal()
            throws Exception {

        Authentication authentication =
                createAuthentication(
                        UUID.randomUUID(),
                        Set.of(RoleCode.AGENCY_MANAGER),
                        Set.of(
                                new PermissionGrant(
                                        PermissionCode.USER_READ,
                                        PermissionScope.AGENCY
                                )
                        )
                );

        mockMvc.perform(
                        get(
                                "/api/test/authorization/users/{userId}",
                                UUID.randomUUID()
                        )
                                .with(
                                        authentication(authentication)
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

    @Test
    void missingPermissionIsDenied()
            throws Exception {

        Authentication authentication =
                createAuthentication(
                        UUID.randomUUID(),
                        Set.of(RoleCode.CLIENT),
                        Set.of(
                                new PermissionGrant(
                                        PermissionCode.ACCOUNT_READ,
                                        PermissionScope.SELF
                                )
                        )
                );

        mockMvc.perform(
                        get(
                                "/api/test/authorization/users/{userId}",
                                UUID.randomUUID()
                        )
                                .with(
                                        authentication(authentication)
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

    @Test
    void adminRoleAllowsAdminMethod()
            throws Exception {

        Authentication authentication =
                createAuthentication(
                        UUID.randomUUID(),
                        Set.of(RoleCode.ADMIN),
                        Set.of()
                );

        mockMvc.perform(
                        get(
                                "/api/test/authorization/admin"
                        )
                                .with(
                                        authentication(authentication)
                                )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        content().string("admin")
                );
    }

    @Test
    void nonAdminRoleCannotAccessAdminMethod()
            throws Exception {

        Authentication authentication =
                createAuthentication(
                        UUID.randomUUID(),
                        Set.of(RoleCode.CLIENT),
                        Set.of()
                );

        mockMvc.perform(
                        get(
                                "/api/test/authorization/admin"
                        )
                                .with(
                                        authentication(authentication)
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

    private Authentication createAuthentication(
            UUID userId,
            Set<RoleCode> roles,
            Set<PermissionGrant> permissions
    ) {
        AuthenticatedUserPrincipal principal =
                new AuthenticatedUserPrincipal(
                        userId,
                        "authorization-test@example.com",
                        roles,
                        permissions,
                        false
                );

        Set<GrantedAuthority> authorities =
                new LinkedHashSet<>();

        for (RoleCode role : roles) {
            authorities.add(
                    new SimpleGrantedAuthority(
                            "ROLE_" + role.name()
                    )
            );
        }

        for (PermissionGrant grant : permissions) {
            authorities.add(
                    new SimpleGrantedAuthority(
                            "PERMISSION_"
                                    + grant.permission().name()
                                    + "_"
                                    + grant.scope().name()
                    )
            );
        }

        return UsernamePasswordAuthenticationToken
                .authenticated(
                        principal,
                        null,
                        authorities
                );
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class AuthorizationTestConfiguration {

        @Bean
        AuthorizationProbeService authorizationProbeService() {
            return new AuthorizationProbeService();
        }

        @Bean
        AuthorizationProbeController authorizationProbeController(
                AuthorizationProbeService authorizationProbeService
        ) {
            return new AuthorizationProbeController(
                    authorizationProbeService
            );
        }
    }

    static class AuthorizationProbeService {

        @PreAuthorize(
                "@identityAuthorization.canAccessSelf("
                        + "authentication, "
                        + "'ACCOUNT_READ', "
                        + "#userId)"
        )
        String readOwnAccount(UUID userId) {
            return "account:" + userId;
        }

        @PreAuthorize(
                "@identityAuthorization.canAccessGlobal("
                        + "authentication, "
                        + "'USER_READ')"
        )
        String readAnyUser(UUID userId) {
            return "user:" + userId;
        }

        @PreAuthorize("hasRole('ADMIN')")
        String adminOnly() {
            return "admin";
        }
    }

    @RestController
    @RequestMapping("/api/test/authorization")
    static class AuthorizationProbeController {

        private final AuthorizationProbeService authorizationProbeService;

        AuthorizationProbeController(
                AuthorizationProbeService authorizationProbeService
        ) {
            this.authorizationProbeService =
                    authorizationProbeService;
        }

        @GetMapping("/account/{userId}")
        String account(
                @PathVariable UUID userId
        ) {
            return authorizationProbeService
                    .readOwnAccount(userId);
        }

        @GetMapping("/users/{userId}")
        String user(
                @PathVariable UUID userId
        ) {
            return authorizationProbeService
                    .readAnyUser(userId);
        }

        @GetMapping("/admin")
        String admin() {
            return authorizationProbeService
                    .adminOnly();
        }
    }
}