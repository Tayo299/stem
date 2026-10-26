ALTER TABLE exam_attempts
    ADD COLUMN released BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE leaderboard_publications (
    exam_id UUID PRIMARY KEY REFERENCES exams(id),
    selected_school_ids TEXT NOT NULL DEFAULT '',
    published BOOLEAN NOT NULL DEFAULT FALSE
);
