package com.autorentpro.agency.api;

import java.time.Instant;

public record AgencyErrorResponse(
        Instant timestamp,
        int status,
        String code,
        String message,
        String path
) {

    public static AgencyErrorResponse of(
            int status,
            String code,
            String message,
            String path
    ) {
        return new AgencyErrorResponse(
                Instant.now(),
                status,
                code,
                message,
                path
        );
    }
}