-- NT-01: users, used_sso_tickets, password_reset_tokens
-- SQL kept portable (PostgreSQL in prod, H2 PostgreSQL-mode in tests).

CREATE TABLE users (
    id                   UUID                     NOT NULL PRIMARY KEY,
    email                VARCHAR(255)             NOT NULL,
    password_hash        VARCHAR(255),
    full_name            VARCHAR(255)             NOT NULL,
    role                 VARCHAR(20)              NOT NULL,
    lms_user_id          VARCHAR(100),
    company_id           UUID,
    must_change_password BOOLEAN                  NOT NULL DEFAULT FALSE,
    failed_login_count   INT                      NOT NULL DEFAULT 0,
    locked_until         TIMESTAMP WITH TIME ZONE,
    status               VARCHAR(10)              NOT NULL DEFAULT 'ACTIVE',
    created_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT uq_users_lms_user_id UNIQUE (lms_user_id),
    CONSTRAINT ck_users_email_lower CHECK (email = LOWER(email)),
    CONSTRAINT ck_users_role CHECK (role IN ('CANDIDATE', 'HR', 'MENTOR', 'STAFF', 'ADMIN')),
    CONSTRAINT ck_users_status CHECK (status IN ('ACTIVE', 'LOCKED')),
    CONSTRAINT ck_users_failed_count CHECK (failed_login_count >= 0)
);

CREATE INDEX idx_users_company_id ON users (company_id);

CREATE TABLE used_sso_tickets (
    ticket  VARCHAR(255)             NOT NULL PRIMARY KEY,
    used_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_used_sso_tickets_used_at ON used_sso_tickets (used_at);

CREATE TABLE password_reset_tokens (
    id         UUID                     NOT NULL PRIMARY KEY,
    user_id    UUID                     NOT NULL,
    token_hash VARCHAR(64)              NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    used_at    TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_prt_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uq_prt_token_hash UNIQUE (token_hash)
);

CREATE INDEX idx_prt_user_id ON password_reset_tokens (user_id);
