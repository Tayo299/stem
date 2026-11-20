CREATE TABLE sponsor_contributions (
    id UUID PRIMARY KEY,
    reference VARCHAR(64) NOT NULL UNIQUE,
    package_code VARCHAR(20) NOT NULL,
    package_name VARCHAR(80) NOT NULL,
    amount_ngn BIGINT NOT NULL CHECK (amount_ngn >= 10000000),
    organization_name VARCHAR(180) NOT NULL,
    contact_name VARCHAR(180) NOT NULL,
    email VARCHAR(254) NOT NULL,
    phone VARCHAR(40) NOT NULL,
    website VARCHAR(255),
    message VARCHAR(1200),
    privacy_consent BOOLEAN NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    paid_at TIMESTAMPTZ
);

CREATE INDEX idx_sponsor_contributions_created_at ON sponsor_contributions (created_at DESC);
