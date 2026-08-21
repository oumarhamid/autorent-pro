package com.autorentpro.agency.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateAgencyRequest(

        @NotBlank
        @Size(max = 160)
        String name,

        @NotBlank
        @Size(max = 255)
        String addressLine1,

        @Size(max = 255)
        String addressLine2,

        @NotBlank
        @Size(max = 120)
        String city,

        @Size(max = 32)
        String postalCode,

        @NotBlank
        @Pattern(regexp = "(?i)[A-Z]{2}")
        String countryCode,

        @Size(max = 32)
        String phone,

        @Email
        @Size(max = 320)
        String email,

        @NotBlank
        @Size(max = 64)
        String timeZone
) {
}