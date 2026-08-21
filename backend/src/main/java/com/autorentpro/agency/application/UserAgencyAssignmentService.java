package com.autorentpro.agency.application;

import com.autorentpro.agency.domain.model.Agency;
import com.autorentpro.agency.domain.model.AgencyStatus;
import com.autorentpro.agency.domain.model.UserAgencyAssignment;
import com.autorentpro.agency.domain.repository.AgencyRepository;
import com.autorentpro.agency.domain.repository.UserAgencyAssignmentRepository;
import com.autorentpro.identity.application.IdentityUserLookup;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class UserAgencyAssignmentService {

    private final UserAgencyAssignmentRepository assignmentRepository;
    private final AgencyRepository agencyRepository;
    private final IdentityUserLookup identityUserLookup;

    public UserAgencyAssignmentService(
            UserAgencyAssignmentRepository assignmentRepository,
            AgencyRepository agencyRepository,
            IdentityUserLookup identityUserLookup
    ) {
        this.assignmentRepository =
                assignmentRepository;

        this.agencyRepository =
                agencyRepository;

        this.identityUserLookup =
                identityUserLookup;
    }

    @Transactional
    public UserAgencyAssignmentView assignUserToAgency(
            UUID userId,
            UUID agencyId
    ) {
        requireUser(userId);

        Agency agency =
                findAgencyForUpdate(agencyId);

        if (agency.getStatus() != AgencyStatus.ACTIVE) {
            throw agencyInactive();
        }

        return assignmentRepository
                .findForUpdateByUserIdAndAgencyId(
                        userId,
                        agencyId
                )
                .map(this::activateExistingAssignment)
                .orElseGet(
                        () -> createAssignment(
                                userId,
                                agencyId
                        )
                );
    }

    @Transactional
    public UserAgencyAssignmentView removeUserFromAgency(
            UUID userId,
            UUID agencyId
    ) {
        requireUser(userId);

        /*
         * Locking the agency serializes assignment changes
         * concerning the same agency.
         *
         * Removing an assignment remains allowed even when
         * the agency itself is inactive.
         */
        findAgencyForUpdate(agencyId);

        UserAgencyAssignment assignment =
                assignmentRepository
                        .findForUpdateByUserIdAndAgencyId(
                                userId,
                                agencyId
                        )
                        .orElseThrow(
                                this::assignmentNotFound
                        );

        if (!assignment.isActive()) {
            return toView(assignment);
        }

        assignment.deactivate();

        return toView(
                assignmentRepository.saveAndFlush(
                        assignment
                )
        );
    }

    @Transactional(readOnly = true)
    public List<UserAgencyAssignmentView>
    listActiveAssignments(
            UUID agencyId
    ) {
        requireAgency(agencyId);

        return assignmentRepository
                .findAllActiveByAgencyId(
                        agencyId
                )
                .stream()
                .map(this::toView)
                .toList();
    }

    private UserAgencyAssignmentView
    activateExistingAssignment(
            UserAgencyAssignment assignment
    ) {
        if (assignment.isActive()) {
            return toView(assignment);
        }

        assignment.activate();

        return toView(
                assignmentRepository.saveAndFlush(
                        assignment
                )
        );
    }

    private UserAgencyAssignmentView createAssignment(
            UUID userId,
            UUID agencyId
    ) {
        UserAgencyAssignment assignment =
                UserAgencyAssignment.assign(
                        userId,
                        agencyId
                );

        try {
            return toView(
                    assignmentRepository
                            .saveAndFlush(
                                    assignment
                            )
            );
        } catch (DataIntegrityViolationException exception) {
            /*
             * The agency row is locked before creation, so
             * normal application writes for the same agency
             * are serialized.
             *
             * The database unique constraint remains the
             * final protection against external/concurrent
             * writes that bypass this service.
             */
            throw assignmentConflict();
        }
    }

    private void requireUser(
            UUID userId
    ) {
        if (userId == null
                || !identityUserLookup.existsById(
                        userId
                )) {
            throw userNotFound();
        }
    }

    private void requireAgency(
            UUID agencyId
    ) {
        if (agencyId == null
                || agencyRepository
                        .findById(agencyId)
                        .isEmpty()) {
            throw agencyNotFound();
        }
    }

    private Agency findAgencyForUpdate(
            UUID agencyId
    ) {
        if (agencyId == null) {
            throw agencyNotFound();
        }

        return agencyRepository
                .findForUpdateById(
                        agencyId
                )
                .orElseThrow(
                        this::agencyNotFound
                );
    }

    private UserAgencyAssignmentView toView(
            UserAgencyAssignment assignment
    ) {
        return new UserAgencyAssignmentView(
                assignment.getId(),
                assignment.getUserId(),
                assignment.getAgencyId(),
                assignment.isActive(),
                assignment.getAssignedAt(),
                assignment.getEndedAt(),
                assignment.getCreatedAt(),
                assignment.getUpdatedAt()
        );
    }

    private AgencyManagementException userNotFound() {
        return new AgencyManagementException(
                "USER_NOT_FOUND",
                "The requested user was not found."
        );
    }

    private AgencyManagementException agencyNotFound() {
        return new AgencyManagementException(
                "AGENCY_NOT_FOUND",
                "The requested agency was not found."
        );
    }

    private AgencyManagementException agencyInactive() {
        return new AgencyManagementException(
                "AGENCY_INACTIVE",
                "Users cannot be assigned to an inactive agency."
        );
    }

    private AgencyManagementException assignmentNotFound() {
        return new AgencyManagementException(
                "USER_AGENCY_ASSIGNMENT_NOT_FOUND",
                "The requested user-agency assignment was not found."
        );
    }

    private AgencyManagementException assignmentConflict() {
        return new AgencyManagementException(
                "USER_AGENCY_ASSIGNMENT_CONFLICT",
                "The user-agency assignment could not be created because of a concurrent conflict."
        );
    }
}