# DN-01 · Hồ sơ doanh nghiệp và tài khoản HR

| Mục | Giá trị |
| --- | --- |
| Mã task | DN-01 |
| Service | `company-service` (cổng 8085) qua `api-gateway`; gọi API nội bộ của `identity-service` (cổng 8081) |
| Frontend | trang `/hr/company` (`web/src/app/(pages)/(app)/hr/company`) |
| Phụ thuộc | NT-01 (JWT, bảng `users`, gửi email qua MailHog) |
| OpenAPI | [company-openapi.yaml](company-openapi.yaml), [identity-openapi.yaml](identity-openapi.yaml) (tag `internal`) |
| Request mẫu | [DN-01.http](DN-01.http) |

## 1. Mục tiêu

- **Cán bộ (`STAFF`)** tạo công ty và cấp tài khoản HR cho công ty đó. HR nhận email chứa mật khẩu tạm, đăng nhập lần đầu bị bắt đổi mật khẩu.
- **HR** xem, sửa hồ sơ và tải logo của **đúng công ty của mình**. Công ty được xác định từ `userId` trong JWT qua bảng `company_hr`; backend không đọc `companyId` hay `id` nào từ body hoặc URL.

## 2. Cơ sở dữ liệu

Schema Postgres riêng `company` (`COMPANY_DB_SCHEMA`), do Flyway tạo ở migration `V1__company_init.sql`. Bảng của identity (`users`...) vẫn ở schema mặc định; hai service không join chéo.

### `companies`

| Cột | Kiểu | Ghi chú |
| --- | --- | --- |
| `id` | UUID, PK | |
| `name` | VARCHAR(255), NOT NULL | |
| `tax_code` | VARCHAR(13), NOT NULL, UNIQUE | 10 hoặc 13 chữ số (CHECK độ dài; chữ số do `@Pattern` ở API kiểm) |
| `industry` | VARCHAR(100) | |
| `size` | VARCHAR(10) | CHECK `1-50`, `51-200`, `201-1000`, `1000+` |
| `address` / `city` / `website` | VARCHAR | `website` rỗng hoặc bắt đầu `http://`, `https://` |
| `logo_url` | VARCHAR(500) | đường dẫn tương đối `/companies/logos/<uuid>.<jpg|png>` |
| `description` | TEXT | |
| `status` | VARCHAR(10), NOT NULL, mặc định `ACTIVE` | CHECK `ACTIVE`, `LOCKED` |
| `created_by` | UUID, NOT NULL | `userId` của cán bộ tạo |
| `created_at`, `updated_at` | TIMESTAMPTZ | |

### `company_hr`

| Cột | Kiểu | Ghi chú |
| --- | --- | --- |
| `user_id` | UUID, PK | `users.id` bên identity (không có FK chéo schema). Mỗi HR thuộc đúng một công ty |
| `company_id` | UUID, NOT NULL, FK → `companies.id` | có index `idx_company_hr_company_id` |
| `created_at`, `updated_at` | TIMESTAMPTZ | |

## 3. API

Qua gateway `http://localhost:8080/api/v1` (gateway bỏ `/api/v1`, service map ở `/staff/companies/**` và `/companies/**`). Thành công trả thẳng DTO, lỗi là `{code, message, errors?}`. Đặc tả đầy đủ và mã lỗi từng endpoint: [company-openapi.yaml](company-openapi.yaml).

| Endpoint | Vai trò | Thành công | Lỗi chính |
| --- | --- | --- | --- |
| `POST /staff/companies` | `STAFF` | 201 `CompanyResponse` | 400 `VALIDATION_ERROR` (`errors.taxCode`...), 409 `CONFLICT` (trùng mã số thuế) |
| `POST /staff/companies/{id}/hr-accounts` | `STAFF` | 201 `HrAccountResponse` | 404 `NOT_FOUND`, 403 `COMPANY_LOCKED`, 409 `CONFLICT` (email đã dùng), 503 `EXTERNAL_UNAVAILABLE` |
| `GET /companies/me` | `HR` | 200 `CompanyResponse` | 404 `NOT_FOUND` (HR chưa gán công ty) |
| `PUT /companies/me` | `HR` | 200 `CompanyResponse` | 400 `VALIDATION_ERROR`, 403 `COMPANY_LOCKED` |
| `POST /companies/me/logo` (multipart, phần `file`) | `HR` | 200 `CompanyResponse` | 400 `FILE_TOO_LARGE` / `INVALID_FILE_TYPE`, 403 `COMPANY_LOCKED` |
| `GET /companies/logos/{fileName}` | công khai | 200 ảnh | 404 `NOT_FOUND` |

