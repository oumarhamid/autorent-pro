package com.autorentpro.agency.api;

import com.autorentpro.agency.application.AgencyManagementException;
import com.autorentpro.agency.application.UserAgencyAssignmentService;
import com.autorentpro.agency.application.UserAgencyAssignmentView;
import com.autorentpro.identity.infrastructure.security.ActiveAccountFilter;
import com.autorentpro.identity.infrastructure.security.IdentityAuthorization;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers =
                UserAgencyAssignmentController.class,
        excludeFilters =
                @ComponentScan.Filter(
                        type =
                                FilterType.ASSIGNABLE_TYPE,
                        classes =
                                ActiveAccountFilter.class
                )
)
@Import({
        AgencyApiExceptionHandler.class,
        UserAgencyAssignmentControllerTest
                .MethodSecurityTestConfiguration.class
})
class UserAgencyAssignmentControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    UserAgencyAssignmentService
            userAgencyAssignmentService;

    @MockBean(name = "identityAuthorization")
    IdentityAuthorization
            identityAuthorization;

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfiguration {
    }

    @Test
    @WithMockUser
    void assignsUserWhenPermissionIsGranted()
            throws Exception {

        UUID agencyId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(
                identityAuthorization
                        .canAccessGlobal(
                                any(),
                                eq("USER_AGENCY_ASSIGN")
                        )
        ).thenReturn(true);

        when(
                userAgencyAssignmentService
                        .assignUserToAgency(
                                userId,
                                agencyId
                        )
        ).thenReturn(
                view(
                        userId,
                        agencyId,
                        true
                )
        );

        mockMvc.perform(
                        post(
                                "/api/agencies/{agencyId}/users/{userId}",
                                agencyId,
                                userId
                        )
                                .with(csrf())
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.userId")
                                .value(
                                        userId.toString()
                                )
                )
                .andExpect(
                        jsonPath("$.agencyId")
                                .value(
                                        agencyId.toString()
                                )
                )
                .andExpect(
                        jsonPath("$.active")
                                .value(true)
                );

        verify(
                userAgencyAssignmentService
        ).assignUserToAgency(
                userId,
                agencyId
        );
    }

    @Test
    @WithMockUser
    void rejectsAssignmentWithoutPermission()
            throws Exception {

        UUID agencyId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(
                identityAuthorization
                        .canAccessGlobal(
                                any(),
                                eq("USER_AGENCY_ASSIGN")
                        )
        ).thenReturn(false);

        mockMvc.perform(
                        post(
                                "/api/agencies/{agencyId}/users/{userId}",
                                agencyId,
                                userId
                        )
                                .with(csrf())
                )
                .andExpect(
                        status().isForbidden()
                );

        verify(
                userAgencyAssignmentService,
                never()
        ).assignUserToAgency(
                any(),
                any()
        );
    }

    @Test
    @WithMockUser
    void removesUserWhenPermissionIsGranted()
            throws Exception {

        UUID agencyId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(
                identityAuthorization
                        .canAccessGlobal(
                                any(),
                                eq("USER_AGENCY_ASSIGN")
                        )
        ).thenReturn(true);

        when(
                userAgencyAssignmentService
                        .removeUserFromAgency(
                                userId,
                                agencyId
                        )
        ).thenReturn(
                view(
                        userId,
                        agencyId,
                        false
                )
        );

        mockMvc.perform(
                        post(
                                "/api/agencies/{agencyId}/users/{userId}/remove",
                                agencyId,
                                userId
                        )
                                .with(csrf())
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.active")
                                .value(false)
                )
                .andExpect(
                        jsonPath("$.endedAt")
                                .exists()
                );

        verify(
                userAgencyAssignmentService
        ).removeUserFromAgency(
                userId,
                agencyId
        );
    }

    @Test
    @WithMockUser
    void rejectsRemovalWithoutPermission()
            throws Exception {

        UUID agencyId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(
                identityAuthorization
                        .canAccessGlobal(
                                any(),
                                eq("USER_AGENCY_ASSIGN")
                        )
        ).thenReturn(false);

        mockMvc.perform(
                        post(
                                "/api/agencies/{agencyId}/users/{userId}/remove",
                                agencyId,
                                userId
                        )
                                .with(csrf())
                )
                .andExpect(
                        status().isForbidden()
                );

        verify(
                userAgencyAssignmentService,
                never()
        ).removeUserFromAgency(
                any(),
                any()
        );
    }

    @Test
    @WithMockUser
    void listsAssignmentsWhenPermissionIsGranted()
            throws Exception {

        UUID agencyId = UUID.randomUUID();

        UUID firstUserId =
                UUID.randomUUID();

        UUID secondUserId =
                UUID.randomUUID();

        when(
                identityAuthorization
                        .canAccessGlobal(
                                any(),
                                eq("USER_AGENCY_ASSIGN")
                        )
        ).thenReturn(true);

        when(
                userAgencyAssignmentService
                        .listActiveAssignments(
                                agencyId
                        )
        ).thenReturn(
                List.of(
                        view(
                                firstUserId,
                                agencyId,
                                true
                        ),
                        view(
                                secondUserId,
                                agencyId,
                                true
                        )
                )
        );

        mockMvc.perform(
                        get(
                                "/api/agencies/{agencyId}/users",
                                agencyId
                        )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.length()")
                                .value(2)
                )
                .andExpect(
                        jsonPath("$[0].active")
                                .value(true)
                )
                .andExpect(
                        jsonPath("$[1].active")
                                .value(true)
                );
    }

    @Test
    @WithMockUser
    void rejectsListWithoutPermission()
            throws Exception {

        UUID agencyId = UUID.randomUUID();

        when(
                identityAuthorization
                        .canAccessGlobal(
                                any(),
                                eq("USER_AGENCY_ASSIGN")
                        )
        ).thenReturn(false);

        mockMvc.perform(
                        get(
                                "/api/agencies/{agencyId}/users",
                                agencyId
                        )
                )
                .andExpect(
                        status().isForbidden()
                );

        verify(
                userAgencyAssignmentService,
                never()
        ).listActiveAssignments(any());
    }

    @Test
    @WithMockUser
    void returnsNotFoundForUnknownUser()
            throws Exception {

        UUID agencyId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(
                identityAuthorization
                        .canAccessGlobal(
                                any(),
                                eq("USER_AGENCY_ASSIGN")
                        )
        ).thenReturn(true);

        when(
                userAgencyAssignmentService
                        .assignUserToAgency(
                                userId,
                                agencyId
                        )
        ).thenThrow(
                new AgencyManagementException(
                        "USER_NOT_FOUND",
                        "The requested user was not found."
                )
        );

        mockMvc.perform(
                        post(
                                "/api/agencies/{agencyId}/users/{userId}",
                                agencyId,
                                userId
                        )
                                .with(csrf())
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
    @WithMockUser
    void returnsConflictForInactiveAgency()
            throws Exception {

        UUID agencyId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(
                identityAuthorization
                        .canAccessGlobal(
                                any(),
                                eq("USER_AGENCY_ASSIGN")
                        )
        ).thenReturn(true);

        when(
                userAgencyAssignmentService
                        .assignUserToAgency(
                                userId,
                                agencyId
                        )
        ).thenThrow(
                new AgencyManagementException(
                        "AGENCY_INACTIVE",
                        "Users cannot be assigned to an inactive agency."
                )
        );

        mockMvc.perform(
                        post(
                                "/api/agencies/{agencyId}/users/{userId}",
                                agencyId,
                                userId
                        )
                                .with(csrf())
                )
                .andExpect(
                        status().isConflict()
                )
                .andExpect(
                        jsonPath("$.code")
                                .value("AGENCY_INACTIVE")
                );
    }

    @Test
    @WithMockUser
    void returnsNotFoundForMissingAssignment()
            throws Exception {

        UUID agencyId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(
                identityAuthorization
                        .canAccessGlobal(
                                any(),
                                eq("USER_AGENCY_ASSIGN")
                        )
        ).thenReturn(true);

        when(
                userAgencyAssignmentService
                        .removeUserFromAgency(
                                userId,
                                agencyId
                        )
        ).thenThrow(
                new AgencyManagementException(
                        "USER_AGENCY_ASSIGNMENT_NOT_FOUND",
                        "The requested user-agency assignment was not found."
                )
        );

        mockMvc.perform(
                        post(
                                "/api/agencies/{agencyId}/users/{userId}/remove",
                                agencyId,
                                userId
                        )
                                .with(csrf())
                )
                .andExpect(
                        status().isNotFound()
                )
                .andExpect(
                        jsonPath("$.code")
                                .value(
                                        "USER_AGENCY_ASSIGNMENT_NOT_FOUND"
                                )
                );
    }

    @Test
    @WithMockUser
    void returnsConflictForAssignmentConflict()
            throws Exception {

        UUID agencyId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(
                identityAuthorization
                        .canAccessGlobal(
                                any(),
                                eq("USER_AGENCY_ASSIGN")
                        )
        ).thenReturn(true);

        when(
                userAgencyAssignmentService
                        .assignUserToAgency(
                                userId,
                                agencyId
                        )
        ).thenThrow(
                new AgencyManagementException(
                        "USER_AGENCY_ASSIGNMENT_CONFLICT",
                        "The user-agency assignment could not be created because of a concurrent conflict."
                )
        );

        mockMvc.perform(
                        post(
                                "/api/agencies/{agencyId}/users/{userId}",
                                agencyId,
                                userId
                        )
                                .with(csrf())
                )
                .andExpect(
                        status().isConflict()
                )
                .andExpect(
                        jsonPath("$.code")
                                .value(
                                        "USER_AGENCY_ASSIGNMENT_CONFLICT"
                                )
                );
    }

    private UserAgencyAssignmentView view(
            UUID userId,
            UUID agencyId,
            boolean active
    ) {
        Instant now = Instant.now();

        return new UserAgencyAssignmentView(
                UUID.randomUUID(),
                userId,
                agencyId,
                active,
                now,
                active ? null : now,
                now,
                now
        );
    }
}