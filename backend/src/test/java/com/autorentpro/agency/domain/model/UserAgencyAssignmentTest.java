package com.autorentpro.agency.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class UserAgencyAssignmentTest {

    @Test
    void assignsUserToAgencyAsActiveAssignment() {
        UUID userId = UUID.randomUUID();
        UUID agencyId = UUID.randomUUID();

        UserAgencyAssignment assignment =
                UserAgencyAssignment.assign(userId, agencyId);

        assertThat(assignment.getId()).isNotNull();
        assertThat(assignment.getUserId()).isEqualTo(userId);
        assertThat(assignment.getAgencyId()).isEqualTo(agencyId);
        assertThat(assignment.isActive()).isTrue();
        assertThat(assignment.getAssignedAt()).isNotNull();
        assertThat(assignment.getEndedAt()).isNull();
        assertThat(assignment.getCreatedAt()).isNotNull();
        assertThat(assignment.getUpdatedAt()).isNotNull();
    }

    @Test
    void rejectsNullUserId() {
        assertThatNullPointerException()
                .isThrownBy(() ->
                        UserAgencyAssignment.assign(
                                null,
                                UUID.randomUUID()
                        )
                )
                .withMessage("userId must not be null");
    }

    @Test
    void rejectsNullAgencyId() {
        assertThatNullPointerException()
                .isThrownBy(() ->
                        UserAgencyAssignment.assign(
                                UUID.randomUUID(),
                                null
                        )
                )
                .withMessage("agencyId must not be null");
    }

    @Test
    void deactivatesAssignment() {
        UserAgencyAssignment assignment =
                UserAgencyAssignment.assign(
                        UUID.randomUUID(),
                        UUID.randomUUID()
                );

        Instant previousUpdatedAt = assignment.getUpdatedAt();

        assignment.deactivate();

        assertThat(assignment.isActive()).isFalse();
        assertThat(assignment.getEndedAt()).isNotNull();
        assertThat(assignment.getUpdatedAt())
                .isAfterOrEqualTo(previousUpdatedAt);
    }

    @Test
    void deactivationIsIdempotent() {
        UserAgencyAssignment assignment =
                UserAgencyAssignment.assign(
                        UUID.randomUUID(),
                        UUID.randomUUID()
                );

        assignment.deactivate();

        Instant endedAt = assignment.getEndedAt();
        Instant updatedAt = assignment.getUpdatedAt();

        assignment.deactivate();

        assertThat(assignment.isActive()).isFalse();
        assertThat(assignment.getEndedAt()).isEqualTo(endedAt);
        assertThat(assignment.getUpdatedAt()).isEqualTo(updatedAt);
    }

    @Test
    void reactivatesInactiveAssignment() {
        UserAgencyAssignment assignment =
                UserAgencyAssignment.assign(
                        UUID.randomUUID(),
                        UUID.randomUUID()
                );

        assignment.deactivate();

        Instant previousAssignedAt = assignment.getAssignedAt();

        assignment.activate();

        assertThat(assignment.isActive()).isTrue();
        assertThat(assignment.getEndedAt()).isNull();
        assertThat(assignment.getAssignedAt())
                .isEqualTo(previousAssignedAt);
    }

    @Test
    void activationIsIdempotentWhenAlreadyActive() {
        UserAgencyAssignment assignment =
                UserAgencyAssignment.assign(
                        UUID.randomUUID(),
                        UUID.randomUUID()
                );

        Instant assignedAt = assignment.getAssignedAt();
        Instant updatedAt = assignment.getUpdatedAt();

        assignment.activate();

        assertThat(assignment.isActive()).isTrue();
        assertThat(assignment.getAssignedAt()).isEqualTo(assignedAt);
        assertThat(assignment.getUpdatedAt()).isEqualTo(updatedAt);
        assertThat(assignment.getEndedAt()).isNull();
    }
}