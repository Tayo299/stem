ALTER TABLE questions
    ALTER COLUMN correct_option DROP NOT NULL,
    ADD COLUMN source_number SMALLINT CHECK (source_number BETWEEN 1 AND 400);

ALTER TABLE questions
    ADD CONSTRAINT uk_questions_bank_source_number UNIQUE (bank_id, source_number);
