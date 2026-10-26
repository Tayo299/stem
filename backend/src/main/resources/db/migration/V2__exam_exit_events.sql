CREATE TABLE exam_events (
    id UUID PRIMARY KEY,
    attempt_id UUID NOT NULL REFERENCES exam_attempts(id),
    reason VARCHAR(32) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX ix_exam_event_attempt_time ON exam_events(attempt_id, occurred_at);
