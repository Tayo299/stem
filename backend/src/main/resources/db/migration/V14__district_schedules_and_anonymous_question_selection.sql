ALTER TABLE exams
    ADD COLUMN district_id UUID REFERENCES districts(id);

ALTER TABLE questions
    ADD COLUMN anonymous BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE exam_attempts
    ADD COLUMN school_participant_id UUID REFERENCES school_participants(id);

ALTER TABLE exam_attempts
    ADD CONSTRAINT ck_attempt_single_candidate
        CHECK (student_id IS NULL OR school_participant_id IS NULL);

CREATE UNIQUE INDEX uk_exam_attempt_school_participant
    ON exam_attempts(exam_id, school_participant_id)
    WHERE school_participant_id IS NOT NULL;

CREATE TABLE exam_attempt_questions (
    id UUID PRIMARY KEY,
    attempt_id UUID NOT NULL REFERENCES exam_attempts(id),
    question_id UUID NOT NULL REFERENCES questions(id),
    display_order INTEGER NOT NULL,
    CONSTRAINT uk_attempt_question UNIQUE (attempt_id, question_id),
    CONSTRAINT uk_attempt_question_order UNIQUE (attempt_id, display_order)
);

CREATE INDEX ix_exam_attempt_questions_attempt
    ON exam_attempt_questions(attempt_id, display_order);
