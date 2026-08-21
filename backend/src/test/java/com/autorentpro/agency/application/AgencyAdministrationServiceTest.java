package com.autorentpro.agency.application;

import com.autorentpro.agency.domain.model.Agency;
import com.autorentpro.agency.domain.model.AgencyStatus;
import com.autorentpro.agency.domain.repository.AgencyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgencyAdministrationServiceTest {

    private AgencyRepository agencyRepository;

    private AgencyAdministrationService service;

    @BeforeEach
    void setUp() {
        agencyRepository =
                org.mockito.Mockito.mock(
                        AgencyRepository.class
                );

        service =
                new AgencyAdministrationService(
                        agencyRepository
                );
    }

    @Test
    void createsAgencyWithNormalizedBusinessCode() {
        when(
                agencyRepository.existsByCode(
                        "CASA-CENTRE"
                )
        ).thenReturn(false);

        when(
                agencyRepository.saveAndFlush(
                        any(Agency.class)
                )
        ).thenAnswer(
                invocation ->
                        invocation.getArgument(0)
        );

        AgencyView result =
                service.createAgency(
                        " casa-centre ",
                        "Casablanca Centre",
                        "1 Avenue Hassan II",
                        null,
                        "Casablanca",
                        "20000",
                        "ma",
                        "+212500000000",
                        " CENTRE@AUTORENT.MA ",
                        "Africa/Casablanca"
                );

        assertEquals(
                "CASA-CENTRE",
                result.code()
        );

        assertEquals(
                "MA",
                result.countryCode()
        );

        assertEquals(
                "centre@autorent.ma",
                result.email()
        );

        assertEquals(
                AgencyStatus.ACTIVE,
                result.status()
        );

        verify(
                agencyRepository
        ).existsByCode(
                "CASA-CENTRE"
        );

        verify(
                agencyRepository
        ).saveAndFlush(
                any(Agency.class)
        );
    }

    @Test
    void rejectsExistingAgencyCode() {
        when(
                agencyRepository.existsByCode(
                        "CASA-CENTRE"
                )
        ).thenReturn(true);

        AgencyManagementException exception =
                assertThrows(
                        AgencyManagementException.class,
                        () ->
                                service.createAgency(
                                        "CASA-CENTRE",
                                        "Casablanca Centre",
                                        "1 Avenue Hassan II",
                                        null,
                                        "Casablanca",
                                        "20000",
                                        "MA",
                                        null,
                                        null,
                                        "Africa/Casablanca"
                                )
                );

        assertEquals(
                "AGENCY_CODE_ALREADY_IN_USE",
                exception.getCode()
        );

        verify(
                agencyRepository,
                never()
        ).saveAndFlush(
                any(Agency.class)
        );
    }

    @Test
    void translatesConcurrentCodeConflict() {
        when(
                agencyRepository.existsByCode(
                        "CASA-CENTRE"
                )
        ).thenReturn(false);

        when(
                agencyRepository.saveAndFlush(
                        any(Agency.class)
                )
        ).thenThrow(
                new DataIntegrityViolationException(
                        "duplicate code"
                )
        );

        AgencyManagementException exception =
                assertThrows(
                        AgencyManagementException.class,
                        () ->
                                service.createAgency(
                                        "CASA-CENTRE",
                                        "Casablanca Centre",
                                        "1 Avenue Hassan II",
                                        null,
                                        "Casablanca",
                                        "20000",
                                        "MA",
                                        null,
                                        null,
                                        "Africa/Casablanca"
                                )
                );

        assertEquals(
                "AGENCY_CODE_ALREADY_IN_USE",
                exception.getCode()
        );
    }

    @Test
    void getsAgencyById() {
        Agency agency =
                createAgency();

        when(
                agencyRepository.findById(
                        agency.getId()
                )
        ).thenReturn(
                Optional.of(agency)
        );

        AgencyView result =
                service.getAgency(
                        agency.getId()
                );

        assertEquals(
                agency.getId(),
                result.id()
        );

        assertEquals(
                agency.getCode(),
                result.code()
        );
    }

    @Test
    void rejectsUnknownAgencyOnRead() {
        UUID agencyId =
                UUID.randomUUID();

        when(
                agencyRepository.findById(
                        agencyId
                )
        ).thenReturn(
                Optional.empty()
        );

        AgencyManagementException exception =
                assertThrows(
                        AgencyManagementException.class,
                        () ->
                                service.getAgency(
                                        agencyId
                                )
                );

        assertEquals(
                "AGENCY_NOT_FOUND",
                exception.getCode()
        );
    }

    @Test
    void updatesAgencyWithoutChangingBusinessIdentity() {
        Agency agency =
                createAgency();

        UUID agencyId =
                agency.getId();

        String originalCode =
                agency.getCode();

        when(
                agencyRepository.findForUpdateById(
                        agencyId
                )
        ).thenReturn(
                Optional.of(agency)
        );

        when(
                agencyRepository.saveAndFlush(
                        agency
                )
        ).thenReturn(
                agency
        );

        AgencyView result =
                service.updateAgency(
                        agencyId,
                        "Casablanca Centre Updated",
                        "25 Avenue Hassan II",
                        "Étage 2",
                        "Casablanca",
                        "20250",
                        "MA",
                        "+212511111111",
                        "updated@autorent.ma",
                        "Africa/Casablanca"
                );

        assertEquals(
                agencyId,
                result.id()
        );

        assertEquals(
                originalCode,
                result.code()
        );

        assertEquals(
                "Casablanca Centre Updated",
                result.name()
        );

        assertEquals(
                "Étage 2",
                result.addressLine2()
        );

        verify(
                agencyRepository
        ).saveAndFlush(
                agency
        );
    }

    @Test
    void deactivatesAgency() {
        Agency agency =
                createAgency();

        when(
                agencyRepository.findForUpdateById(
                        agency.getId()
                )
        ).thenReturn(
                Optional.of(agency)
        );

        when(
                agencyRepository.saveAndFlush(
                        agency
                )
        ).thenReturn(
                agency
        );

        AgencyView result =
                service.deactivateAgency(
                        agency.getId()
                );

        assertEquals(
                AgencyStatus.INACTIVE,
                result.status()
        );

        assertEquals(
                AgencyStatus.INACTIVE,
                agency.getStatus()
        );
    }

    @Test
    void activatesAgency() {
        Agency agency =
                createAgency();

        agency.deactivate();

        when(
                agencyRepository.findForUpdateById(
                        agency.getId()
                )
        ).thenReturn(
                Optional.of(agency)
        );

        when(
                agencyRepository.saveAndFlush(
                        agency
                )
        ).thenReturn(
                agency
        );

        AgencyView result =
                service.activateAgency(
                        agency.getId()
                );

        assertEquals(
                AgencyStatus.ACTIVE,
                result.status()
        );

        assertEquals(
                AgencyStatus.ACTIVE,
                agency.getStatus()
        );
    }

    @Test
    void rejectsUnknownAgencyForModification() {
        UUID agencyId =
                UUID.randomUUID();

        when(
                agencyRepository.findForUpdateById(
                        agencyId
                )
        ).thenReturn(
                Optional.empty()
        );

        AgencyManagementException exception =
                assertThrows(
                        AgencyManagementException.class,
                        () ->
                                service.deactivateAgency(
                                        agencyId
                                )
                );

        assertEquals(
                "AGENCY_NOT_FOUND",
                exception.getCode()
        );
    }

    @Test
    void rejectsNullAgencyIdentifier() {
        AgencyManagementException exception =
                assertThrows(
                        AgencyManagementException.class,
                        () ->
                                service.activateAgency(
                                        null
                                )
                );

        assertEquals(
                "AGENCY_NOT_FOUND",
                exception.getCode()
        );
    }

    private static Agency createAgency() {
        return Agency.create(
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
}