CREATE TABLE agency_user_assignments (
    id UUID PRIMARY KEY,

    user_id UUID NOT NULL,
    agency_id UUID NOT NULL,

    active BOOLEAN NOT NULL DEFAULT TRUE,

    assigned_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    ended_at TIMESTAMP(6) WITH TIME ZONE,

    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,

    CONSTRAINT uk_agency_user_assignments_user_agency
        UNIQUE (user_id, agency_id),

    CONSTRAINT fk_agency_user_assignments_user
        FOREIGN KEY (user_id)
        REFERENCES identity_users(id),

    CONSTRAINT fk_agency_user_assignments_agency
        FOREIGN KEY (agency_id)
        REFERENCES agency_agencies(id),

    CONSTRAINT chk_agency_user_assignments_dates
        CHECK (
            (active = TRUE AND ended_at IS NULL)
            OR
            (active = FALSE AND ended_at IS NOT NULL)
        )
);

CREATE INDEX idx_agency_user_assignments_user
    ON agency_user_assignments(user_id);

CREATE INDEX idx_agency_user_assignments_agency
    ON agency_user_assignments(agency_id);

CREATE INDEX idx_agency_user_assignments_active_agency
    ON agency_user_assignments(agency_id, active);

CREATE INDEX idx_agency_user_assignments_active_user
    ON agency_user_assignments(user_id, active);