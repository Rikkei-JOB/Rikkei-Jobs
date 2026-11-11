# Sơ đồ cơ sở dữ liệu (ERD)

Cập nhật file này trong cùng PR mỗi khi thêm hoặc đổi bảng. Nét liền là khoá ngoại thật; nét đứt là tham chiếu
giữa hai service (chỉ lưu id, **không có khoá ngoại vật lý**).

## Sơ đồ

```mermaid
erDiagram
    companies ||..o{ users : "company_id (HR)"
    users ||--o{ password_reset_tokens : "user_id"

    companies {
        UUID id PK
        VARCHAR name "NOT NULL"
        TIMESTAMPTZ created_at "NOT NULL"
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
| company-service | `company` | `companies` (bản tối thiểu: `id`, `name`) | `V001__create_companies.sql` | NT-02; DN-01 thêm cột |

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
  `V1__identity_init.sql` của identity tương đương `V001`; migration tiếp theo của identity là `V002__`.
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
mvn -pl services/identity-service -am spring-boot:run -Dspring-boot.run.profiles=dev   # tạo users..., seed 9 tài khoản
mvn -pl services/company-service -am spring-boot:run -Dspring-boot.run.profiles=dev    # tạo company.companies, seed A/B
```

Test tự động chạy cùng các migration trên H2 (`mvn test`), nên migration lỗi sẽ làm test đỏ.
