package com.autorentpro.identity.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.util.Objects;

@Entity
@Table(name = "identity_roles")
public class Role extends AbstractAuditableEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "code", nullable = false, unique = true, length = 64)
    private RoleCode code;

    protected Role() {
    }

    private Role(RoleCode code) {
        this.code = Objects.requireNonNull(
                code,
                "code must not be null"
        );
    }

    public static Role create(RoleCode code) {
        return new Role(code);
    }

    public RoleCode getCode() {
        return code;
    }
}