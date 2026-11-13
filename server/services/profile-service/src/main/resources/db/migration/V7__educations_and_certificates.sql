CREATE TABLE educations (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    school VARCHAR(255) NOT NULL,
    major VARCHAR(255),
    from_date DATE NOT NULL,
    to_date DATE,
    gpa NUMERIC(10,4),
    note VARCHAR(5000),
    CONSTRAINT ck_education_dates CHECK (to_date IS NULL OR to_date >= from_date),
    CONSTRAINT ck_education_gpa CHECK (gpa IS NULL OR gpa BETWEEN 0 AND 10)
);
CREATE INDEX idx_educations_user ON educations(user_id);

CREATE TABLE certificates (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    issuer VARCHAR(255) NOT NULL,
    issued_at DATE NOT NULL,
    expires_at DATE,
    file_url VARCHAR(1000),
    file_type VARCHAR(32),
    CONSTRAINT ck_certificate_dates CHECK (expires_at IS NULL OR expires_at >= issued_at)
);
CREATE INDEX idx_certificates_user ON certificates(user_id);
