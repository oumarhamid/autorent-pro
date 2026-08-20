package com.autorentpro.identity.application;

import java.util.Objects;

public class PasswordChangeException extends RuntimeException {

    private final String code;

    public PasswordChangeException(
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