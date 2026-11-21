CREATE TABLE donation_contributions (
    id UUID PRIMARY KEY,
    reference VARCHAR(64) NOT NULL UNIQUE,
    amount_ngn BIGINT NOT NULL CHECK (amount_ngn >= 50),
    donor_name VARCHAR(180) NOT NULL,
    email VARCHAR(254) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    paid_at TIMESTAMPTZ
);

CREATE INDEX idx_donation_contributions_created_at ON donation_contributions (created_at DESC);
