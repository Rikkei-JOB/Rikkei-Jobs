-- NT-02: minimal companies table so users.company_id (identity-service) has something to point to.
-- DN-01 adds the remaining columns in a new migration (never edit this file after merge).
-- No physical FK from users.company_id: the two tables belong to different services.

CREATE TABLE companies (
    id         UUID                     NOT NULL PRIMARY KEY,
    name       VARCHAR(255)             NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
