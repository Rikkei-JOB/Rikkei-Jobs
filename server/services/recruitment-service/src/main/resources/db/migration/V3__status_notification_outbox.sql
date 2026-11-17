CREATE TABLE application_status_notification_outbox (
    id UUID PRIMARY KEY,
    status_history_id UUID NOT NULL UNIQUE REFERENCES application_status_history(id),
    application_id UUID NOT NULL REFERENCES applications(id),
    user_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'PROCESSING', 'SENT', 'FAILED')),
    attempts INTEGER NOT NULL DEFAULT 0 CHECK (attempts >= 0),
    next_attempt_at TIMESTAMPTZ NOT NULL,
    lease_until TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    sent_at TIMESTAMPTZ
);
CREATE INDEX ix_application_status_outbox_due
    ON application_status_notification_outbox(status, next_attempt_at, created_at);
