package com.autorentpro.identity.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.Objects;

@Entity
@Table(
        name = "identity_role_permissions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_identity_role_permissions_role_permission_scope",
                        columnNames = {
                                "role_id",
                                "permission_id",
                                "scope"
                        }
                )
        }
)
public class RolePermission extends AbstractAuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "permission_id", nullable = false)
    private Permission permission;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope", nullable = false, length = 16)
    private PermissionScope scope;

    protected RolePermission() {
    }

    private RolePermission(
            Role role,
            Permission permission,
            PermissionScope scope
    ) {
        this.role = Objects.requireNonNull(
                role,
                "role must not be null"
        );

        this.permission = Objects.requireNonNull(
                permission,
                "permission must not be null"
        );

        this.scope = Objects.requireNonNull(
                scope,
                "scope must not be null"
        );
    }

    public static RolePermission grant(
            Role role,
            Permission permission,
            PermissionScope scope
    ) {
        return new RolePermission(
                role,
                permission,
                scope
        );
    }

    public Role getRole() {
        return role;
    }

    public Permission getPermission() {
        return permission;
    }

    public PermissionScope getScope() {
        return scope;
    }
}