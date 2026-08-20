package com.autorentpro.identity.api;

import java.time.Instant;

public record SecurityErrorResponse(
        Instant timestamp,
        int status,
        String code,
        String message,
        String path
) {

    public static SecurityErrorResponse of(
            int status,
            String code,
            String message,
            String path
    ) {
        return new SecurityErrorResponse(
                Instant.now(),
                status,
                code,
                message,
                path
        );
    }
}