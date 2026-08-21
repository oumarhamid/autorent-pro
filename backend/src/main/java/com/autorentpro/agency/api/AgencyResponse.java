package com.autorentpro.agency.api;

import com.autorentpro.agency.application.AgencyView;
import com.autorentpro.agency.domain.model.AgencyStatus;

import java.time.Instant;
import java.util.UUID;

public record AgencyResponse(
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

    public static AgencyResponse from(
            AgencyView view
    ) {
        return new AgencyResponse(
                view.id(),
                view.code(),
                view.name(),
                view.addressLine1(),
                view.addressLine2(),
                view.city(),
                view.postalCode(),
                view.countryCode(),
                view.phone(),
                view.email(),
                view.timeZone(),
                view.status(),
                view.createdAt(),
                view.updatedAt()
        );
    }
}