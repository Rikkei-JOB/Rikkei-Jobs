# Sơ đồ cơ sở dữ liệu (ERD)

Cập nhật file này trong cùng PR mỗi khi thêm hoặc đổi bảng. Nét liền là khoá ngoại thật; nét đứt là tham chiếu
giữa hai service (chỉ lưu id, **không có khoá ngoại vật lý**).

## Sơ đồ

```mermaid
erDiagram
    companies ||..o{ users : "company_id (HR)"
    users ||--o{ password_reset_tokens : "user_id"
    companies ||--o{ company_hr : "company_id"
    users ||..o| company_hr : "user_id (HR)"
    users ||..o{ skill_evidences : "user_id"
    users ||..o{ skill_scores : "user_id"
    users ||..o{ external_records : "user_id"
    users ||..o| skill_profile_states : "user_id"
    skills ||--o{ skill_mappings : "skill_id"
    skills ||--o{ skill_evidences : "skill_id"
    skills ||--o{ skill_scores : "skill_id"
    external_records ||--o{ skill_evidences : "external_record_id"
    external_records ||--o{ manual_overrides : "record_id"
    users ||..o{ sync_runs : "user_id"

    companies {
        UUID id PK
        VARCHAR name "NOT NULL"
        VARCHAR tax_code UK "10 hoặc 13 chữ số"
        VARCHAR industry
        VARCHAR size "1-50 | 51-200 | 201-1000 | 1000+"
        VARCHAR address
        VARCHAR city
        VARCHAR website
        VARCHAR logo_url
        TEXT description
        VARCHAR status "ACTIVE | LOCKED"
        UUID created_by "STAFF tạo"
        TIMESTAMPTZ created_at
        TIMESTAMPTZ updated_at
    }

    company_hr {
        UUID user_id PK "HR (identity users.id)"
        UUID company_id FK
        TIMESTAMPTZ created_at
        TIMESTAMPTZ updated_at
    }

    skills {
        UUID id PK
        VARCHAR code UK "JAVA, SQL..."
        VARCHAR name
        VARCHAR skill_group
    }

    skill_mappings {
        UUID id PK
        VARCHAR source "LMS | EXAM | PM"
        VARCHAR source_code "JAVA01..."
        UUID skill_id FK
        NUMERIC weight
    }

    external_records {
        UUID id PK
        UUID user_id
        VARCHAR source "LMS | EXAM | PM"
        VARCHAR source_code
        VARCHAR source_ref "UK cùng user_id, source"
        VARCHAR title
        NUMERIC score
        NUMERIC score_max
        TIMESTAMPTZ recorded_at
        BOOLEAN verified
        VARCHAR source_key "UK cùng source (NT-05)"
        JSONB payload "bản ghi gốc từ mock"
        TIMESTAMPTZ synced_at
    }

    manual_overrides {
        UUID id PK
        UUID record_id FK
        VARCHAR field "score | title"
        VARCHAR value
        VARCHAR reason "NOT NULL"
        UUID staff_id
        TIMESTAMPTZ created_at
    }

    sync_runs {
        UUID id PK
        VARCHAR trigger "NIGHTLY | ON_OPEN | MANUAL"
        UUID user_id "NULL khi job đêm lỗi sớm"
        VARCHAR status "SUCCESS | FAILED"
        TIMESTAMPTZ started_at
        TIMESTAMPTZ finished_at
        VARCHAR error
        JSONB counts
        UUID triggered_by
    }

    skill_evidences {
        UUID id PK
        UUID user_id
        UUID skill_id FK
        VARCHAR source "LMS | EXAM | INTERNSHIP | MENTOR | SELF"
        VARCHAR source_ref
        VARCHAR title
        NUMERIC score "0-100"
        NUMERIC weight
        TIMESTAMPTZ recorded_at
        BOOLEAN verified
        UUID external_record_id FK "NULL nếu không từ nguồn ngoài"
    }

    skill_scores {
        UUID user_id PK
        UUID skill_id PK
        NUMERIC verified_score "NULL nếu chỉ tự khai"
        NUMERIC self_score
        VARCHAR level "BASIC | FAIR | GOOD | EXCELLENT"
        TIMESTAMPTZ updated_at
    }

    skill_profile_states {
        UUID user_id PK
        BOOLEAN consented "ứng viên đồng ý công khai"
        BOOLEAN approved "cán bộ đã duyệt"
    }

    users {
        UUID id PK
        VARCHAR email UK "chữ thường"
        VARCHAR password_hash "NULL với user SSO"
        VARCHAR full_name "NOT NULL"
        VARCHAR role "CANDIDATE | HR | MENTOR | STAFF | ADMIN"
        VARCHAR lms_user_id UK "NULL với HR"
        UUID company_id "chỉ HR, trỏ companies.id"
        BOOLEAN must_change_password
        INT failed_login_count
        TIMESTAMPTZ locked_until
        VARCHAR status "ACTIVE | LOCKED"
        TIMESTAMPTZ created_at
    }

    password_reset_tokens {
        UUID id PK
        UUID user_id FK
        VARCHAR token_hash UK "chỉ lưu hash"
        TIMESTAMPTZ expires_at
        TIMESTAMPTZ used_at
    }

    used_sso_tickets {
        VARCHAR ticket PK
        TIMESTAMPTZ used_at
    }
```

