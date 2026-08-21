package com.autorentpro.agency.api;

import com.autorentpro.agency.application.AgencyAdministrationService;
import com.autorentpro.agency.application.AgencyManagementException;
import com.autorentpro.agency.application.AgencyView;
import com.autorentpro.agency.domain.model.AgencyStatus;
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
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = AgencyController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = ActiveAccountFilter.class
        )
)
@Import({
        AgencyApiExceptionHandler.class,
        AgencyControllerTest.MethodSecurityTestConfiguration.class
})
class AgencyControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    AgencyAdministrationService agencyAdministrationService;

    @MockBean(name = "identityAuthorization")
    IdentityAuthorization identityAuthorization;

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfiguration {
    }

    @Test
    @WithMockUser
    void createsAgencyWhenGlobalPermissionIsGranted()
            throws Exception {

        when(
                identityAuthorization
                        .canAccessGlobal(
                                any(),
                                eq("AGENCY_CREATE")
                        )
        ).thenReturn(true);

        when(
                agencyAdministrationService
                        .createAgency(
                                any(),
                                any(),
                                any(),
                                any(),
                                any(),
                                any(),
                                any(),
                                any(),
                                any(),
                                any()
                        )
        ).thenReturn(
                agencyView(
                        AgencyStatus.ACTIVE
                )
        );

        mockMvc.perform(
                        post("/api/agencies")
                                .with(csrf())
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        validCreateRequest()
                                )
                )
                .andExpect(
                        status().isCreated()
                )
                .andExpect(
                        jsonPath("$.code")
                                .value("CASA-CENTRE")
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("ACTIVE")
                );

        verify(
                agencyAdministrationService
        ).createAgency(
                "CASA-CENTRE",
                "Casablanca Centre",
                "1 Avenue Hassan II",
                null,
                "Casablanca",
                "20000",
                "MA",
                "+212500000000",
                "centre@autorent.ma",
                "Africa/Casablanca"
        );
    }

    @Test
    @WithMockUser
    void rejectsAgencyCreationWithoutGlobalPermission()
            throws Exception {

        when(
                identityAuthorization
                        .canAccessGlobal(
                                any(),
                                eq("AGENCY_CREATE")
                        )
        ).thenReturn(false);

        mockMvc.perform(
                        post("/api/agencies")
                                .with(csrf())
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        validCreateRequest()
                                )
                )
                .andExpect(
                        status().isForbidden()
                );

        verify(
                agencyAdministrationService,
                never()
        ).createAgency(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
        );
    }

    @Test
    @WithMockUser
    void readsAgencyWhenAgencyAccessIsGranted()
            throws Exception {

        UUID agencyId =
                UUID.randomUUID();

        when(
                identityAuthorization
                        .canAccessAgency(
                                any(),
                                eq("AGENCY_READ"),
                                eq(agencyId)
                        )
        ).thenReturn(true);

        when(
                agencyAdministrationService
                        .getAgency(
                                agencyId
                        )
        ).thenReturn(
                agencyView(
                        AgencyStatus.ACTIVE
                )
        );

        mockMvc.perform(
                        get(
                                "/api/agencies/{agencyId}",
                                agencyId
                        )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.code")
                                .value("CASA-CENTRE")
                );

        verify(
                agencyAdministrationService
        ).getAgency(
                agencyId
        );
    }

    @Test
    @WithMockUser
    void rejectsAgencyReadWhenAgencyAccessIsDenied()
            throws Exception {

        UUID agencyId =
                UUID.randomUUID();

        when(
                identityAuthorization
                        .canAccessAgency(
                                any(),
                                eq("AGENCY_READ"),
                                eq(agencyId)
                        )
        ).thenReturn(false);

        mockMvc.perform(
                        get(
                                "/api/agencies/{agencyId}",
                                agencyId
                        )
                )
                .andExpect(
                        status().isForbidden()
                );

        verify(
                agencyAdministrationService,
                never()
        ).getAgency(
                any()
        );
    }

    @Test
    @WithMockUser
    void updatesAgencyWhenAgencyAccessIsGranted()
            throws Exception {

        UUID agencyId =
                UUID.randomUUID();

        when(
                identityAuthorization
                        .canAccessAgency(
                                any(),
                                eq("AGENCY_UPDATE"),
                                eq(agencyId)
                        )
        ).thenReturn(true);

        when(
                agencyAdministrationService
                        .updateAgency(
                                eq(agencyId),
                                any(),
                                any(),
                                any(),
                                any(),
                                any(),
                                any(),
                                any(),
                                any(),
                                any()
                        )
        ).thenReturn(
                agencyView(
                        AgencyStatus.ACTIVE
                )
        );

        mockMvc.perform(
                        put(
                                "/api/agencies/{agencyId}",
                                agencyId
                        )
                                .with(csrf())
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        validUpdateRequest()
                                )
                )
                .andExpect(
                        status().isOk()
                );
    }

    @Test
    @WithMockUser
    void enablesAgencyOnlyWithGlobalPermission()
            throws Exception {

        UUID agencyId =
                UUID.randomUUID();

        when(
                identityAuthorization
                        .canAccessGlobal(
                                any(),
                                eq("AGENCY_ENABLE")
                        )
        ).thenReturn(true);

        when(
                agencyAdministrationService
                        .activateAgency(
                                agencyId
                        )
        ).thenReturn(
                agencyView(
                        AgencyStatus.ACTIVE
                )
        );

        mockMvc.perform(
                        post(
                                "/api/agencies/{agencyId}/enable",
                                agencyId
                        )
                                .with(csrf())
                )
                .andExpect(
                        status().isOk()
                );

        verify(
                agencyAdministrationService
        ).activateAgency(
                agencyId
        );
    }

    @Test
    @WithMockUser
    void disablesAgencyOnlyWithGlobalPermission()
            throws Exception {

        UUID agencyId =
                UUID.randomUUID();

        when(
                identityAuthorization
                        .canAccessGlobal(
                                any(),
                                eq("AGENCY_DISABLE")
                        )
        ).thenReturn(true);

        when(
                agencyAdministrationService
                        .deactivateAgency(
                                agencyId
                        )
        ).thenReturn(
                agencyView(
                        AgencyStatus.INACTIVE
                )
        );

        mockMvc.perform(
                        post(
                                "/api/agencies/{agencyId}/disable",
                                agencyId
                        )
                                .with(csrf())
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("INACTIVE")
                );

        verify(
                agencyAdministrationService
        ).deactivateAgency(
                agencyId
        );
    }

    @Test
    @WithMockUser
    void returnsBadRequestForInvalidAgencyRequest()
            throws Exception {

        when(
                identityAuthorization
                        .canAccessGlobal(
                                any(),
                                eq("AGENCY_CREATE")
                        )
        ).thenReturn(true);

        mockMvc.perform(
                        post("/api/agencies")
                                .with(csrf())
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "code": "",
                                          "name": "",
                                          "addressLine1": "",
                                          "city": "",
                                          "countryCode": "MAR",
                                          "timeZone": ""
                                        }
                                        """
                                )
                )
                .andExpect(
                        status().isBadRequest()
                )
                .andExpect(
                        jsonPath("$.code")
                                .value("VALIDATION_FAILED")
                );

        verify(
                agencyAdministrationService,
                never()
        ).createAgency(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
        );
    }

    @Test
    @WithMockUser
    void returnsNotFoundForUnknownAgency()
            throws Exception {

        UUID agencyId =
                UUID.randomUUID();

        when(
                identityAuthorization
                        .canAccessAgency(
                                any(),
                                eq("AGENCY_READ"),
                                eq(agencyId)
                        )
        ).thenReturn(true);

        when(
                agencyAdministrationService
                        .getAgency(
                                agencyId
                        )
        ).thenThrow(
                new AgencyManagementException(
                        "AGENCY_NOT_FOUND",
                        "The requested agency was not found."
                )
        );

        mockMvc.perform(
                        get(
                                "/api/agencies/{agencyId}",
                                agencyId
                        )
                )
                .andExpect(
                        status().isNotFound()
                )
                .andExpect(
                        jsonPath("$.code")
                                .value("AGENCY_NOT_FOUND")
                );
    }

    @Test
    @WithMockUser
    void returnsConflictForDuplicateAgencyCode()
            throws Exception {

        when(
                identityAuthorization
                        .canAccessGlobal(
                                any(),
                                eq("AGENCY_CREATE")
                        )
        ).thenReturn(true);

        when(
                agencyAdministrationService
                        .createAgency(
                                any(),
                                any(),
                                any(),
                                any(),
                                any(),
                                any(),
                                any(),
                                any(),
                                any(),
                                any()
                        )
        ).thenThrow(
                new AgencyManagementException(
                        "AGENCY_CODE_ALREADY_IN_USE",
                        "An agency already exists with this code."
                )
        );

        mockMvc.perform(
                        post("/api/agencies")
                                .with(csrf())
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        validCreateRequest()
                                )
                )
                .andExpect(
                        status().isConflict()
                )
                .andExpect(
                        jsonPath("$.code")
                                .value(
                                        "AGENCY_CODE_ALREADY_IN_USE"
                                )
                );
    }

    private static AgencyView agencyView(
            AgencyStatus status
    ) {
        Instant now =
                Instant.parse(
                        "2026-08-21T00:00:00Z"
                );

        return new AgencyView(
                UUID.randomUUID(),
                "CASA-CENTRE",
                "Casablanca Centre",
                "1 Avenue Hassan II",
                null,
                "Casablanca",
                "20000",
                "MA",
                "+212500000000",
                "centre@autorent.ma",
                "Africa/Casablanca",
                status,
                now,
                now
        );
    }

    private static String validCreateRequest() {
        return """
                {
                  "code": "CASA-CENTRE",
                  "name": "Casablanca Centre",
                  "addressLine1": "1 Avenue Hassan II",
                  "addressLine2": null,
                  "city": "Casablanca",
                  "postalCode": "20000",
                  "countryCode": "MA",
                  "phone": "+212500000000",
                  "email": "centre@autorent.ma",
                  "timeZone": "Africa/Casablanca"
                }
                """;
    }

    private static String validUpdateRequest() {
        return """
                {
                  "name": "Casablanca Centre Updated",
                  "addressLine1": "25 Avenue Hassan II",
                  "addressLine2": "Etage 2",
                  "city": "Casablanca",
                  "postalCode": "20250",
                  "countryCode": "MA",
                  "phone": "+212511111111",
                  "email": "updated@autorent.ma",
                  "timeZone": "Africa/Casablanca"
                }
                """;
    }
}