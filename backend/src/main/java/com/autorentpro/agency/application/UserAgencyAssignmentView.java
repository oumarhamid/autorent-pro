package com.autorentpro.agency.application;

import java.time.Instant;
import java.util.UUID;

public record UserAgencyAssignmentView(
        UUID id,
        UUID userId,
        UUID agencyId,
        boolean active,
        Instant assignedAt,
        Instant endedAt,
        Instant createdAt,
        Instant updatedAt
) {
}