package com.autorentpro.identity.domain.model;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.Objects;

@Entity
@Table(
        name = "identity_user_roles",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_identity_user_roles_user_role",
                        columnNames = {"user_id", "role_id"}
                )
        }
)
public class UserRole extends AbstractAuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    protected UserRole() {
    }

    private UserRole(UserAccount user, Role role) {
        this.user = Objects.requireNonNull(
                user,
                "user must not be null"
        );

        this.role = Objects.requireNonNull(
                role,
                "role must not be null"
        );
    }

    public static UserRole assign(UserAccount user, Role role) {
        return new UserRole(user, role);
    }

    public UserAccount getUser() {
        return user;
    }

    public Role getRole() {
        return role;
    }
}