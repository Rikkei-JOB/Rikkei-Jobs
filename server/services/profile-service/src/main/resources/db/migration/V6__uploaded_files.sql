CREATE TABLE uploaded_files (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    content_type VARCHAR(32) NOT NULL,
    size_bytes BIGINT NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_profile_file_type CHECK (content_type IN ('application/pdf', 'image/jpeg', 'image/png')),
    CONSTRAINT ck_profile_file_size CHECK (size_bytes > 0 AND size_bytes <= 5242880)
);
CREATE INDEX idx_profile_files_user ON uploaded_files(user_id);
