CREATE TABLE interview_rounds (
    id UUID PRIMARY KEY,
    application_id UUID NOT NULL REFERENCES applications(id),
    round_no INTEGER NOT NULL CHECK (round_no > 0),
    name VARCHAR(255) NOT NULL,
    interviewer_email VARCHAR(320) NOT NULL,
    scheduled_at TIMESTAMPTZ,
    result VARCHAR(10) NOT NULL CHECK (result IN ('PENDING', 'PASS', 'FAIL')),
    score NUMERIC(5, 2) CHECK (score BETWEEN 0 AND 100),
    note VARCHAR(2000),
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_interview_round_number UNIQUE (application_id, round_no)
);
CREATE INDEX ix_interview_rounds_application ON interview_rounds(application_id, round_no);

CREATE TABLE interview_round_revisions (
    id UUID PRIMARY KEY,
    round_id UUID NOT NULL REFERENCES interview_rounds(id),
    application_id UUID NOT NULL REFERENCES applications(id),
    actor_id UUID NOT NULL,
    old_result VARCHAR(10) NOT NULL CHECK (old_result IN ('PENDING', 'PASS', 'FAIL')),
    new_result VARCHAR(10) NOT NULL CHECK (new_result IN ('PENDING', 'PASS', 'FAIL')),
    old_score NUMERIC(5, 2),
    new_score NUMERIC(5, 2),
    old_note VARCHAR(2000),
    new_note VARCHAR(2000),
    created_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX ix_interview_round_revisions_application ON interview_round_revisions(application_id, created_at, id);
