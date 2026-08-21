package com.autorentpro.agency.application;

import java.util.Objects;

public class AgencyManagementException
        extends RuntimeException {

    private final String code;

    public AgencyManagementException(
            String code,
            String message
    ) {
        super(message);

        this.code = Objects.requireNonNull(
                code,
                "code must not be null"
        );
    }

    public String getCode() {
        return code;
    }
}