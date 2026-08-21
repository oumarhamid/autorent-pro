package com.autorentpro.agency.application;

import com.autorentpro.agency.domain.model.Agency;
import com.autorentpro.agency.domain.model.UserAgencyAssignment;
import com.autorentpro.agency.domain.repository.AgencyRepository;
import com.autorentpro.agency.domain.repository.UserAgencyAssignmentRepository;
import com.autorentpro.identity.application.IdentityUserLookup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserAgencyAssignmentServiceTest {

    @Mock
    UserAgencyAssignmentRepository assignmentRepository;

    @Mock
    AgencyRepository agencyRepository;

    @Mock
    IdentityUserLookup identityUserLookup;

    UserAgencyAssignmentService service;

    @BeforeEach
    void setUp() {
        service =
                new UserAgencyAssignmentService(
                        assignmentRepository,
                        agencyRepository,
                        identityUserLookup
                );
    }

    @Test
    void createsNewAssignment() {
        UUID userId = UUID.randomUUID();
        Agency agency = agency();

        when(identityUserLookup.existsById(userId))
                .thenReturn(true);

        when(agencyRepository.findForUpdateById(
                agency.getId()
        )).thenReturn(
                Optional.of(agency)
        );

        when(
                assignmentRepository
                        .findForUpdateByUserIdAndAgencyId(
                                userId,
                                agency.getId()
                        )
        ).thenReturn(
                Optional.empty()
        );

        when(assignmentRepository.saveAndFlush(any()))
                .thenAnswer(
                        invocation ->
                                invocation.getArgument(0)
                );

        UserAgencyAssignmentView result =
                service.assignUserToAgency(
                        userId,
                        agency.getId()
                );

        assertThat(result.userId())
                .isEqualTo(userId);

        assertThat(result.agencyId())
                .isEqualTo(agency.getId());

        assertThat(result.active()).isTrue();
        assertThat(result.endedAt()).isNull();

        verify(assignmentRepository)
                .saveAndFlush(any());
    }

    @Test
    void activeAssignmentIsIdempotent() {
        UUID userId = UUID.randomUUID();
        Agency agency = agency();

        UserAgencyAssignment assignment =
                UserAgencyAssignment.assign(
                        userId,
                        agency.getId()
                );

        when(identityUserLookup.existsById(userId))
                .thenReturn(true);

        when(agencyRepository.findForUpdateById(
                agency.getId()
        )).thenReturn(
                Optional.of(agency)
        );

        when(
                assignmentRepository
                        .findForUpdateByUserIdAndAgencyId(
                                userId,
                                agency.getId()
                        )
        ).thenReturn(
                Optional.of(assignment)
        );

        UserAgencyAssignmentView result =
                service.assignUserToAgency(
                        userId,
                        agency.getId()
                );

        assertThat(result.active()).isTrue();

        verify(
                assignmentRepository,
                never()
        ).saveAndFlush(any());
    }

    @Test
    void inactiveAssignmentIsReactivated() {
        UUID userId = UUID.randomUUID();
        Agency agency = agency();

        UserAgencyAssignment assignment =
                UserAgencyAssignment.assign(
                        userId,
                        agency.getId()
                );

        assignment.deactivate();

        when(identityUserLookup.existsById(userId))
                .thenReturn(true);

        when(agencyRepository.findForUpdateById(
                agency.getId()
        )).thenReturn(
                Optional.of(agency)
        );

        when(
                assignmentRepository
                        .findForUpdateByUserIdAndAgencyId(
                                userId,
                                agency.getId()
                        )
        ).thenReturn(
                Optional.of(assignment)
        );

        when(assignmentRepository.saveAndFlush(
                assignment
        )).thenReturn(
                assignment
        );

        UserAgencyAssignmentView result =
                service.assignUserToAgency(
                        userId,
                        agency.getId()
                );

        assertThat(result.active()).isTrue();
        assertThat(result.endedAt()).isNull();

        verify(assignmentRepository)
                .saveAndFlush(assignment);
    }

    @Test
    void rejectsUnknownUser() {
        UUID userId = UUID.randomUUID();
        UUID agencyId = UUID.randomUUID();

        when(identityUserLookup.existsById(userId))
                .thenReturn(false);

        assertThatThrownBy(
                () ->
                        service.assignUserToAgency(
                                userId,
                                agencyId
                        )
        )
                .isInstanceOf(
                        AgencyManagementException.class
                )
                .extracting("code")
                .isEqualTo("USER_NOT_FOUND");

        verify(
                agencyRepository,
                never()
        ).findForUpdateById(any());
    }

    @Test
    void rejectsUnknownAgency() {
        UUID userId = UUID.randomUUID();
        UUID agencyId = UUID.randomUUID();

        when(identityUserLookup.existsById(userId))
                .thenReturn(true);

        when(
                agencyRepository.findForUpdateById(
                        agencyId
                )
        ).thenReturn(
                Optional.empty()
        );

        assertThatThrownBy(
                () ->
                        service.assignUserToAgency(
                                userId,
                                agencyId
                        )
        )
                .isInstanceOf(
                        AgencyManagementException.class
                )
                .extracting("code")
                .isEqualTo("AGENCY_NOT_FOUND");
    }

    @Test
    void rejectsAssignmentToInactiveAgency() {
        UUID userId = UUID.randomUUID();

        Agency agency = agency();
        agency.deactivate();

        when(identityUserLookup.existsById(userId))
                .thenReturn(true);

        when(agencyRepository.findForUpdateById(
                agency.getId()
        )).thenReturn(
                Optional.of(agency)
        );

        assertThatThrownBy(
                () ->
                        service.assignUserToAgency(
                                userId,
                                agency.getId()
                        )
        )
                .isInstanceOf(
                        AgencyManagementException.class
                )
                .extracting("code")
                .isEqualTo("AGENCY_INACTIVE");

        verify(
                assignmentRepository,
                never()
        ).saveAndFlush(any());
    }

    @Test
    void removesActiveAssignment() {
        UUID userId = UUID.randomUUID();
        Agency agency = agency();

        UserAgencyAssignment assignment =
                UserAgencyAssignment.assign(
                        userId,
                        agency.getId()
                );

        when(identityUserLookup.existsById(userId))
                .thenReturn(true);

        when(agencyRepository.findForUpdateById(
                agency.getId()
        )).thenReturn(
                Optional.of(agency)
        );

        when(
                assignmentRepository
                        .findForUpdateByUserIdAndAgencyId(
                                userId,
                                agency.getId()
                        )
        ).thenReturn(
                Optional.of(assignment)
        );

        when(assignmentRepository.saveAndFlush(
                assignment
        )).thenReturn(
                assignment
        );

        UserAgencyAssignmentView result =
                service.removeUserFromAgency(
                        userId,
                        agency.getId()
                );

        assertThat(result.active()).isFalse();
        assertThat(result.endedAt()).isNotNull();

        verify(assignmentRepository)
                .saveAndFlush(assignment);
    }

    @Test
    void removingInactiveAssignmentIsIdempotent() {
        UUID userId = UUID.randomUUID();
        Agency agency = agency();

        UserAgencyAssignment assignment =
                UserAgencyAssignment.assign(
                        userId,
                        agency.getId()
                );

        assignment.deactivate();

        when(identityUserLookup.existsById(userId))
                .thenReturn(true);

        when(agencyRepository.findForUpdateById(
                agency.getId()
        )).thenReturn(
                Optional.of(agency)
        );

        when(
                assignmentRepository
                        .findForUpdateByUserIdAndAgencyId(
                                userId,
                                agency.getId()
                        )
        ).thenReturn(
                Optional.of(assignment)
        );

        UserAgencyAssignmentView result =
                service.removeUserFromAgency(
                        userId,
                        agency.getId()
                );

        assertThat(result.active()).isFalse();

        verify(
                assignmentRepository,
                never()
        ).saveAndFlush(any());
    }

    @Test
    void rejectsRemovalOfMissingAssignment() {
        UUID userId = UUID.randomUUID();
        Agency agency = agency();

        when(identityUserLookup.existsById(userId))
                .thenReturn(true);

        when(agencyRepository.findForUpdateById(
                agency.getId()
        )).thenReturn(
                Optional.of(agency)
        );

        when(
                assignmentRepository
                        .findForUpdateByUserIdAndAgencyId(
                                userId,
                                agency.getId()
                        )
        ).thenReturn(
                Optional.empty()
        );

        assertThatThrownBy(
                () ->
                        service.removeUserFromAgency(
                                userId,
                                agency.getId()
                        )
        )
                .isInstanceOf(
                        AgencyManagementException.class
                )
                .extracting("code")
                .isEqualTo(
                        "USER_AGENCY_ASSIGNMENT_NOT_FOUND"
                );
    }

    @Test
    void listsActiveAssignments() {
        Agency agency = agency();

        UserAgencyAssignment first =
                UserAgencyAssignment.assign(
                        UUID.randomUUID(),
                        agency.getId()
                );

        UserAgencyAssignment second =
                UserAgencyAssignment.assign(
                        UUID.randomUUID(),
                        agency.getId()
                );

        when(
                agencyRepository.findById(
                        agency.getId()
                )
        ).thenReturn(
                Optional.of(agency)
        );

        when(
                assignmentRepository
                        .findAllActiveByAgencyId(
                                agency.getId()
                        )
        ).thenReturn(
                List.of(
                        first,
                        second
                )
        );

        List<UserAgencyAssignmentView> result =
                service.listActiveAssignments(
                        agency.getId()
                );

        assertThat(result).hasSize(2);

        assertThat(result)
                .allMatch(
                        UserAgencyAssignmentView::active
                );
    }

    @Test
    void translatesDatabaseCreationConflict() {
        UUID userId = UUID.randomUUID();
        Agency agency = agency();

        when(identityUserLookup.existsById(userId))
                .thenReturn(true);

        when(agencyRepository.findForUpdateById(
                agency.getId()
        )).thenReturn(
                Optional.of(agency)
        );

        when(
                assignmentRepository
                        .findForUpdateByUserIdAndAgencyId(
                                userId,
                                agency.getId()
                        )
        ).thenReturn(
                Optional.empty()
        );

        when(assignmentRepository.saveAndFlush(any()))
                .thenThrow(
                        new DataIntegrityViolationException(
                                "duplicate"
                        )
                );

        assertThatThrownBy(
                () ->
                        service.assignUserToAgency(
                                userId,
                                agency.getId()
                        )
        )
                .isInstanceOf(
                        AgencyManagementException.class
                )
                .extracting("code")
                .isEqualTo(
                        "USER_AGENCY_ASSIGNMENT_CONFLICT"
                );
    }

    private Agency agency() {
        return Agency.create(
                "CASA-CENTER-" + UUID.randomUUID(),
                "Casablanca Center",
                "10 Avenue Principale",
                null,
                "Casablanca",
                "20000",
                "MA",
                null,
                null,
                "Africa/Casablanca"
        );
    }
}