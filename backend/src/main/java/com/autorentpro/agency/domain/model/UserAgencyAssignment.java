package com.autorentpro.agency.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "agency_user_assignments")
public class UserAgencyAssignment {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "agency_id", nullable = false, updatable = false)
    private UUID agencyId;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "assigned_at", nullable = false)
    private Instant assignedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected UserAgencyAssignment() {
        // Required by JPA.
    }

    private UserAgencyAssignment(
            UUID id,
            UUID userId,
            UUID agencyId,
            boolean active,
            Instant assignedAt,
            Instant endedAt,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.userId = Objects.requireNonNull(userId, "userId must not be null");
        this.agencyId = Objects.requireNonNull(agencyId, "agencyId must not be null");
        this.active = active;
        this.assignedAt = Objects.requireNonNull(assignedAt, "assignedAt must not be null");
        this.endedAt = endedAt;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");

        validateState();
    }

    public static UserAgencyAssignment assign(UUID userId, UUID agencyId) {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(agencyId, "agencyId must not be null");

        Instant now = Instant.now();

        return new UserAgencyAssignment(
                UUID.randomUUID(),
                userId,
                agencyId,
                true,
                now,
                null,
                now,
                now
        );
    }

    public void activate() {
        if (active) {
            return;
        }

        Instant now = Instant.now();

        this.active = true;
        this.assignedAt = now;
        this.endedAt = null;
        this.updatedAt = now;

        validateState();
    }

    public void deactivate() {
        if (!active) {
            return;
        }

        Instant now = Instant.now();

        this.active = false;
        this.endedAt = now;
        this.updatedAt = now;

        validateState();
    }

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }

        Instant now = Instant.now();

        if (assignedAt == null) {
            assignedAt = now;
        }

        if (createdAt == null) {
            createdAt = now;
        }

        if (updatedAt == null) {
            updatedAt = createdAt;
        }

        validateState();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
        validateState();
    }

    private void validateState() {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(agencyId, "agencyId must not be null");
        Objects.requireNonNull(assignedAt, "assignedAt must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");

        if (active && endedAt != null) {
            throw new IllegalStateException(
                    "An active user-agency assignment cannot have an endedAt timestamp"
            );
        }

        if (!active && endedAt == null) {
            throw new IllegalStateException(
                    "An inactive user-agency assignment must have an endedAt timestamp"
            );
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getAgencyId() {
        return agencyId;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getAssignedAt() {
        return assignedAt;
    }

    public Instant getEndedAt() {
        return endedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}