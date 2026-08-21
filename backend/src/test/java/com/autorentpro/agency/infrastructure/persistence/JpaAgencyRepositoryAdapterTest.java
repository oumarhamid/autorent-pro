package com.autorentpro.agency.infrastructure.persistence;

import com.autorentpro.agency.domain.model.Agency;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JpaAgencyRepositoryAdapterTest {

    private SpringDataAgencyRepository springDataRepository;
    private JpaAgencyRepositoryAdapter repositoryAdapter;

    @BeforeEach
    void setUp() {
        springDataRepository =
                mock(SpringDataAgencyRepository.class);

        repositoryAdapter =
                new JpaAgencyRepositoryAdapter(
                        springDataRepository
                );
    }

    @Test
    void savesAgencyThroughSpringDataRepository() {
        Agency agency =
                createAgency();

        when(
                springDataRepository.save(agency)
        ).thenReturn(
                agency
        );

        Agency saved =
                repositoryAdapter.save(agency);

        assertSame(
                agency,
                saved
        );

        verify(
                springDataRepository
        ).save(
                agency
        );
    }

    @Test
    void savesAndFlushesAgencyThroughSpringDataRepository() {
        Agency agency =
                createAgency();

        when(
                springDataRepository.saveAndFlush(
                        agency
                )
        ).thenReturn(
                agency
        );

        Agency saved =
                repositoryAdapter.saveAndFlush(
                        agency
                );

        assertSame(
                agency,
                saved
        );

        verify(
                springDataRepository
        ).saveAndFlush(
                agency
        );
    }

    @Test
    void findsAgencyById() {
        Agency agency =
                createAgency();

        UUID agencyId =
                agency.getId();

        when(
                springDataRepository.findById(
                        agencyId
                )
        ).thenReturn(
                Optional.of(agency)
        );

        Optional<Agency> result =
                repositoryAdapter.findById(
                        agencyId
                );

        assertTrue(
                result.isPresent()
        );

        assertSame(
                agency,
                result.orElseThrow()
        );

        verify(
                springDataRepository
        ).findById(
                agencyId
        );
    }

    @Test
    void returnsEmptyWhenAgencyIdDoesNotExist() {
        UUID agencyId =
                UUID.randomUUID();

        when(
                springDataRepository.findById(
                        agencyId
                )
        ).thenReturn(
                Optional.empty()
        );

        Optional<Agency> result =
                repositoryAdapter.findById(
                        agencyId
                );

        assertTrue(
                result.isEmpty()
        );

        verify(
                springDataRepository
        ).findById(
                agencyId
        );
    }

    @Test
    void findsAgencyForUpdateById() {
        Agency agency =
                createAgency();

        UUID agencyId =
                agency.getId();

        when(
                springDataRepository.findForUpdateById(
                        agencyId
                )
        ).thenReturn(
                Optional.of(agency)
        );

        Optional<Agency> result =
                repositoryAdapter.findForUpdateById(
                        agencyId
                );

        assertTrue(
                result.isPresent()
        );

        assertSame(
                agency,
                result.orElseThrow()
        );

        verify(
                springDataRepository
        ).findForUpdateById(
                agencyId
        );
    }

    @Test
    void findsAgencyByCode() {
        Agency agency =
                createAgency();

        when(
                springDataRepository.findByCode(
                        "CASA-CENTRE"
                )
        ).thenReturn(
                Optional.of(agency)
        );

        Optional<Agency> result =
                repositoryAdapter.findByCode(
                        "CASA-CENTRE"
                );

        assertTrue(
                result.isPresent()
        );

        assertSame(
                agency,
                result.orElseThrow()
        );

        verify(
                springDataRepository
        ).findByCode(
                "CASA-CENTRE"
        );
    }

    @Test
    void checksWhetherAgencyCodeExists() {
        when(
                springDataRepository.existsByCode(
                        "CASA-CENTRE"
                )
        ).thenReturn(
                true
        );

        assertTrue(
                repositoryAdapter.existsByCode(
                        "CASA-CENTRE"
                )
        );

        verify(
                springDataRepository
        ).existsByCode(
                "CASA-CENTRE"
        );
    }

    @Test
    void returnsFalseWhenAgencyCodeDoesNotExist() {
        when(
                springDataRepository.existsByCode(
                        "RABAT-AGDAL"
                )
        ).thenReturn(
                false
        );

        assertFalse(
                repositoryAdapter.existsByCode(
                        "RABAT-AGDAL"
                )
        );

        verify(
                springDataRepository
        ).existsByCode(
                "RABAT-AGDAL"
        );
    }

    @Test
    void rejectsNullAgencyOnSave() {
        assertThrows(
                NullPointerException.class,
                () ->
                        repositoryAdapter.save(
                                null
                        )
        );
    }

    @Test
    void rejectsNullAgencyOnSaveAndFlush() {
        assertThrows(
                NullPointerException.class,
                () ->
                        repositoryAdapter.saveAndFlush(
                                null
                        )
        );
    }

    @Test
    void rejectsNullAgencyId() {
        assertThrows(
                NullPointerException.class,
                () ->
                        repositoryAdapter.findById(
                                null
                        )
        );
    }

    @Test
    void rejectsNullAgencyIdForUpdateLookup() {
        assertThrows(
                NullPointerException.class,
                () ->
                        repositoryAdapter.findForUpdateById(
                                null
                        )
        );
    }

    @Test
    void rejectsNullCodeLookup() {
        assertThrows(
                NullPointerException.class,
                () ->
                        repositoryAdapter.findByCode(
                                null
                        )
        );
    }

    @Test
    void rejectsNullCodeExistenceCheck() {
        assertThrows(
                NullPointerException.class,
                () ->
                        repositoryAdapter.existsByCode(
                                null
                        )
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