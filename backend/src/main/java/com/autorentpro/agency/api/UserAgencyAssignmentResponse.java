package com.autorentpro.agency.api;

import com.autorentpro.agency.application.UserAgencyAssignmentView;

import java.time.Instant;
import java.util.UUID;

public record UserAgencyAssignmentResponse(
        UUID id,
        UUID userId,
        UUID agencyId,
        boolean active,
        Instant assignedAt,
        Instant endedAt,
        Instant createdAt,
        Instant updatedAt
) {

    public static UserAgencyAssignmentResponse from(
            UserAgencyAssignmentView view
    ) {
        return new UserAgencyAssignmentResponse(
                view.id(),
                view.userId(),
                view.agencyId(),
                view.active(),
                view.assignedAt(),
                view.endedAt(),
                view.createdAt(),
                view.updatedAt()
        );
    }
}