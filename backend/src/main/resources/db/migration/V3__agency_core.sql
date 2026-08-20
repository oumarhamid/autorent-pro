CREATE TABLE agency_agencies (
    id UUID PRIMARY KEY,

    code VARCHAR(64) NOT NULL,
    name VARCHAR(160) NOT NULL,

    address_line1 VARCHAR(255) NOT NULL,
    address_line2 VARCHAR(255),
    city VARCHAR(120) NOT NULL,
    postal_code VARCHAR(32),
    country_code CHAR(2) NOT NULL,

    phone VARCHAR(32),
    email VARCHAR(320),

    time_zone VARCHAR(64) NOT NULL,

    status VARCHAR(16) NOT NULL,

    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,

    CONSTRAINT uk_agency_agencies_code
        UNIQUE (code),

    CONSTRAINT chk_agency_agencies_code_non_blank
        CHECK (LENGTH(BTRIM(code)) > 0),

    CONSTRAINT chk_agency_agencies_code_normalized
        CHECK (code = UPPER(BTRIM(code))),

    CONSTRAINT chk_agency_agencies_name_non_blank
        CHECK (LENGTH(BTRIM(name)) > 0),

    CONSTRAINT chk_agency_agencies_name_trimmed
        CHECK (name = BTRIM(name)),

    CONSTRAINT chk_agency_agencies_address_line1_non_blank
        CHECK (LENGTH(BTRIM(address_line1)) > 0),

    CONSTRAINT chk_agency_agencies_address_line1_trimmed
        CHECK (address_line1 = BTRIM(address_line1)),

    CONSTRAINT chk_agency_agencies_address_line2
        CHECK (
            address_line2 IS NULL
            OR (
                LENGTH(BTRIM(address_line2)) > 0
                AND address_line2 = BTRIM(address_line2)
            )
        ),

    CONSTRAINT chk_agency_agencies_city_non_blank
        CHECK (LENGTH(BTRIM(city)) > 0),

    CONSTRAINT chk_agency_agencies_city_trimmed
        CHECK (city = BTRIM(city)),

    CONSTRAINT chk_agency_agencies_postal_code
        CHECK (
            postal_code IS NULL
            OR (
                LENGTH(BTRIM(postal_code)) > 0
                AND postal_code = BTRIM(postal_code)
            )
        ),

    CONSTRAINT chk_agency_agencies_country_code
        CHECK (country_code ~ '^[A-Z]{2}$'),

    CONSTRAINT chk_agency_agencies_phone
        CHECK (
            phone IS NULL
            OR (
                LENGTH(BTRIM(phone)) > 0
                AND phone = BTRIM(phone)
            )
        ),

    CONSTRAINT chk_agency_agencies_email
        CHECK (
            email IS NULL
            OR (
                LENGTH(BTRIM(email)) > 0
                AND email = LOWER(BTRIM(email))
            )
        ),

    CONSTRAINT chk_agency_agencies_time_zone_non_blank
        CHECK (LENGTH(BTRIM(time_zone)) > 0),

    CONSTRAINT chk_agency_agencies_time_zone_trimmed
        CHECK (time_zone = BTRIM(time_zone)),

    CONSTRAINT chk_agency_agencies_status
        CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE INDEX idx_agency_agencies_status
    ON agency_agencies(status);

CREATE INDEX idx_agency_agencies_country_city
    ON agency_agencies(country_code, city);