-- HS-02: ngoại ngữ trong hồ sơ, trình độ theo khung CEFR. HS-04 đưa vào phần LANGUAGES của CV.
CREATE TABLE languages (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    name VARCHAR(100) NOT NULL,
    level VARCHAR(2) NOT NULL,
    note VARCHAR(255),
    CONSTRAINT ck_language_level CHECK (level IN ('A1', 'A2', 'B1', 'B2', 'C1', 'C2'))
);
CREATE INDEX idx_languages_user ON languages(user_id);
