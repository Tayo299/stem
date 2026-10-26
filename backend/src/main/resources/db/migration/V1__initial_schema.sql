CREATE TABLE districts (
    id UUID PRIMARY KEY,
    code VARCHAR(32) NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL
);
CREATE TABLE zones (
    id UUID PRIMARY KEY,
    district_id UUID NOT NULL REFERENCES districts(id),
    name VARCHAR(120) NOT NULL,
    CONSTRAINT uk_zone_district_name UNIQUE(district_id, name)
);
CREATE TABLE user_accounts (
    id UUID PRIMARY KEY,
    email VARCHAR(254) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_account_email UNIQUE(email)
);
CREATE TABLE schools (
    id UUID PRIMARY KEY,
    account_id UUID NOT NULL UNIQUE REFERENCES user_accounts(id),
    zone_id UUID NOT NULL REFERENCES zones(id),
    name VARCHAR(180) NOT NULL,
    registration_number VARCHAR(80) NOT NULL,
    address VARCHAR(255) NOT NULL
);
CREATE TABLE students (
    id UUID PRIMARY KEY,
    account_id UUID NOT NULL UNIQUE REFERENCES user_accounts(id),
    school_id UUID NOT NULL REFERENCES schools(id),
    student_number VARCHAR(30) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    date_of_birth DATE NOT NULL,
    CONSTRAINT uk_student_school_number UNIQUE(school_id, student_number)
);
CREATE TABLE question_banks (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    active BOOLEAN NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    created_by UUID REFERENCES user_accounts(id)
);
CREATE TABLE questions (
    id UUID PRIMARY KEY,
    bank_id UUID NOT NULL REFERENCES question_banks(id),
    prompt TEXT NOT NULL,
    subject VARCHAR(80) NOT NULL,
    points INTEGER NOT NULL CHECK(points > 0),
    options_json TEXT NOT NULL,
    correct_option VARCHAR(1) NOT NULL
);
CREATE TABLE exams (
    id UUID PRIMARY KEY,
    title VARCHAR(160) NOT NULL,
    bank_id UUID NOT NULL REFERENCES question_banks(id),
    opens_at TIMESTAMPTZ NOT NULL,
    closes_at TIMESTAMPTZ NOT NULL,
    duration_minutes INTEGER NOT NULL CHECK(duration_minutes > 0),
    question_count INTEGER NOT NULL CHECK(question_count > 0),
    anonymous_allowed BOOLEAN NOT NULL,
    status VARCHAR(24) NOT NULL
);
CREATE TABLE exam_assignments (
    id UUID PRIMARY KEY,
    exam_id UUID NOT NULL REFERENCES exams(id),
    question_id UUID NOT NULL REFERENCES questions(id),
    display_order INTEGER NOT NULL,
    CONSTRAINT uk_exam_question UNIQUE(exam_id, question_id)
);
CREATE TABLE exam_attempts (
    id UUID PRIMARY KEY,
    exam_id UUID NOT NULL REFERENCES exams(id),
    student_id UUID REFERENCES students(id),
    public_code VARCHAR(48) NOT NULL UNIQUE,
    started_at TIMESTAMPTZ NOT NULL,
    submitted_at TIMESTAMPTZ,
    status VARCHAR(20) NOT NULL,
    score INTEGER NOT NULL DEFAULT 0,
    possible_points INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT uk_exam_attempt_student UNIQUE(exam_id, student_id)
);
CREATE TABLE answers (
    id UUID PRIMARY KEY,
    attempt_id UUID NOT NULL REFERENCES exam_attempts(id),
    question_id UUID NOT NULL REFERENCES questions(id),
    selected_option VARCHAR(1) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_answer_attempt_question UNIQUE(attempt_id, question_id)
);
CREATE TABLE notifications (
    id UUID PRIMARY KEY,
    recipient_id UUID NOT NULL REFERENCES user_accounts(id),
    type VARCHAR(80) NOT NULL,
    message VARCHAR(500) NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL
);
CREATE TABLE audit_events (
    id UUID PRIMARY KEY,
    actor_id UUID REFERENCES user_accounts(id),
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL
);
CREATE TABLE document_uploads (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL REFERENCES user_accounts(id),
    storage_key VARCHAR(128) NOT NULL,
    original_name VARCHAR(180) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    size BIGINT NOT NULL,
    sha256 VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);
CREATE TABLE leaderboard_policies (
    id INTEGER PRIMARY KEY CHECK(id = 1),
    qualifiers_per_district INTEGER NOT NULL,
    tie_break_order VARCHAR(500) NOT NULL,
    configured BOOLEAN NOT NULL
);
INSERT INTO leaderboard_policies(id, qualifiers_per_district, tie_break_order, configured) VALUES(1, 0, '', FALSE);
CREATE TABLE portal_controls (
    key VARCHAR(80) PRIMARY KEY,
    enabled BOOLEAN NOT NULL
);
INSERT INTO portal_controls(key, enabled) VALUES('registration', TRUE), ('exam_access', TRUE), ('result_review', FALSE);
CREATE INDEX ix_student_school ON students(school_id);
CREATE UNIQUE INDEX uk_school_registration_number_ci ON schools(lower(registration_number));
CREATE UNIQUE INDEX uk_student_school_number_ci ON students(school_id, lower(student_number));
CREATE INDEX ix_attempt_exam_status ON exam_attempts(exam_id, status);
CREATE INDEX ix_notification_recipient ON notifications(recipient_id, created_at DESC);