Mọi endpoint có token còn thêm: 401 `UNAUTHORIZED`, 403 `FORBIDDEN` (sai vai trò), 403 `MUST_CHANGE_PASSWORD`. Đường dẫn không tồn tại → 404 `NOT_FOUND`, sai phương thức → 405 `METHOD_NOT_ALLOWED`, sai `Content-Type` → 415 `UNSUPPORTED_MEDIA_TYPE`.

Quy tắc dữ liệu:

- Mã số thuế: `^(\d{10}|\d{13})$`, thông báo "Mã số thuế phải gồm 10 hoặc 13 chữ số". Duy nhất toàn hệ thống.
- Quy mô: `1-50`, `51-200`, `201-1000`, `1000+`. Chuỗi rỗng hoặc `null` ở các trường tuỳ chọn được lưu thành `null`.
- `PUT /companies/me` chỉ nhận `name` (bắt buộc), `industry`, `size`, `address`, `city`, `website`, `description`. Không có `id`, `taxCode`, `status`, `logoUrl`; trường lạ trong body bị bỏ qua.
- Logo: JPG (`FF D8 FF`) hoặc PNG (`89 50 4E 47 0D 0A 1A 0A`) kiểm theo **nội dung tệp**, tối đa 2 MB (`2 * 1024 * 1024` byte). Giới hạn multipart của Spring là 3 MB; vượt quá cũng trả 400 `FILE_TOO_LARGE`. Tệp lưu ở `COMPANY_UPLOAD_DIR` với tên `<uuid>.<jpg|png>` do server sinh; logo cũ bị xoá sau khi logo mới đã lưu.

### API nội bộ của identity-service (không qua gateway)

`POST /internal/users` và `DELETE /internal/users/{id}` trên `http://localhost:8081`, header `X-Internal-Key` = `INTERNAL_SERVICE_KEY` (mặc định `dev-internal-key`). Thiếu hoặc sai khoá → 401 `INVALID_SERVICE_KEY`. Gateway **không** định tuyến `/internal/**` nên trình duyệt không chạm tới được. Chi tiết trong tag `internal` của [identity-openapi.yaml](identity-openapi.yaml). Mã lỗi: 409 `EMAIL_EXISTS` (kèm `errors.userId/role/companyId`), 503 `MAIL_UNAVAILABLE` (không gửi được email, user không được tạo).

## 4. Luồng tạo tài khoản HR

```text
STAFF ──POST /staff/companies/{id}/hr-accounts──▶ company-service
   company-service: kiểm tra công ty tồn tại và không LOCKED
   company-service ──POST /internal/users (X-Internal-Key)──▶ identity-service
       identity: tạo user HR (mật khẩu tạm ngẫu nhiên, mustChangePassword=true), gửi email tới MailHog
       (gửi email lỗi → rollback, trả 503 MAIL_UNAVAILABLE, không để lại user)
   company-service: ghi company_hr(user_id, company_id)
       ghi lỗi → gọi DELETE /internal/users/{id} để hoàn tác
HR nhận email → đăng nhập → bị chuyển /change-password → đổi mật khẩu → vào /hr/company
```

- Mật khẩu tạm chỉ nằm trong email; không đi qua company-service, không xuất hiện trong response hay log.
- Email trùng: identity trả 409 `EMAIL_EXISTS`. Nếu user đó là HR của **đúng công ty này** nhưng chưa có liên kết (lần gọi trước bị timeout giữa chừng) thì company-service nối tiếp và trả 201; còn lại trả 409 `CONFLICT` "Email đã được sử dụng". Nhánh nối tiếp không bao giờ xoá user.
- Không gọi identity được hoặc identity lỗi → 503 `EXTERNAL_UNAVAILABLE`.

## 5. Cách chạy

Cần Docker (MailHog, Postgres), Java 21 + Maven, Node 24. Mỗi lệnh chạy từ gốc repo, mỗi tiến trình dài một terminal.

```bash
cp .env.example .env                                                  # nếu chưa có
docker compose --env-file .env -f infra/compose.yaml up -d postgres mailhog   # Postgres 5432, MailHog http://localhost:8025
(cd mock && npm ci && npm start)                                      # mock LMS :9000
# Cài parent POM + thư viện dùng chung vào ~/.m2 (chạy lại mỗi khi sửa common-core):
mvn -f server/pom.xml -pl services/identity-service,services/company-service,api-gateway -am install -DskipTests
mvn -f server/services/identity-service/pom.xml spring-boot:run "-Dspring-boot.run.profiles=dev"   # :8081, seed tài khoản mẫu
mvn -f server/services/company-service/pom.xml spring-boot:run "-Dspring-boot.run.profiles=dev"    # :8085, tạo schema company, seed Công ty A/B
mvn -f server/api-gateway/pom.xml spring-boot:run                                                  # :8080
(cd web && cp .env.example .env.local && npm ci && npm run dev -- --port 3000)   # http://localhost:3000
```

