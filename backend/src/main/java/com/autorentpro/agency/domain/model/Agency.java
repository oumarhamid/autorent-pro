package com.autorentpro.agency.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Locale;
import java.util.UUID;

@Entity
@Table(name = "agency_agencies")
public class Agency {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, unique = true, length = 64, updatable = false)
    private String code;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(name = "address_line1", nullable = false, length = 255)
    private String addressLine1;

    @Column(name = "address_line2", length = 255)
    private String addressLine2;

    @Column(nullable = false, length = 120)
    private String city;

    @Column(name = "postal_code", length = 32)
    private String postalCode;

    @Column(name = "country_code", nullable = false, length = 2)
    private String countryCode;

    @Column(length = 32)
    private String phone;

    @Column(length = 320)
    private String email;

    @Column(name = "time_zone", nullable = false, length = 64)
    private String timeZone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AgencyStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Agency() {
        // Required by JPA.
    }

    private Agency(
            String code,
            String name,
            String addressLine1,
            String addressLine2,
            String city,
            String postalCode,
            String countryCode,
            String phone,
            String email,
            String timeZone
    ) {
        this.id = UUID.randomUUID();
        this.code = normalizeCode(code);
        this.status = AgencyStatus.ACTIVE;

        applyDetails(
                name,
                addressLine1,
                addressLine2,
                city,
                postalCode,
                countryCode,
                phone,
                email,
                timeZone
        );
    }

    public static Agency create(
            String code,
            String name,
            String addressLine1,
            String addressLine2,
            String city,
            String postalCode,
            String countryCode,
            String phone,
            String email,
            String timeZone
    ) {
        return new Agency(
                code,
                name,
                addressLine1,
                addressLine2,
                city,
                postalCode,
                countryCode,
                phone,
                email,
                timeZone
        );
    }

    public void updateDetails(
            String name,
            String addressLine1,
            String addressLine2,
            String city,
            String postalCode,
            String countryCode,
            String phone,
            String email,
            String timeZone
    ) {
        applyDetails(
                name,
                addressLine1,
                addressLine2,
                city,
                postalCode,
                countryCode,
                phone,
                email,
                timeZone
        );
    }

    public void activate() {
        this.status = AgencyStatus.ACTIVE;
    }

    public void deactivate() {
        this.status = AgencyStatus.INACTIVE;
    }

    private void applyDetails(
            String name,
            String addressLine1,
            String addressLine2,
            String city,
            String postalCode,
            String countryCode,
            String phone,
            String email,
            String timeZone
    ) {
        this.name = requireText(name, "name", 160);
        this.addressLine1 = requireText(addressLine1, "addressLine1", 255);
        this.addressLine2 = optionalText(addressLine2, "addressLine2", 255);
        this.city = requireText(city, "city", 120);
        this.postalCode = optionalText(postalCode, "postalCode", 32);
        this.countryCode = normalizeCountryCode(countryCode);
        this.phone = optionalText(phone, "phone", 32);
        this.email = normalizeEmail(email);
        this.timeZone = normalizeTimeZone(timeZone);
    }

    private static String normalizeCode(String value) {
        String normalized = requireText(value, "code", 64)
                .toUpperCase(Locale.ROOT);

        return normalized;
    }

    private static String normalizeCountryCode(String value) {
        String normalized = requireText(value, "countryCode", 2)
                .toUpperCase(Locale.ROOT);

        if (!normalized.matches("[A-Z]{2}")) {
            throw new IllegalArgumentException(
                    "countryCode must contain exactly two ASCII letters"
            );
        }

        return normalized;
    }

    private static String normalizeEmail(String value) {
        String normalized = optionalText(value, "email", 320);

        if (normalized == null) {
            return null;
        }

        return normalized.toLowerCase(Locale.ROOT);
    }

    private static String normalizeTimeZone(String value) {
        String normalized = requireText(value, "timeZone", 64);

        try {
            ZoneId.of(normalized);
        } catch (DateTimeException exception) {
            throw new IllegalArgumentException(
                    "timeZone must be a valid IANA time zone",
                    exception
            );
        }

        return normalized;
    }

    private static String requireText(
            String value,
            String fieldName,
            int maxLength
    ) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " is required");
        }

        String normalized = value.trim();

        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }

        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(
                    fieldName + " must not exceed " + maxLength + " characters"
            );
        }

        return normalized;
    }

    private static String optionalText(
            String value,
            String fieldName,
            int maxLength
    ) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();

        if (normalized.isEmpty()) {
            return null;
        }

        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(
                    fieldName + " must not exceed " + maxLength + " characters"
            );
        }

        return normalized;
    }

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();

        if (createdAt == null) {
            createdAt = now;
        }

        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getAddressLine1() {
        return addressLine1;
    }

    public String getAddressLine2() {
        return addressLine2;
    }

    public String getCity() {
        return city;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public String getPhone() {
        return phone;
    }

    public String getEmail() {
        return email;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public AgencyStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}