CREATE TABLE applications (
    id UUID PRIMARY KEY,
    job_id UUID NOT NULL,
    user_id UUID NOT NULL,
    company_id UUID NOT NULL,
    job_title VARCHAR(500) NOT NULL,
    company_name VARCHAR(500) NOT NULL,
    cv_id UUID NOT NULL,
    cv_snapshot_url VARCHAR(1000) NOT NULL,
    cover_letter VARCHAR(5000),
    status VARCHAR(20) NOT NULL CHECK (status IN ('SUBMITTED','SCREENING','INTERVIEW','OFFER','HIRED','REJECTED','WITHDRAWN')),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);
-- A rejected or withdrawn attempt never blocks a new attempt.
CREATE UNIQUE INDEX uq_applications_user_job_active ON applications(user_id, job_id)
    WHERE status NOT IN ('WITHDRAWN','REJECTED');
CREATE INDEX ix_applications_user_created ON applications(user_id, created_at DESC, id DESC);
CREATE INDEX ix_applications_user_status_created ON applications(user_id, status, created_at DESC, id DESC);

CREATE TABLE application_status_history (
    id UUID PRIMARY KEY,
    application_id UUID NOT NULL REFERENCES applications(id),
    from_status VARCHAR(20) CHECK (from_status IN ('SUBMITTED','SCREENING','INTERVIEW','OFFER','HIRED','REJECTED','WITHDRAWN')),
    to_status VARCHAR(20) NOT NULL CHECK (to_status IN ('SUBMITTED','SCREENING','INTERVIEW','OFFER','HIRED','REJECTED','WITHDRAWN')),
    actor_id UUID NOT NULL,
    note VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX ix_application_history_order ON application_status_history(application_id, created_at, id);

CREATE TABLE application_notification_outbox (
    id UUID PRIMARY KEY,
    application_id UUID NOT NULL REFERENCES applications(id),
    recipient VARCHAR(320) NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING','PROCESSING','SENT','FAILED')),
    attempts INTEGER NOT NULL DEFAULT 0 CHECK (attempts >= 0),
    next_attempt_at TIMESTAMPTZ NOT NULL,
    lease_until TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    sent_at TIMESTAMPTZ,
    CONSTRAINT uq_application_notification_recipient UNIQUE(application_id, recipient)
);
CREATE INDEX ix_application_outbox_due ON application_notification_outbox(status, next_attempt_at);
