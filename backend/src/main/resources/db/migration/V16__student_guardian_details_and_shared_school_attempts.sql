ALTER TABLE school_participants
    ADD COLUMN guardian_name VARCHAR(200) NOT NULL DEFAULT '',
    ADD COLUMN home_address VARCHAR(500) NOT NULL DEFAULT '';

ALTER TABLE students
    ADD COLUMN parent_guardian_name VARCHAR(200) NOT NULL DEFAULT '',
    ADD COLUMN home_address VARCHAR(500) NOT NULL DEFAULT '';

CREATE TABLE exam_attempt_participants (
    attempt_id UUID NOT NULL REFERENCES exam_attempts(id) ON DELETE CASCADE,
    participant_id UUID NOT NULL REFERENCES school_participants(id),
    PRIMARY KEY (attempt_id, participant_id)
);

CREATE INDEX ix_exam_attempt_participants_participant
    ON exam_attempt_participants(participant_id);

UPDATE sponsors
SET active = FALSE, updated_at = now()
WHERE lower(name) = 'monika';
