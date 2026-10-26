ALTER TABLE schools
    ADD COLUMN principal_name VARCHAR(180),
    ADD COLUMN principal_email VARCHAR(254),
    ADD COLUMN principal_phone VARCHAR(40);

ALTER TABLE students
    ADD COLUMN full_name VARCHAR(200),
    ADD COLUMN age INTEGER,
    ADD COLUMN gender VARCHAR(40);

UPDATE students SET full_name = trim(first_name || ' ' || last_name);
ALTER TABLE students ALTER COLUMN full_name SET NOT NULL;
ALTER TABLE students ALTER COLUMN date_of_birth DROP NOT NULL;

ALTER TABLE notifications ADD COLUMN action_url VARCHAR(500);

CREATE TABLE sponsors (
    id UUID PRIMARY KEY,
    name VARCHAR(160) NOT NULL,
    organization VARCHAR(180),
    description VARCHAR(1000),
    website_url VARCHAR(500),
    display_order INTEGER NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE content_policies (
    id UUID PRIMARY KEY,
    slug VARCHAR(100) NOT NULL UNIQUE,
    title VARCHAR(180) NOT NULL,
    content TEXT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    display_order INTEGER NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL
);

INSERT INTO sponsors (id, name, display_order, active, created_at, updated_at) VALUES
    (gen_random_uuid(), 'Monica', 1, TRUE, now(), now()),
    (gen_random_uuid(), 'Sosa', 2, TRUE, now(), now());

INSERT INTO content_policies (id, slug, title, content, active, display_order, updated_at) VALUES
    (gen_random_uuid(), 'account-access', 'Account access',
     'Registration details are reviewed before an account is activated. Keep login credentials private and use only your own account.', TRUE, 1, now()),
    (gen_random_uuid(), 'student-registration', 'Student registration',
     'Student records must be accurate and associated with the student''s active school. A school may review only students registered to that school.', TRUE, 2, now()),
    (gen_random_uuid(), 'exam-integrity', 'Exam integrity',
     'Competition responses must be submitted by the registered participant. Do not share exam access codes or attempt to interfere with another participant''s session.', TRUE, 3, now());

CREATE TABLE exam_district_releases (
    id UUID PRIMARY KEY,
    exam_id UUID NOT NULL REFERENCES exams(id),
    district_id UUID NOT NULL REFERENCES districts(id),
    released_by UUID REFERENCES user_accounts(id),
    released_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_exam_district_release UNIQUE (exam_id, district_id)
);