## Bảng theo service

| Service | Schema | Bảng | Migration | Task |
| --- | --- | --- | --- | --- |
| identity-service | `public` | `users`, `used_sso_tickets`, `password_reset_tokens` | `V1__identity_init.sql` | NT-01 |
| company-service | `company` | `companies`, `company_hr` | `V1__company_init.sql` | DN-01 |
| profile-service | `profile` | `skills`, `skill_mappings`, `skill_evidences`, `skill_scores`, `external_records`, `skill_profile_states` | `V1__skill_profile.sql` → `V3__external_records_and_profile_state.sql` | HS-01 |
| profile-service | `profile` | `manual_overrides`, `sync_runs`; thêm `source_key`, `payload`, `synced_at` vào `external_records` | `V4__nt05_external_sync.sql` | NT-05 |

## Quy ước

- **Mỗi service một schema** trong database chung `rikkei_jobs`, cấu hình trong `application.yml` của service:

  ```yaml
  spring:
    jpa.properties.hibernate.default_schema: <service>
    flyway:
      schemas: <service>
      default-schema: <service>
  ```

  Bảng lịch sử Flyway nằm trong schema đó, nên các service migrate độc lập, khởi động theo thứ tự nào cũng được.
  `identity-service` đã merge trước quy ước này nên giữ schema `public`.
- **Tên migration** đánh số tăng dần, 3 chữ số: `V001__mo_ta.sql`, `V002__...`. Flyway so số theo giá trị, nên
  `V1__` của identity và company tương đương `V001`; migration tiếp theo của hai service này là `V002__`.
- **Không sửa migration đã merge.** Muốn đổi thì thêm migration mới.
- **Không đặt khoá ngoại giữa hai service.** Ví dụ `users.company_id` chỉ lưu id; tính đúng do seed và API tạo HR (DN-01) bảo đảm.
- **Seed** chỉ chạy với profile `dev` (`app.dev-data.enabled`), chạy lại không tạo trùng. ID cố định của dữ liệu mẫu:

  | Dữ liệu | ID |
  | --- | --- |
  | Công ty A (hr1, hr-new) | `00000000-0000-0000-0000-0000000000a1` |
  | Công ty B (hr2) | `00000000-0000-0000-0000-0000000000b2` |

## Kiểm tra từ DB trống

```bash
docker compose --env-file .env -f infra/compose.yaml up -d postgres
cd server
mvn install -DskipTests
mvn -f services/identity-service/pom.xml spring-boot:run "-Dspring-boot.run.profiles=dev"   # tạo users..., seed 9 tài khoản
mvn -f services/company-service/pom.xml spring-boot:run "-Dspring-boot.run.profiles=dev"    # tạo company.*, seed A/B + liên kết HR
```

Test tự động chạy cùng các migration trên H2 (`mvn test`), nên migration lỗi sẽ làm test đỏ.
