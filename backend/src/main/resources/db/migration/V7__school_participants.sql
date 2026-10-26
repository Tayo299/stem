CREATE TABLE school_participants (
    id UUID PRIMARY KEY,
    school_id UUID NOT NULL REFERENCES schools(id),
    photo_document_id UUID NOT NULL UNIQUE REFERENCES document_uploads(id),
    full_name VARCHAR(200) NOT NULL,
    gender VARCHAR(40) NOT NULL,
    age INTEGER NOT NULL CHECK (age BETWEEN 1 AND 120),
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX ix_school_participants_school ON school_participants(school_id);