Lưu ý:

- Không dùng `-pl <service> -am spring-boot:run`: `-am` chạy `spring-boot:run` trên cả parent POM và thư viện dùng chung, Maven báo `Unable to find a suitable main class`. Vì vậy phải `install` trước rồi chạy riêng từng service.
- Trên PowerShell phải đặt `"-Dspring-boot.run.profiles=dev"` trong dấu nháy; nếu không, PowerShell tách tham số tại dấu chấm và Maven báo `Unknown lifecycle phase ".run.profiles=dev"`.
- Máy đã có PostgreSQL chạy sẵn ở cổng 5432 thì container `postgres` của Compose sẽ trùng cổng. Khi đó bỏ `postgres` khỏi lệnh `docker compose`, dùng Postgres có sẵn: tạo database `rikkei_jobs` (hoặc tên khác qua `POSTGRES_DB`) và đặt `$env:POSTGRES_USER` / `$env:POSTGRES_PASSWORD` (Bash: `export ...`) trong **mỗi** terminal chạy Java. Sai mật khẩu thì service dừng với `FATAL: password authentication failed for user "postgres"`.

- Chạy identity với profile `dev` **trước hoặc cùng lúc** company-service để các UUID cố định trùng nhau. Nếu DB dev đã seed bằng bản code cũ (user mẫu có id ngẫu nhiên), cần xoá các user `@rikkeijobs.test` rồi chạy lại identity để seed lại với id cố định.
- Muốn dùng DB riêng: `COMPANY_DB_URL`, `COMPANY_DB_USER`, `COMPANY_DB_PASSWORD` (xem `.env.example`). Hai service dùng chung một DB thì company-service vẫn chỉ ghi vào schema `company`.
- Java không tự đọc `.env`; các giá trị mặc định đã khớp Compose. Thư mục logo mặc định `./data/uploads` (tính từ thư mục chạy, đã được `.gitignore` bỏ qua).
- Test backend không cần Postgres: `mvn -f server/pom.xml -B clean test` (H2 `MODE=PostgreSQL`).

Đi các kịch bản bằng [DN-01.http](DN-01.http) (IntelliJ HTTP Client hoặc VS Code REST Client) và giao diện `/hr/company`.

## 6. Dữ liệu mẫu

Chỉ có khi chạy profile `dev` (`DEV_DATA_ENABLED=true`; mặc định tắt). Chạy lại không tạo trùng. UUID cố định, khai báo ở cả identity lẫn company-service (có comment trỏ sang nhau).

| Đối tượng | Giá trị |
| --- | --- |
| Công ty A | id `00000000-0000-0000-0000-0000000000a1`, mã số thuế `0101234567`, 51-200, Hà Nội |
| Công ty B | id `00000000-0000-0000-0000-0000000000b2`, mã số thuế `0109876543210`, 201-1000, Hồ Chí Minh |
| `hr1@rikkeijobs.test` | `…0001-000000000001`, Công ty A, mật khẩu `Test@123` |
| `hr-new@rikkeijobs.test` | `…0001-000000000003`, Công ty A, bắt đổi mật khẩu lần đầu |
| `hr2@rikkeijobs.test` | `…0001-000000000002`, Công ty B, mật khẩu `Test@123` |
| `staff1@rikkeijobs.test` | `…0001-000000000005`, STAFF, đăng nhập qua SSO với `lmsUserId` `lms-st-001` |

## 7. Ca kiểm thử và tiêu chí nghiệm thu

| # | Tiêu chí | Cách thử | Kết quả mong đợi | Test tự động |
| --- | --- | --- | --- | --- |
| 1 | Cán bộ tạo công ty và cấp HR; HR nhận mật khẩu tạm và bị buộc đổi | `DN-01.http` mục 1-2, MailHog, rồi đăng nhập HR mới | 201; email ở MailHog; đăng nhập xong bị chuyển `/change-password` | `StaffCompanyApiTest`, `HrAccountApiTest`, `InternalUserApiTest` |
| 2 | Tạo lại cùng email | `DN-01.http` 2c | 409 `CONFLICT` | `HrAccountApiTest` |
| 3 | HR chỉ sửa được công ty của mình | `DN-01.http` mục 3 và `/hr/company` | `PUT /companies/me` kèm id B chỉ đổi A; `PUT /companies/{B}` → 404; hr2 vẫn thấy B nguyên | `MyCompanyApiTest` |
| 4 | Logo ≤ 2 MB, JPG/PNG | `DN-01.http` mục 4 và giao diện | tệp 2 MB + 1 byte → 400 `FILE_TOO_LARGE`; giao diện chặn tệp 3 MB, không gửi request | `CompanyLogoApiTest` |
| 5 | Mã số thuế 10 hoặc 13 chữ số | `DN-01.http` 2d | `12345` → 400, `errors.taxCode` | `StaffCompanyApiTest` |
| 6 | Tài liệu API đầy đủ | mở [company-openapi.yaml](company-openapi.yaml) bằng Swagger Editor/Postman | mở được, có đủ mã lỗi từng endpoint | — |

