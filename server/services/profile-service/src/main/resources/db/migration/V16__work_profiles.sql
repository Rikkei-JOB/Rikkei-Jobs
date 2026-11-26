-- Hồ sơ nghề nghiệp dùng chung cho Mentor, Staff, Admin. Họ tên, email lấy từ identity (đồng bộ LMS) nên không lưu ở đây.
CREATE TABLE work_profiles (
    user_id UUID PRIMARY KEY,
    title VARCHAR(255),
    organization VARCHAR(255),
    years_experience INTEGER,
    phone VARCHAR(10),
    linkedin_url VARCHAR(500),
    expertise VARCHAR(1000),
    bio VARCHAR(3000),
    updated_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_work_profile_years CHECK (years_experience IS NULL OR years_experience BETWEEN 0 AND 60)
);
