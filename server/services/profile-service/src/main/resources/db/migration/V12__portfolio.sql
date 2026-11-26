-- HS-03: portfolio dự án. Dự án IOC do TT-01 tạo (chưa có TT-01 thì seed dev), ứng viên tự thêm dự án PERSONAL.
CREATE TABLE portfolio_projects (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    source VARCHAR(16) NOT NULL,
    internship_id UUID,
    name VARCHAR(255) NOT NULL,
    role VARCHAR(255),
    description VARCHAR(5000),
    tech_stack VARCHAR(1100),
    repo_url VARCHAR(1000),
    demo_url VARCHAR(1000),
    start_date DATE,
    end_date DATE,
    visible BOOLEAN NOT NULL,
    sort_order INTEGER NOT NULL,
    CONSTRAINT ck_portfolio_source CHECK (source IN ('IOC', 'PERSONAL')),
    CONSTRAINT ck_portfolio_internship CHECK ((source = 'IOC' AND internship_id IS NOT NULL) OR (source = 'PERSONAL' AND internship_id IS NULL)),
    CONSTRAINT ck_portfolio_dates CHECK (start_date IS NULL OR end_date IS NULL OR end_date >= start_date),
    -- TT-01 ghi lại cùng một kỳ thực tập phải cập nhật, không tạo bản trùng.
    CONSTRAINT uq_portfolio_internship UNIQUE (user_id, internship_id)
);
CREATE INDEX idx_portfolio_projects_user ON portfolio_projects(user_id, sort_order);

CREATE TABLE portfolio_media (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES portfolio_projects(id) ON DELETE CASCADE,
    type VARCHAR(16) NOT NULL,
    url VARCHAR(1000) NOT NULL,
    caption VARCHAR(255),
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_portfolio_media_type CHECK (type IN ('IMAGE', 'EVIDENCE'))
);
CREATE INDEX idx_portfolio_media_project ON portfolio_media(project_id);
