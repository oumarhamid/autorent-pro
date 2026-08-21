ALTER TABLE agency_agencies
    ALTER COLUMN country_code TYPE VARCHAR(2)
    USING BTRIM(country_code);