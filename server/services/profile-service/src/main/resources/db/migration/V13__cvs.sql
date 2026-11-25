-- HS-04: CV dựng từ hồ sơ. CV chỉ lưu cấu hình (phần nào, mục nào); nội dung đọc từ hồ sơ lúc xuất.
CREATE TABLE cvs (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    target_position VARCHAR(255),
    template VARCHAR(32) NOT NULL,
    public_token VARCHAR(64) NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_cvs_public_token UNIQUE (public_token),
    CONSTRAINT ck_cvs_template CHECK (template IN ('RIKKEI'))
);
CREATE INDEX idx_cvs_user ON cvs(user_id, created_at);

CREATE TABLE cv_sections (
    cv_id UUID NOT NULL REFERENCES cvs(id) ON DELETE CASCADE,
    section VARCHAR(16) NOT NULL,
    -- Mảng JSON id mục được chọn; NULL nghĩa là lấy mọi mục, kể cả mục thêm vào hồ sơ sau này.
    item_ids VARCHAR(8000),
    sort_order INTEGER NOT NULL,
    included BOOLEAN NOT NULL,
    PRIMARY KEY (cv_id, section),
    CONSTRAINT ck_cv_sections_section CHECK (section IN
        ('SUMMARY', 'SKILLS', 'EDUCATION', 'PROJECTS', 'CERTIFICATES', 'INTERNSHIP', 'LANGUAGES'))
);