Tên lớp test ở cột cuối là các lớp trong `server/services/company-service/src/test` và `server/services/identity-service/src/test`; chạy toàn bộ bằng `mvn -f server/pom.xml -B clean test`.

## 8. Ảnh màn hình

Thư mục [screenshots/DN-01](screenshots/DN-01) **chưa có ảnh**: máy bàn giao không dựng được Postgres (xem mục 9), nên không chụp được giao diện chạy thật và không có ảnh giả. Ảnh cần chụp khi chạy theo mục 5:

| Ảnh (đặt tên gợi ý) | Nội dung |
| --- | --- |
| `mailhog-temp-password.png` | MailHog nhận email mật khẩu tạm sau `DN-01.http` 2b |
| `hr-first-login-change-password.png` | HR mới đăng nhập, bị chuyển `/change-password` |
| `hr-company-profile.png` | `/hr/company` của hr1, hai cột (thông tin + biểu mẫu sửa) |
| `hr-company-logo-too-large.png` | Chọn logo 3 MB, giao diện báo lỗi và không gửi request |
| `tax-code-400.png` | Phản hồi 400 `errors.taxCode` (Postman/IDE) |

## 9. Trạng thái xác minh

Đã chạy trên nhánh `feature/DN-01-company-hr`:

- `mvn -f server/pom.xml -B clean test`: BUILD SUCCESS, không test nào lỗi (H2, không cần Postgres).
- `web/`: `npm run type-check` và `npm run build` thành công (route `/hr/company` có trong kết quả build). `npm run lint`: 1 lỗi và 1 cảnh báo **có sẵn**, không thuộc DN-01: lỗi `react-hooks/set-state-in-effect` ở `web/src/app/components/skills/skill-profile.tsx:101` (HS-01) và cảnh báo ở `postcss.config.mjs`.
- `company-openapi.yaml` và `identity-openapi.yaml`: `postman spec lint <file> --issueType=syntax` báo "No issues found".

**Chưa chạy** (không dựng được hạ tầng trên máy bàn giao: Docker không chạy và Postgres có sẵn ở cổng 5432 không nhận `postgres`/`postgres`): toàn luồng với Postgres, MailHog, gateway và trình duyệt, tức là sáu tiêu chí ở mục 7 theo `DN-01.http` và giao diện, cùng các kiểm tra tay của trang `/hr/company` (hai cột và một cột khi hẹp cửa sổ, `PUT` gửi `website: null` khi để trống, upload logo dạng multipart, chặn logo 3 MB không gọi API, quay lại trang trong 30 giây không gọi lại `GET`, đăng xuất rồi đăng nhập hr2 ngay trong cùng tab thấy Công ty B không nhấp nháy A, các trang NT-01 vẫn chạy). Các request trong `DN-01.http` được viết theo code của controller/DTO, **chưa được chạy** với dịch vụ thật; kiểu `404` khi gọi `/api/v1/internal/users` qua gateway (5b) cũng chưa chạy. Người nhận cần chạy các bước này theo mục 5 trước khi merge.

## 10. Checklist nộp PR

- [ ] Nhánh `feature/DN-01-company-hr`, PR **vào `test`**, tiêu đề `[DN-01] Doanh nghiệp: hồ sơ công ty, tài khoản HR`
- [x] Có: cách chạy, dữ liệu mẫu, OpenAPI (`company-openapi.yaml`, tag `internal` của `identity-openapi.yaml`), request mẫu
- [ ] Ảnh màn hình đủ trong `docs/api/screenshots/DN-01`
- [ ] Tự chạy `npm run lint` và `npm run build` ở `web/` (CI chỉ chạy `type-check`); lỗi lint sẵn có ở `skill-profile.tsx` thuộc HS-01, không thuộc DN-01
- [ ] Một bạn trong nhóm review trước, mentor duyệt PR
