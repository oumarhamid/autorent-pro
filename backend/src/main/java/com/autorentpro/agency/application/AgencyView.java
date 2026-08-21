package com.autorentpro.agency.application;

import com.autorentpro.agency.domain.model.AgencyStatus;

import java.time.Instant;
import java.util.UUID;

public record AgencyView(
        UUID id,
        String code,
        String name,
        String addressLine1,
        String addressLine2,
        String city,
        String postalCode,
        String countryCode,
        String phone,
        String email,
        String timeZone,
        AgencyStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}