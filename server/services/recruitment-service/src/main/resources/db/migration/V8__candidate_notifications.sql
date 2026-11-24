CREATE TABLE recruitment.candidate_notifications (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    application_id UUID NOT NULL REFERENCES recruitment.applications(id) ON DELETE CASCADE,
    event_key VARCHAR(300) NOT NULL,
    type VARCHAR(40) NOT NULL,
    title VARCHAR(200) NOT NULL,
    body TEXT NOT NULL,
    href VARCHAR(250) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    read_at TIMESTAMPTZ,
    UNIQUE (user_id, event_key)
);
CREATE INDEX candidate_notifications_recipient_date ON recruitment.candidate_notifications(user_id, created_at DESC, id DESC);
CREATE INDEX candidate_notifications_unread ON recruitment.candidate_notifications(user_id) WHERE read_at IS NULL;
