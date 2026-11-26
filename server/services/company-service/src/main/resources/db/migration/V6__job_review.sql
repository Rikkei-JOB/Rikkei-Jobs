-- Duyệt JD và quản lý doanh nghiệp (DN-07). Chỉ dùng cú pháp chạy được trên cả PostgreSQL lẫn H2.

-- Lịch sử thao tác của cán bộ trên công ty; chỉ thêm, không sửa, không xoá.
-- target_id không có khoá ngoại để giữ lịch sử kể cả khi JD bị xoá về sau.
CREATE TABLE company_actions (
    id         UUID                     NOT NULL,
    company_id UUID                     NOT NULL,
    action     VARCHAR(12)              NOT NULL,
    target_id  UUID,
    reason     VARCHAR(1000),
    staff_id   UUID                     NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_company_actions PRIMARY KEY (id),
    CONSTRAINT fk_company_actions_company FOREIGN KEY (company_id) REFERENCES companies (id),
    CONSTRAINT ck_company_actions_action CHECK (action IN ('LOCK', 'UNLOCK', 'APPROVE_JD', 'REJECT_JD', 'CLOSE_JD'))
);

CREATE INDEX idx_company_actions_company_created ON company_actions (company_id, created_at);

CREATE INDEX idx_jobs_status_deadline ON jobs (status, deadline);

-- JD đã xử lý từ trước chưa có reviewed_at; reviewed_by để trống (không bịa người duyệt).
UPDATE jobs SET reviewed_at = updated_at WHERE status IN ('APPROVED', 'REJECTED', 'CLOSED') AND reviewed_at IS NULL;
