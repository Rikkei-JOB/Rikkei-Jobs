-- HS-01 tables in Flyway's isolated profile schema; user IDs are identity UUIDs.
-- There is no cross-service FK so identity and profile can migrate independently.
CREATE TABLE IF NOT EXISTS skills (
    id UUID PRIMARY KEY,
    code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    skill_group VARCHAR(100) NOT NULL,
    CONSTRAINT ck_skill_code CHECK (LENGTH(TRIM(code)) > 0),
    CONSTRAINT ck_skill_name CHECK (LENGTH(TRIM(name)) > 0),
    CONSTRAINT ck_skill_group CHECK (LENGTH(TRIM(skill_group)) > 0)
);

CREATE TABLE IF NOT EXISTS skill_mappings (
    id UUID PRIMARY KEY,
    source VARCHAR(20) NOT NULL,
    source_code VARCHAR(100) NOT NULL,
    skill_id UUID NOT NULL REFERENCES skills(id),
    weight NUMERIC(20,12) NOT NULL,
    CONSTRAINT uq_skill_mapping UNIQUE (source, source_code, skill_id),
    CONSTRAINT ck_mapping_source CHECK (source IN ('LMS', 'EXAM', 'PM')),
    CONSTRAINT ck_mapping_code CHECK (LENGTH(TRIM(source_code)) > 0),
    CONSTRAINT ck_mapping_weight CHECK (weight > 0)
);

CREATE TABLE IF NOT EXISTS skill_evidences (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    skill_id UUID NOT NULL REFERENCES skills(id),
    source VARCHAR(20) NOT NULL,
    source_ref VARCHAR(255) NOT NULL,
    title VARCHAR(255) NOT NULL,
    score NUMERIC(20,12) NOT NULL,
    weight NUMERIC(20,12) NOT NULL,
    recorded_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    verified BOOLEAN NOT NULL,
    CONSTRAINT uq_skill_evidence UNIQUE (user_id, skill_id, source, source_ref),
    CONSTRAINT ck_evidence_source CHECK (source IN ('LMS', 'EXAM', 'INTERNSHIP', 'MENTOR', 'SELF')),
    CONSTRAINT ck_evidence_ref CHECK (LENGTH(TRIM(source_ref)) > 0),
    CONSTRAINT ck_evidence_title CHECK (LENGTH(TRIM(title)) > 0),
    CONSTRAINT ck_evidence_score CHECK (score BETWEEN 0 AND 100),
    CONSTRAINT ck_evidence_weight CHECK (weight > 0),
    CONSTRAINT ck_evidence_self CHECK (source <> 'SELF' OR verified = FALSE)
);

CREATE TABLE IF NOT EXISTS skill_scores (
    user_id UUID NOT NULL,
    skill_id UUID NOT NULL REFERENCES skills(id),
    verified_score NUMERIC(20,6),
    self_score NUMERIC(20,6),
    level VARCHAR(20),
    updated_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    PRIMARY KEY (user_id, skill_id),
    CONSTRAINT ck_verified_score CHECK (verified_score IS NULL OR verified_score BETWEEN 0 AND 100),
    CONSTRAINT ck_self_score CHECK (self_score IS NULL OR self_score BETWEEN 0 AND 100),
    CONSTRAINT ck_score_level CHECK (
        (verified_score IS NULL AND level IS NULL)
        OR (verified_score IS NOT NULL AND level IS NOT NULL
            AND level IN ('BASIC', 'FAIR', 'GOOD', 'EXCELLENT'))
    )
);
