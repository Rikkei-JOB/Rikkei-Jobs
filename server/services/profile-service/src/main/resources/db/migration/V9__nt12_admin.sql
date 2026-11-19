-- NT-12: ADMIN cấu hình danh mục kỹ năng, trọng số, mẫu phiếu; nhật ký sửa dữ liệu; ứng viên báo dữ liệu sai.
-- score_weights (DG-03) và evaluation_templates/evaluation_criteria (DG-01) được tạo ở đây trước;
-- DG-01/DG-03 mở rộng bằng migration ALTER TABLE mới, không tạo lại. Service khác đọc qua API, không đọc thẳng bảng.

-- Kỹ năng đã có bằng chứng không xoá cứng, chỉ tắt; kỹ năng tắt vẫn giữ nguyên bằng chứng và điểm cũ.
ALTER TABLE skills ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE;

-- Trọng số từng nguồn khi gộp điểm (DG-03). Tổng phải bằng 100, kiểm ở API vì CHECK không so được nhiều dòng.
CREATE TABLE score_weights (
    source     VARCHAR(20)              NOT NULL PRIMARY KEY,
    weight     NUMERIC(5, 2)            NOT NULL,
    updated_by UUID,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_score_weights_source CHECK (source IN ('LMS', 'EXAM', 'INTERNSHIP', 'MENTOR')),
    CONSTRAINT ck_score_weights_weight CHECK (weight >= 0 AND weight <= 100)
);

INSERT INTO score_weights (source, weight) VALUES
    ('LMS', 30), ('EXAM', 30), ('INTERNSHIP', 25), ('MENTOR', 15);

-- Mẫu phiếu đánh giá (DG-01). Phiếu đã chấm nên lưu bản chụp tiêu chí, không phụ thuộc mẫu đang sửa.
CREATE TABLE evaluation_templates (
    id          UUID                     NOT NULL PRIMARY KEY,
    name        VARCHAR(255)             NOT NULL,
    description VARCHAR(2000),
    active      BOOLEAN                  NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_evaluation_templates_name UNIQUE (name),
    CONSTRAINT ck_evaluation_templates_name CHECK (LENGTH(TRIM(name)) > 0)
);

CREATE TABLE evaluation_criteria (
    id          UUID          NOT NULL PRIMARY KEY,
    template_id UUID          NOT NULL REFERENCES evaluation_templates (id) ON DELETE CASCADE,
    name        VARCHAR(255)  NOT NULL,
    description VARCHAR(2000),
    weight      NUMERIC(5, 2) NOT NULL,
    max_score   INT           NOT NULL DEFAULT 10,
    sort_order  INT           NOT NULL,
    CONSTRAINT ck_evaluation_criteria_name CHECK (LENGTH(TRIM(name)) > 0),
    CONSTRAINT ck_evaluation_criteria_weight CHECK (weight > 0 AND weight <= 100),
    CONSTRAINT ck_evaluation_criteria_max CHECK (max_score BETWEEN 1 AND 100)
);

CREATE INDEX idx_evaluation_criteria_template ON evaluation_criteria (template_id);

INSERT INTO evaluation_templates (id, name, description) VALUES
    ('00000000-0000-0000-0012-000000000001', 'Đánh giá thực tập cuối kỳ',
     'Mẫu mặc định cho mentor đánh giá thực tập sinh khi kết thúc đợt.');

INSERT INTO evaluation_criteria (id, template_id, name, description, weight, max_score, sort_order) VALUES
    ('00000000-0000-0000-0012-000000000101', '00000000-0000-0000-0012-000000000001',
     'Kỹ năng chuyên môn', 'Chất lượng code, hiểu bài toán, hoàn thành task.', 40, 10, 1),
    ('00000000-0000-0000-0012-000000000102', '00000000-0000-0000-0012-000000000001',
     'Thái độ và kỷ luật', 'Đúng giờ, chủ động, tuân thủ quy trình.', 30, 10, 2),
    ('00000000-0000-0000-0012-000000000103', '00000000-0000-0000-0012-000000000001',
     'Làm việc nhóm và giao tiếp', 'Phối hợp với nhóm, báo cáo tiến độ.', 30, 10, 3);

-- Nhật ký mọi lần ADMIN sửa dữ liệu. entity_id là text để ghi được cả khoá không phải UUID (ví dụ nguồn trọng số).
CREATE TABLE change_logs (
    id         UUID                     NOT NULL PRIMARY KEY,
    entity     VARCHAR(40)              NOT NULL,
    entity_id  VARCHAR(100)             NOT NULL,
    field      VARCHAR(100)             NOT NULL,
    old_value  VARCHAR(5000),
    new_value  VARCHAR(5000),
    reason     VARCHAR(1000),
    admin_id   UUID                     NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_change_logs_entity CHECK (entity IN ('SKILL', 'SCORE_WEIGHT', 'EVALUATION_TEMPLATE',
        'PROFILE', 'EVIDENCE', 'DATA_REPORT'))
);

CREATE INDEX idx_change_logs_entity_created ON change_logs (entity, created_at);
CREATE INDEX idx_change_logs_created ON change_logs (created_at);

-- Ứng viên báo dữ liệu sai. evidence_title giữ lại tên bằng chứng lúc báo, phòng khi bằng chứng bị xoá do đồng bộ.
CREATE TABLE data_reports (
    id             UUID                     NOT NULL PRIMARY KEY,
    user_id        UUID                     NOT NULL,
    evidence_id    UUID REFERENCES skill_evidences (id) ON DELETE SET NULL,
    evidence_title VARCHAR(255)             NOT NULL,
    skill_name     VARCHAR(255)             NOT NULL,
    content        VARCHAR(2000)            NOT NULL,
    status         VARCHAR(10)              NOT NULL DEFAULT 'OPEN',
    resolution     VARCHAR(2000),
    resolved_by    UUID,
    resolved_at    TIMESTAMP WITH TIME ZONE,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_data_reports_status CHECK (status IN ('OPEN', 'RESOLVED', 'REJECTED')),
    CONSTRAINT ck_data_reports_content CHECK (LENGTH(TRIM(content)) > 0),
    CONSTRAINT ck_data_reports_resolved CHECK (
        (status = 'OPEN' AND resolved_at IS NULL)
        OR (status <> 'OPEN' AND resolved_at IS NOT NULL AND resolved_by IS NOT NULL AND resolution IS NOT NULL))
);

CREATE INDEX idx_data_reports_status_created ON data_reports (status, created_at);
CREATE INDEX idx_data_reports_user ON data_reports (user_id, created_at);
CREATE INDEX idx_data_reports_evidence ON data_reports (evidence_id);
