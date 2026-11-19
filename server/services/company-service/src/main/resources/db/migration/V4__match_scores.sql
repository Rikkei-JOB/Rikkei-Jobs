CREATE TABLE match_scores (
    job_id      UUID                     NOT NULL,
    user_id     UUID                     NOT NULL,
    score       INTEGER                  NOT NULL,
    detail      TEXT                     NOT NULL,   -- JSON (xem dưới)
    stale       BOOLEAN                  NOT NULL DEFAULT FALSE,
    computed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_match_scores PRIMARY KEY (job_id, user_id),
    CONSTRAINT fk_match_scores_job FOREIGN KEY (job_id) REFERENCES jobs (id) ON DELETE CASCADE,
    CONSTRAINT ck_match_scores_score CHECK (score BETWEEN 0 AND 100)
);
