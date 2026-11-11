CREATE TABLE skill_profile_states (
    user_id UUID PRIMARY KEY,
    consented BOOLEAN NOT NULL DEFAULT FALSE,
    approved BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE external_records (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    source VARCHAR(20) NOT NULL,
    source_code VARCHAR(100) NOT NULL,
    source_ref VARCHAR(255) NOT NULL,
    title VARCHAR(255) NOT NULL,
    score NUMERIC(20,12) NOT NULL,
    score_max NUMERIC(20,12) NOT NULL,
    recorded_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    verified BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uq_external_record UNIQUE (user_id, source, source_ref),
    CONSTRAINT ck_external_source CHECK (source IN ('LMS','EXAM','PM')),
    CONSTRAINT ck_external_code CHECK (LENGTH(TRIM(source_code)) > 0),
    CONSTRAINT ck_external_ref CHECK (LENGTH(TRIM(source_ref)) > 0),
    CONSTRAINT ck_external_title CHECK (LENGTH(TRIM(title)) > 0),
    CONSTRAINT ck_external_score CHECK (score_max > 0 AND score >= 0 AND score <= score_max)
);

ALTER TABLE skill_evidences ADD COLUMN external_record_id UUID;
ALTER TABLE skill_evidences ADD CONSTRAINT fk_evidence_external
    FOREIGN KEY (external_record_id) REFERENCES external_records(id) ON DELETE CASCADE;
CREATE INDEX idx_evidence_external ON skill_evidences(external_record_id);
