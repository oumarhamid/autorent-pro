package com.autorentpro.identity.api;

import com.autorentpro.identity.domain.model.RoleCode;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(

        @NotBlank
        @Email
        @Size(max = 320)
        String email,

        @NotBlank
        @Size(max = 256)
        String temporaryPassword,

        @NotNull
        RoleCode initialRole
) {

    public CreateUserRequest {
        if (email != null) {
            email = email.trim();
        }
    }
}