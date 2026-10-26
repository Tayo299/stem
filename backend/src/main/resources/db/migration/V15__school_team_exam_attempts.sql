ALTER TABLE exam_attempts
    ADD COLUMN school_id UUID REFERENCES schools(id);

ALTER TABLE exam_attempts
    ADD CONSTRAINT ck_exam_attempt_single_identity
        CHECK (num_nonnulls(student_id, school_participant_id, school_id) <= 1);

CREATE UNIQUE INDEX uk_exam_attempt_school
    ON exam_attempts(exam_id, school_id)
    WHERE school_id IS NOT NULL;