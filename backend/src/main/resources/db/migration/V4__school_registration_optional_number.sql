ALTER TABLE schools
    ALTER COLUMN registration_number DROP NOT NULL;

CREATE UNIQUE INDEX uk_school_zone_name_ci
    ON schools(zone_id, lower(name));
