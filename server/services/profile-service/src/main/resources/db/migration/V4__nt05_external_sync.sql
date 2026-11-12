-- NT-05: đồng bộ dữ liệu học tập, thi, thực tập từ hệ thống ngoài (mock) về external_records của HS-01.

-- source_key: khoá ổn định của bản ghi bên nguồn, dạng "<lmsUserId>:<mã nguồn>" (ví dụ lms-sv-001:JAVA01).
-- Bản ghi cũ do HS-01 nạp tay không có source_key (NULL không vi phạm UNIQUE).
ALTER TABLE external_records ADD COLUMN source_key VARCHAR(255);
ALTER TABLE external_records ADD COLUMN payload JSONB;
ALTER TABLE external_records ADD COLUMN synced_at TIMESTAMP(6) WITH TIME ZONE;
ALTER TABLE external_records ADD CONSTRAINT uq_external_source_key UNIQUE (source, source_key);

-- Cán bộ sửa tay một trường. Giữ cả lịch sử; bản mới nhất của mỗi (record_id, field) là giá trị đang áp dụng.
CREATE TABLE manual_overrides (
    id UUID PRIMARY KEY,
    record_id UUID NOT NULL,
    field VARCHAR(30) NOT NULL,
    "value" VARCHAR(255) NOT NULL, -- trong nháy vì VALUE là từ khoá của H2 (test)
    reason VARCHAR(500) NOT NULL,
    staff_id UUID NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_override_record FOREIGN KEY (record_id) REFERENCES external_records(id) ON DELETE CASCADE,
    CONSTRAINT ck_override_field CHECK (field IN ('score', 'title')),
    CONSTRAINT ck_override_reason CHECK (LENGTH(TRIM(reason)) > 0)
);
CREATE INDEX idx_override_record ON manual_overrides(record_id, created_at);

-- Mỗi lần đồng bộ một ứng viên là một dòng. user_id NULL khi job đêm lỗi trước khi tới được ứng viên nào.
CREATE TABLE sync_runs (
    id UUID PRIMARY KEY,
    trigger VARCHAR(10) NOT NULL,
    user_id UUID,
    status VARCHAR(10) NOT NULL,
    started_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    finished_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    error VARCHAR(1000),
    counts JSONB,
    triggered_by UUID,
    CONSTRAINT ck_sync_trigger CHECK (trigger IN ('NIGHTLY', 'ON_OPEN', 'MANUAL')),
    CONSTRAINT ck_sync_status CHECK (status IN ('SUCCESS', 'FAILED')),
    CONSTRAINT ck_sync_error CHECK (status = 'SUCCESS' OR error IS NOT NULL)
);
CREATE INDEX idx_sync_runs_started ON sync_runs(started_at);
CREATE INDEX idx_sync_runs_user_started ON sync_runs(user_id, started_at);

-- Ánh xạ đợt thực tập (PM) sang kỹ năng. Mock không có mã kỹ năng cho đợt, nên dùng batchId làm source_code.
-- Dữ liệu mẫu, cần chủ HS-01 xác nhận trọng số.
INSERT INTO skill_mappings (id, source, source_code, skill_id, weight)
SELECT CAST(seed.id AS UUID), seed.source, seed.source_code, skill.id, seed.weight
FROM (VALUES
    ('00000000-0000-0000-0002-000000000101', 'PM', 'IOC-JB-2026-09', 'REST_API', 0.6),
    ('00000000-0000-0000-0002-000000000102', 'PM', 'IOC-JB-2026-09', 'SPRING_BOOT', 0.4),
    ('00000000-0000-0000-0002-000000000103', 'PM', 'IOC-JB-2026-12', 'REST_API', 0.6),
    ('00000000-0000-0000-0002-000000000104', 'PM', 'IOC-JB-2026-12', 'SPRING_BOOT', 0.4)
) AS seed(id, source, source_code, skill_code, weight)
JOIN skills skill ON skill.code = seed.skill_code
WHERE NOT EXISTS (SELECT 1 FROM skill_mappings existing
    WHERE existing.source = seed.source AND existing.source_code = seed.source_code
        AND existing.skill_id = skill.id);
