-- Schema company do Flyway tạo và đặt làm mặc định (spring.flyway.schemas), nên không ghi tên schema ở đây.
-- Chỉ dùng cú pháp chạy được trên cả PostgreSQL lẫn H2 MODE=PostgreSQL.

CREATE TABLE companies (
    id          UUID                     NOT NULL,
    name        VARCHAR(255)             NOT NULL,
    tax_code    VARCHAR(13)              NOT NULL,
    industry    VARCHAR(100),
    size        VARCHAR(10),
    address     VARCHAR(255),
    city        VARCHAR(100),
    website     VARCHAR(255),
    logo_url    VARCHAR(500),
    description TEXT,
    status      VARCHAR(10)              NOT NULL DEFAULT 'ACTIVE',
    created_by  UUID                     NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_companies PRIMARY KEY (id),
    CONSTRAINT uq_companies_tax_code UNIQUE (tax_code),
    -- Phần "chỉ gồm chữ số" do @Pattern ở tầng API lo (cú pháp regex khác nhau giữa H2 và PostgreSQL).
    CONSTRAINT ck_companies_tax_code_length CHECK (LENGTH(tax_code) IN (10, 13)),
    CONSTRAINT ck_companies_size CHECK (size IN ('1-50', '51-200', '201-1000', '1000+')),
    CONSTRAINT ck_companies_status CHECK (status IN ('ACTIVE', 'LOCKED'))
);

CREATE TABLE company_hr (
    user_id    UUID                     NOT NULL,
    company_id UUID                     NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_company_hr PRIMARY KEY (user_id),
    CONSTRAINT fk_company_hr_company FOREIGN KEY (company_id) REFERENCES companies (id)
);

CREATE INDEX idx_company_hr_company_id ON company_hr (company_id);
