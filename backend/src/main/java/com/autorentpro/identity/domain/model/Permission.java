package com.autorentpro.identity.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.util.Objects;

@Entity
@Table(name = "identity_permissions")
public class Permission extends AbstractAuditableEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "code", nullable = false, unique = true, length = 96)
    private PermissionCode code;

    protected Permission() {
    }

    private Permission(PermissionCode code) {
        this.code = Objects.requireNonNull(
                code,
                "code must not be null"
        );
    }

    public static Permission create(PermissionCode code) {
        return new Permission(code);
    }

    public PermissionCode getCode() {
        return code;
    }
}