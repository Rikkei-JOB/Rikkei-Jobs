CREATE TABLE interview_slots (
 id UUID PRIMARY KEY, round_id UUID NOT NULL REFERENCES interview_rounds(id), proposal_id UUID NOT NULL,
 start_at TIMESTAMPTZ NOT NULL, end_at TIMESTAMPTZ NOT NULL,
 status VARCHAR(16) NOT NULL CHECK(status IN ('OPEN','PICKED','CANCELLED')), created_at TIMESTAMPTZ NOT NULL,
 CHECK(end_at >= start_at + INTERVAL '30 minutes'), UNIQUE(id,round_id)
);
CREATE INDEX ix_interview_slots_round ON interview_slots(round_id,status);
CREATE INDEX ix_interview_slots_time ON interview_slots(start_at,end_at);
CREATE TABLE interview_schedules (
 id UUID PRIMARY KEY, round_id UUID NOT NULL REFERENCES interview_rounds(id), slot_id UUID NOT NULL UNIQUE,
 meet_url VARCHAR(2000) NOT NULL, status VARCHAR(16) NOT NULL CHECK(status IN ('SCHEDULED','DONE','CANCELLED')),
 candidate_id UUID NOT NULL, candidate_email VARCHAR(320) NOT NULL, interviewer_email VARCHAR(320) NOT NULL,
 reminded_24h_at TIMESTAMPTZ, reminded_1h_at TIMESTAMPTZ, reminder_1h_suppressed BOOLEAN NOT NULL DEFAULT FALSE,
 created_at TIMESTAMPTZ NOT NULL, cancelled_at TIMESTAMPTZ,
 FOREIGN KEY(slot_id,round_id) REFERENCES interview_slots(id,round_id)
);
CREATE UNIQUE INDEX uq_interview_schedule_active ON interview_schedules(round_id) WHERE status='SCHEDULED';
CREATE INDEX ix_interview_schedule_candidate ON interview_schedules(candidate_id,status);
CREATE INDEX ix_interview_schedule_interviewer ON interview_schedules(interviewer_email,status);
CREATE TABLE interview_notification_outbox (
 id UUID PRIMARY KEY, event_key VARCHAR(100) NOT NULL, type VARCHAR(24) NOT NULL,
 round_id UUID NOT NULL REFERENCES interview_rounds(id), schedule_id UUID REFERENCES interview_schedules(id),
 recipient VARCHAR(320) NOT NULL, payload TEXT NOT NULL,
 status VARCHAR(16) NOT NULL CHECK(status IN ('PENDING','PROCESSING','SENT','FAILED','CANCELLED')),
 attempts INTEGER NOT NULL DEFAULT 0, next_attempt_at TIMESTAMPTZ NOT NULL,
 lease_until TIMESTAMPTZ, created_at TIMESTAMPTZ NOT NULL, sent_at TIMESTAMPTZ,
 UNIQUE(event_key,type,recipient)
);
CREATE INDEX ix_interview_notification_due ON interview_notification_outbox(status,next_attempt_at);
