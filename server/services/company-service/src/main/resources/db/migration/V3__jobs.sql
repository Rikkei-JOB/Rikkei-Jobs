-- JD (nhu cầu tuyển dụng) và kỹ năng yêu cầu. Chỉ dùng cú pháp chạy được trên cả PostgreSQL lẫn H2.

CREATE TABLE jobs (
    id            UUID                     NOT NULL,
    company_id    UUID                     NOT NULL,
    title         VARCHAR(255)             NOT NULL,
    position      VARCHAR(255),
    level         VARCHAR(10),
    type          VARCHAR(12),
    quantity      INTEGER,
    city          VARCHAR(100),
    address       VARCHAR(255),
    work_mode     VARCHAR(10),
    salary_min    BIGINT,
    salary_max    BIGINT,
    description   TEXT,
    requirements  TEXT,
    benefits      TEXT,
    deadline      DATE,
    status        VARCHAR(10)              NOT NULL DEFAULT 'DRAFT',
    reviewed_by   UUID,
    reviewed_at   TIMESTAMP WITH TIME ZONE,
    reject_reason VARCHAR(1000),
    created_by    UUID                     NOT NULL,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version       BIGINT                   NOT NULL DEFAULT 0,
    CONSTRAINT pk_jobs PRIMARY KEY (id),
    CONSTRAINT fk_jobs_company FOREIGN KEY (company_id) REFERENCES companies (id),
    CONSTRAINT ck_jobs_level CHECK (level IN ('INTERN', 'FRESHER', 'JUNIOR', 'MIDDLE', 'SENIOR')),
    CONSTRAINT ck_jobs_type CHECK (type IN ('FULLTIME', 'PARTTIME', 'INTERNSHIP')),
    CONSTRAINT ck_jobs_work_mode CHECK (work_mode IN ('ONSITE', 'REMOTE', 'HYBRID')),
    CONSTRAINT ck_jobs_status CHECK (status IN ('DRAFT', 'PENDING', 'APPROVED', 'REJECTED', 'CLOSED')),
    CONSTRAINT ck_jobs_quantity CHECK (quantity >= 1),
    CONSTRAINT ck_jobs_salary_min CHECK (salary_min >= 0),
    CONSTRAINT ck_jobs_salary_max CHECK (salary_max >= 0),
    CONSTRAINT ck_jobs_salary_range CHECK (salary_min IS NULL OR salary_max IS NULL OR salary_min <= salary_max)
);

CREATE INDEX idx_jobs_company_status_updated ON jobs (company_id, status, updated_at);

CREATE TABLE job_skills (
    job_id    UUID    NOT NULL,
    skill_id  UUID    NOT NULL,
    required  BOOLEAN NOT NULL,
    min_score INTEGER NOT NULL,
    CONSTRAINT pk_job_skills PRIMARY KEY (job_id, skill_id),
    CONSTRAINT fk_job_skills_job FOREIGN KEY (job_id) REFERENCES jobs (id) ON DELETE CASCADE,
    CONSTRAINT ck_job_skills_min_score CHECK (min_score BETWEEN 1 AND 100)
);
