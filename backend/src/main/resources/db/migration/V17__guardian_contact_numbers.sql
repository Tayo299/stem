ALTER TABLE school_participants
    ADD COLUMN guardian_phone VARCHAR(40) NOT NULL DEFAULT '';

ALTER TABLE students
    ADD COLUMN guardian_phone VARCHAR(40) NOT NULL DEFAULT '';
