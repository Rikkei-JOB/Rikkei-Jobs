CREATE TABLE offers (
    id UUID PRIMARY KEY,
    application_id UUID NOT NULL UNIQUE REFERENCES applications(id),
    position VARCHAR(255) NOT NULL CHECK (length(trim(position)) > 0),
    salary BIGINT NOT NULL CHECK (salary > 0),
    start_date DATE NOT NULL,
    work_location VARCHAR(500) NOT NULL CHECK (length(trim(work_location)) > 0),
    note VARCHAR(2000),
    response_deadline TIMESTAMPTZ NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('SENT','ACCEPTED','DECLINED','EXPIRED')),
    responded_at TIMESTAMPTZ,
    decline_reason VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_offer_response CHECK (
        (status = 'SENT' AND responded_at IS NULL AND decline_reason IS NULL) OR
        (status = 'ACCEPTED' AND responded_at IS NOT NULL AND decline_reason IS NULL) OR
        (status = 'DECLINED' AND responded_at IS NOT NULL AND length(trim(decline_reason)) > 0) OR
        (status = 'EXPIRED' AND responded_at IS NULL AND decline_reason IS NULL)
    )
);
CREATE INDEX ix_offers_due ON offers(status, response_deadline);

CREATE TABLE employment_records (
    id UUID PRIMARY KEY,
    application_id UUID NOT NULL UNIQUE REFERENCES applications(id),
    user_id UUID NOT NULL,
    company_id UUID NOT NULL,
    job_id UUID NOT NULL,
    position VARCHAR(255) NOT NULL,
    start_date DATE NOT NULL,
    confirmed_at TIMESTAMPTZ
);
CREATE INDEX ix_employment_records_user ON employment_records(user_id, start_date DESC, id DESC);
