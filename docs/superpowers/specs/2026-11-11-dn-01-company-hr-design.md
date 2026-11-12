# DN-01 · Doanh nghiệp: hồ sơ công ty, tài khoản HR — Thiết kế

Task: **[Doanh nghiệp & JD · DN-01] Doanh nghiệp: hồ sơ công ty, tài khoản HR**. Nhánh `feature/DN-01-company-hr` từ `test`, PR vào `test`, tiêu đề bắt đầu bằng `[DN-01]`.

Ký hiệu: **(chốt)** là điều người giao task đã quyết; **(người thiết kế chọn)** là quyết định trong lúc thiết kế, người làm có thể nêu lại nếu thấy sai.

## 1. Mục đích

Cán bộ tuyển dụng (STAFF) đưa một doanh nghiệp lên hệ thống và cấp tài khoản cho HR của doanh nghiệp đó. HR nhận email có mật khẩu tạm, lần đầu đăng nhập bị bắt đổi mật khẩu (luồng NT-01 đã có), sau đó tự sửa hồ sơ công ty của mình và tải logo lên.

Màn hình của cán bộ và bản hoàn chỉnh của form HR làm ở **DN-05**. DN-01 chỉ gồm API cho cán bộ, API và form "Hồ sơ công ty" bản cơ bản cho HR.

### Tiêu chí nghiệm thu (chốt)

1. staff1 tạo công ty và HR → MailHog có email chứa mật khẩu tạm; HR đăng nhập bị bắt đổi mật khẩu.
2. Tạo HR với email đã có → 409.
3. hr1 sửa được công ty A; gọi API sửa công ty B → 404.
4. Logo > 2MB → bị từ chối.
5. Mã số thuế sai định dạng → báo lỗi.
6. PR vào `test` có cách chạy, dữ liệu mẫu, ảnh màn hình; OpenAPI có endpoint mới.

### Ngoài phạm vi (chốt)

- Chuyển HR sang công ty khác (`PUT /staff/hr-accounts/{userId}/company`, `PATCH /internal/users/{id}/company`).
- Rút ngắn access token, refresh token (phải thống nhất với người làm NT-01).
- Màn hình của cán bộ, danh sách/tìm kiếm công ty, khoá/mở khoá công ty qua API (DN-05).

Thiết kế không được chặn đường cho các việc trên: `company_hr` phải cập nhật được và `/companies/me` không phụ thuộc claim `companyId`.

## 2. Ranh giới service (chốt)

- **company-service** sở hữu `companies` và quan hệ HR–công ty (`company_hr`). Đây là **nguồn sự thật** để biết HR thuộc công ty nào.
- **identity-service** vẫn là nơi duy nhất tạo tài khoản: hash mật khẩu, chặn email trùng, `must_change_password`, gửi email. `users.company_id` (đã có từ NT-01) chỉ là **bản sao** để ký vào JWT và hiển thị.
- company-service **không** dùng claim `companyId` để phân quyền và không đọc DB của identity.
- Hai service nói chuyện qua API nội bộ `/internal/**` của identity. Gateway không định tuyến tới `/internal/**`. Mỗi lời gọi kèm khoá dịch vụ nội bộ đọc từ cấu hình.
- Mật khẩu tạm chỉ tồn tại trong identity, không bao giờ đi qua company-service.

## 3. Dữ liệu

### 3.1 company-service — migration `V1__company_init.sql`

Thêm `flyway-core`, `flyway-database-postgresql` vào `server/services/company-service/pom.xml` và `spring.flyway` vào `application.yml` (mẫu ở identity-service). SQL phải chạy được trên cả PostgreSQL lẫn H2 `MODE=PostgreSQL` như migration của identity.

**Mỗi service một schema riêng (chốt).** Mọi service vẫn dùng chung DB `rikkei_jobs`, nhưng company-service để bảng và lịch sử Flyway trong schema `company`:

- `spring.flyway.schemas: ${COMPANY_DB_SCHEMA:company}` (Flyway tự tạo schema, `create-schemas` mặc định bật). Bảng `flyway_schema_history` nằm trong schema đó, nên không đụng tới `V1__identity_init` của identity.
- `spring.jpa.properties.hibernate.default_schema` dùng cùng giá trị, để `ddl-auto: validate` và các query tìm đúng schema.
- Test H2 dùng cùng cấu hình; người làm kiểm tra H2 tạo được schema (nếu cần thì thêm `INIT=CREATE SCHEMA IF NOT EXISTS company` vào URL test).
- identity giữ nguyên schema `public`, không đổi gì. Ghi quy ước này vào `docs/architecture/service-conventions.md` (mục Cấu hình): service mới dùng Flyway thì đặt schema trùng tên khu, ví dụ `profile`, `recruitment`.

`companies` (chốt các cột; kiểu dữ liệu và độ dài do người thiết kế chọn):

| Cột | Kiểu | Ràng buộc |
| --- | --- | --- |
| `id` | UUID | PK |
| `name` | VARCHAR(255) | NOT NULL |
| `tax_code` | VARCHAR(13) | NOT NULL, UNIQUE, CHECK chỉ gồm 10 hoặc 13 chữ số |
| `industry` | VARCHAR(100) | NULL |
| `size` | VARCHAR(10) | NULL, CHECK IN (`1-50`, `51-200`, `201-1000`, `1000+`) |
| `address` | VARCHAR(255) | NULL |
| `city` | VARCHAR(100) | NULL |
| `website` | VARCHAR(255) | NULL |
| `logo_url` | VARCHAR(500) | NULL |
| `description` | TEXT | NULL |
| `status` | VARCHAR(10) | NOT NULL DEFAULT `ACTIVE`, CHECK IN (`ACTIVE`, `LOCKED`) |
| `created_by` | UUID | NOT NULL (user id của cán bộ, lấy từ JWT) |
| `created_at` | TIMESTAMP WITH TIME ZONE | NOT NULL DEFAULT CURRENT_TIMESTAMP |
| `updated_at` | TIMESTAMP WITH TIME ZONE | NOT NULL DEFAULT CURRENT_TIMESTAMP (người thiết kế chọn thêm) |

`company_hr` (chốt):

| Cột | Kiểu | Ràng buộc |
| --- | --- | --- |
| `user_id` | UUID | PK (mỗi HR thuộc đúng một công ty) |
| `company_id` | UUID | NOT NULL, FK → `companies(id)`, có index |
| `created_at`, `updated_at` | TIMESTAMP WITH TIME ZONE | NOT NULL DEFAULT CURRENT_TIMESTAMP |

Entity dùng `Instant` cho cột thời gian, giống identity.

### 3.2 identity-service

Không cần migration. `users.company_id`, `must_change_password`, `status` đã có trong `V1__identity_init.sql`.

### 3.3 Dữ liệu mẫu (người thiết kế chọn)

Seeder dev của company-service (`app.dev-data.enabled`, tắt mặc định, bật trong profile `dev`, chạy lại không tạo trùng, giống `DevDataSeeder` của identity):

| ID | Tên | Mã số thuế | `company_hr` |
| --- | --- | --- | --- |
| `00000000-0000-0000-0000-0000000000a1` | Công ty A | `0101234567` | hr1, hr-new |
| `00000000-0000-0000-0000-0000000000b2` | Công ty B | `0109876543210` | hr2 |

Hai ID công ty khớp `DevDataSeeder.COMPANY_A_ID/COMPANY_B_ID` của identity. `company_hr` cần user id của hr1/hr2/hr-new, nhưng identity sinh UUID ngẫu nhiên lúc seed. Vì vậy **identity seeder đổi sang dùng UUID cố định cho các tài khoản mẫu** (ví dụ hr1 `00000000-0000-0000-0001-000000000001`, hr2 `...-0001-000000000002`, hr-new `...-0001-000000000003`) và company seeder dùng lại các hằng đó. Chỉ áp dụng khi user chưa tồn tại. `User.id` đang dùng `@GeneratedValue(strategy = UUID)` nên id gán tay bị bỏ qua; người làm tự chọn cách cho seeder đặt được id cố định (ví dụ insert bằng SQL trong seeder, hoặc bỏ generator và tự sinh UUID trong code tạo user). Máy đã seed trước đó cần xoá DB dev hoặc chạy lại seed; ghi rõ trong hướng dẫn chạy.

## 4. API

Mọi lỗi theo `ErrorResponse {code, message, errors?}`, message tiếng Việt. Đường dẫn dưới đây là đường dẫn ở service; qua gateway thì thêm tiền tố `/api/v1`.

### 4.1 Gateway (người thiết kế chọn)

Mở rộng predicate của route `company` hiện có trong `server/api-gateway/src/main/resources/application.yml`, giống cách HS-01 thêm `/api/v1/profiles/**` vào route `profile`: `["Path=/api/v1/company/**,/api/v1/companies/**,/api/v1/staff/companies/**"]`, giữ `StripPrefix=2`:

- `/api/v1/staff/companies/**` → `/staff/companies/**`
- `/api/v1/companies/**` → `/companies/**`
- `/api/v1/company/**` (đã có, dùng cho `/company/ping`) giữ nguyên.

Không thêm route id mới nên `GatewayRoutesTest` không đổi. Thêm `CompanyRouteIntegrationTest` theo mẫu `ProfileRouteIntegrationTest`. Đây là ngoại lệ so với quy ước "controller map ở `/<khu>/**`" để giữ đúng đường dẫn trong đề; ghi chú ngoại lệ này vào `docs/architecture/service-conventions.md`.

### 4.2 Cán bộ (company-service)

`@PreAuthorize(Roles.STAFF)`, đúng theo đề (chỉ STAFF; ADMIN không được gọi). Token HR → 403 `FORBIDDEN`.

**`POST /staff/companies`** → `201` `CompanyResponse`

- Body: `name` (bắt buộc, ≤ 255), `taxCode` (bắt buộc, regex `^(\d{10}|\d{13})$`), `industry`, `size` (một trong 4 giá trị), `address`, `city`, `website` (nếu có thì phải là URL `http`/`https`), `description`.
- Không nhận `status`, `logoUrl`, `createdBy`: `status = ACTIVE`, `createdBy = principal.userId()`.
- Mã số thuế sai định dạng → 400 `VALIDATION_ERROR`, `errors.taxCode`. Trùng mã số thuế → 409 `CONFLICT` (kiểm tra trước khi lưu, ràng buộc UNIQUE là lưới an toàn).

**`POST /staff/companies/{id}/hr-accounts`** → `201` `{userId, email, fullName, companyId}`

- Body: `email` (bắt buộc, định dạng email, đưa về chữ thường), `fullName` (bắt buộc).
- Luồng:
  1. Không có công ty → 404. Công ty `LOCKED` → 403 `COMPANY_LOCKED`.
  2. Gọi identity `POST /internal/users` (mục 4.4).
  3. Identity trả 201 → ghi `company_hr(userId, companyId)` → trả 201.
  4. Ghi `company_hr` lỗi → gọi identity `DELETE /internal/users/{userId}` để xoá hẳn user vừa tạo (chốt; thay cho phương án "khoá user" ban đầu, vì `LOCKED` của NT-01 là khoá tạm và tự mở khi đăng nhập), rồi trả 500 `INTERNAL_ERROR`. Mật khẩu tạm trong email đã gửi trở thành vô dụng. Lời gọi xoá cũng lỗi thì ghi log ERROR kèm `userId`; lần thử lại của cán bộ sẽ được mục 4.5 nối user đó vào công ty.
  5. Identity trả 409 → xem mục 4.5.
  6. Identity tắt, timeout hoặc 5xx → 503 `EXTERNAL_UNAVAILABLE` (quy ước đổi lỗi trong mock-contract). Không để lộ lỗi gốc.

### 4.3 HR (company-service)

`@PreAuthorize(Roles.HR)`. Cách xác định công ty (chốt):

1. Lấy `sub` từ JWT đã kiểm chữ ký; role phải là HR (không phải thì 403).
2. Tra `company_hr` theo `user_id`. Không có → 404 `NOT_FOUND`.
3. Đọc `companies`. Không có → 404.
4. Không đọc claim `companyId` và không đọc bất kỳ id nào trong body.

**`GET /companies/me`** → `200` `CompanyResponse`. Trả cả khi công ty `LOCKED` (người thiết kế chọn), để form hiện được ở chế độ chỉ xem.

**`PUT /companies/me`** → `200` `CompanyResponse`

- Body: `name` (bắt buộc), `industry`, `size`, `address`, `city`, `website`, `description`. Đây là thay thế toàn bộ: trường không gửi hoặc gửi `null` sẽ bị xoá (trừ `name`).
- DTO không có `id`, `companyId`, `taxCode`, `status`, `logoUrl`. Jackson của Spring Boot mặc định bỏ qua field lạ, nên body có các field đó vẫn 200 và chúng không có tác dụng. Có test khẳng định điều này.
- Công ty `LOCKED` → 403 `COMPANY_LOCKED`.

**`POST /companies/me/logo`** → `200` `CompanyResponse` (có `logoUrl` mới)

- `multipart/form-data`, field `file`.
- Chấp nhận JPG và PNG, kiểm tra bằng byte đầu file (JPG `FF D8 FF`, PNG `89 50 4E 47 0D 0A 1A 0A`), không chỉ tin `Content-Type` hay đuôi file. Sai loại → 400 `INVALID_FILE_TYPE`.
- Lớn hơn 2MB (2 × 1024 × 1024 byte) → 400 `FILE_TOO_LARGE`. Cấu hình `spring.servlet.multipart.max-file-size` và `max-request-size` lớn hơn 2MB một chút (ví dụ 3MB) để chính service trả lỗi theo đúng dạng. Thêm handler cho `MaxUploadSizeExceededException` (file vượt cả giới hạn multipart) → cũng 400 `FILE_TOO_LARGE`, thay vì rơi xuống 500.
- Công ty `LOCKED` → 403 `COMPANY_LOCKED`.
- Lưu trữ (người thiết kế chọn): ghi file vào thư mục `app.upload.dir` (`COMPANY_UPLOAD_DIR`, mặc định `./data/uploads`), tên `<uuid>.jpg|png`. Ghi file mới, cập nhật `logo_url`, rồi xoá file cũ. `logo_url` lưu đường dẫn tương đối `/companies/logos/<tên file>`. `CompanyResponse.logoUrl` trả đúng đường dẫn đó; frontend tự ghép với `NEXT_PUBLIC_API_URL`.

**`GET /companies/logos/{fileName}`**: công khai, không cần token, vì thẻ `<img>` không gửi được `Authorization`. `fileName` phải khớp `^[0-9a-f-]{36}\.(jpg|png)$` (chống path traversal). Không có → 404. Trả đúng `Content-Type`.

### 4.4 Identity nội bộ (mới)

Đường dẫn `/internal/**`, `permitAll` ở JWT chain nhưng bắt buộc header `X-Internal-Key` khớp `app.internal.service-key` (`INTERNAL_SERVICE_KEY`, mặc định dev `dev-internal-key`). Kiểm tra bằng một filter riêng cho `/internal/**`, so sánh hằng thời gian. Thiếu hoặc sai → 401 `INVALID_SERVICE_KEY`. Tên header và tên biến do người thiết kế chọn.

**`POST /internal/users`** → `201` `{userId, email, fullName, role, companyId}`

- Body: `email`, `fullName`, `role` (DN-01 chỉ chấp nhận `HR`), `companyId` (bắt buộc khi role HR).
- Sinh mật khẩu tạm ngẫu nhiên bằng `SecureRandom`, 12 ký tự, luôn có chữ và số để khớp `PasswordPolicy`. Lưu hash BCrypt, `must_change_password = true`, `status = ACTIVE`, `company_id` = bản sao.
- Gửi email "Tài khoản HR của bạn" qua MailService (thêm method mới, ví dụ `sendHrAccountCreated(to, fullName, tempPassword, loginUrl)`). Nội dung có email đăng nhập, mật khẩu tạm, link `${WEB_BASE_URL}/login`, và lời nhắc phải đổi mật khẩu lần đầu.
- **Email gửi lỗi** (người thiết kế chọn): rollback việc tạo user và trả 503 `MAIL_UNAVAILABLE`, để cán bộ thử lại là sạch. Không bao giờ ghi mật khẩu tạm ra log. Khi `MAIL_ENABLED=false`, MailService không gửi và coi như thành công (test dùng mock bean để bắt nội dung).
- Email đã tồn tại → 409 `EMAIL_EXISTS` kèm `{userId, role, companyId}`. Các field thêm này chỉ có ở API nội bộ.

**`DELETE /internal/users/{id}`** → `204`. Chỉ xoá được user role `HR` (role khác → 409 `CONFLICT`), để API bù trừ không xoá nhầm tài khoản khác. `password_reset_tokens` tự xoá theo `ON DELETE CASCADE`. Không có user → 404; company-service coi 404 là đã xoá xong.

### 4.5 Email trùng và thử lại (người thiết kế chọn)

Nếu identity đã tạo user mà company-service không nhận được trả lời (timeout), lần thử lại sẽ gặp 409. Để cán bộ không bị kẹt, khi identity trả 409, company-service **nối user vào công ty thay vì báo lỗi** nếu đủ cả ba điều kiện:

- `role = HR`;
- `companyId` của user trong identity bằng đúng công ty đang thao tác;
- chưa có dòng `company_hr` nào cho `userId` đó.

Khi nối lại, không gửi lại email: HR dùng mật khẩu tạm của email trước, hoặc dùng "Quên mật khẩu". Không có bước mở khoá vì bù trừ là xoá chứ không khoá. Mọi trường hợp khác trả **409 `CONFLICT`**, message "Email đã được sử dụng". Ví dụ: email của hr1 (đã có `company_hr`), email của candidate, hay email HR của công ty khác. Đây là tiêu chí nghiệm thu 2.

### 4.6 Client gọi identity (company-service)

`client/IdentityClient` dùng `RestClient`. Base URL lấy từ `IDENTITY_SERVICE_URL` (mặc định `http://localhost:8081`, cùng tên biến gateway đang dùng), tự gắn `X-Internal-Key`, timeout kết nối và đọc 5 giây, **không tự thử lại** (lời gọi tạo user không idempotent; thử lại do người dùng bấm, được che chắn bởi mục 4.5).

### 4.7 Sửa trong common-core

`GlobalExceptionHandler` hiện ném lại các lỗi có sẵn của Spring MVC (`org.springframework.web.ErrorResponse`: 404, 405, 415...) cho xử lý mặc định. Request khi đó được chuyển sang `/error`, mà `/error` không được `permitAll` và filter JWT không chạy lại ở lượt chuyển này. Vì vậy `PUT /companies/{idB}` nhiều khả năng trả **401 hoặc body lỗi mặc định của Spring**, không phải 404 `{code, message}`, và tiêu chí 3 không đạt. Người làm viết test kiểm hành vi thật trước khi sửa. Thêm handler trả `ErrorResponse` trực tiếp:

- `NoResourceFoundException` → 404 `NOT_FOUND`;
- `HttpRequestMethodNotSupportedException` → 405 `METHOD_NOT_ALLOWED`;
- `HttpMediaTypeNotSupportedException` → 415 `UNSUPPORTED_MEDIA_TYPE`;
- `MissingServletRequestPartException` → 400 `VALIDATION_ERROR`.

Thêm test trong common-core hoặc ping test của một service. Cập nhật bảng lỗi ở `docs/architecture/service-conventions.md`.

### 4.8 Bảo mật của company-service

Chain mặc định của common-core chỉ mở `/actuator/**`. company-service khai báo `SecurityFilterChain` riêng (như identity), giống chain mặc định và thêm `permitAll` cho `GET /companies/logos/**`. Vẫn dùng `JwtAuthenticationFilter`, entry point và access-denied handler của common-core.

## 5. Frontend

Trang **`/hr/company`** (file `web/src/app/(pages)/(app)/hr/company/page.tsx`; segment tĩnh `hr` được ưu tiên hơn `[role]`), chỉ HR vào được (dùng `use-auth-guard`). Trang chủ HR (`/hr`) có link "Hồ sơ công ty".

Bố cục **B, hai cột** (chốt, đã xem mockup):

- **Cột trái, thẻ cố định:** logo (hoặc ô trống), nút "Đổi logo", dòng "JPG hoặc PNG, tối đa 2MB", mã số thuế, trạng thái (Đang hoạt động / Đã khoá), dòng "Liên hệ cán bộ tuyển dụng để đổi các thông tin này".
- **Cột phải, form:** Tên công ty *, Ngành, Quy mô (select 4 giá trị), Địa chỉ, Thành phố, Website, Giới thiệu (textarea), nút "Lưu thay đổi".
- Màn hẹp xếp thành một cột, thẻ cố định ở trên.

Hành vi (người thiết kế chọn):

- Chọn file logo là tải lên ngay (`POST /companies/me/logo`), không đợi nút Lưu. File > 2MB hoặc không phải JPG/PNG bị chặn ở trình duyệt, kèm thông báo lỗi dưới nút. Backend vẫn kiểm tra lại; lỗi từ backend cũng hiện ở đó.
- Lưu thành công → thông báo "Đã lưu". Lỗi `VALIDATION_ERROR` → hiện theo từng trường từ `errors`.
- Công ty `LOCKED` → banner ở đầu trang, form và nút đổi logo bị vô hiệu.
- Dùng react-hook-form + zod (`src/schemas/company.schema.ts`, cùng luật với backend), TanStack Query, `src/services/company.service.ts` qua `api` sẵn có, kiểu trong `src/interface/company.ts`, component UI sẵn có trong `src/app/components/ui`.
- Đọc `web/AGENTS.md` trước khi viết code Next.

Không có màn hình cho cán bộ (DN-05). Khi nghiệm thu, cán bộ gọi API bằng `requests.http`/Postman với token của staff1.

## 6. Cấu hình

Thêm vào `.env.example` (và `application.yml` tương ứng):

- `INTERNAL_SERVICE_KEY=dev-internal-key`: identity và company-service phải giống nhau.
- `IDENTITY_SERVICE_URL=http://localhost:8081`: company-service gọi identity.
- `COMPANY_UPLOAD_DIR=./data/uploads`: thư mục logo. Thêm `data/` vào `.gitignore`.
- `COMPANY_DB_SCHEMA=company`: schema của company-service.
- `COMPANY_DEV_DATA_ENABLED`, hoặc dùng chung `DEV_DATA_ENABLED`, cùng profile `dev` như identity.

## 7. Kiểm thử

Test backend chạy không cần Postgres hay MailHog (H2 `MODE=PostgreSQL` + Flyway, giống `AuthFlowIntegrationTest`). company-service giả lập identity bằng `MockRestServiceServer` hoặc một HTTP server nhỏ trong test (như `MockLmsSsoIntegrationTest`).

| # | Ca | Ở đâu | Tiêu chí |
| --- | --- | --- | --- |
| 1 | staff tạo công ty → 201; tạo HR → identity được gọi đúng body và header khoá, `company_hr` được ghi | company | 1 |
| 2 | `POST /internal/users` tạo user HR `must_change_password = true`, gửi email có mật khẩu tạm (bắt bằng mock MailService), mật khẩu tạm đăng nhập được, response đăng nhập có `mustChangePassword = true`, token đó gọi API ngoài `/auth/**` (ví dụ `GET /companies/me` ở test company-service, hoặc một endpoint không thuộc `/auth/**` của identity) bị 403 `MUST_CHANGE_PASSWORD`. Không đổi hành vi NT-01: `/auth/me` vẫn 200 với token này | identity + company | 1 |
| 3 | Email trùng → identity 409 → company 409; thêm ca nối lại khi thử lại (mục 4.5) | identity + company | 2 |
| 4 | Token hr1 `PUT /companies/{idB}` → 404 (không phải 403/500) | company | 3 |
| 5 | Token hr1 `PUT /companies/me` có `id`/`companyId`/`taxCode`/`status` của B → chỉ A đổi, `taxCode`/`status` của A giữ nguyên; `GET /companies/me` bằng hr2 → B nguyên vẹn | company | 3 |
| 6 | Token hr1 gọi `/staff/companies/**` → 403 | company | 3 |
| 7 | Công ty LOCKED: `PUT /companies/me` và upload logo → 403 `COMPANY_LOCKED`; `GET` vẫn 200 | company | — |
| 8 | Logo 2MB + 1 byte → 400 `FILE_TOO_LARGE`; file 2MB đúng → 200; file `.png` giả (nội dung text) → 400 `INVALID_FILE_TYPE` | company | 4 |
| 9 | Mã số thuế 9, 11, 12, 14 chữ số hoặc có chữ → 400 `VALIDATION_ERROR` `errors.taxCode`; 10 và 13 chữ số → 201; trùng → 409 | company | 5 |
| 10 | Thiếu/sai `X-Internal-Key` → 401 | identity | — |
| 11 | identity tắt → 503 `EXTERNAL_UNAVAILABLE`; ghi `company_hr` lỗi → identity nhận `DELETE /internal/users/{id}`; `DELETE` với user không phải HR → 409 | company + identity | — |
| 12 | Route không tồn tại → 404, sai method → 405 | common-core | 3 |
| 13 | Gateway có hai route mới | gateway | — |

Frontend không có test runner; chạy `npm run type-check`, `npm run lint`, `npm run build`.

Kiểm tra tay toàn luồng (dùng để chụp ảnh màn hình cho PR): chạy Postgres + MailHog, identity và company-service với profile `dev`, gateway, web. staff1 lấy token qua SSO dev → tạo công ty → tạo HR → mở MailHog lấy mật khẩu tạm → HR đăng nhập → bị chuyển tới `/change-password` → đổi mật khẩu → mở `/hr/company` → sửa, tải logo, thử logo > 2MB.

## 8. Tài liệu bàn giao

- `docs/api/company-openapi.yaml` (mới): các endpoint ở mục 4.2, 4.3, kèm schema lỗi.
- `docs/api/identity-openapi.yaml`: thêm `POST /internal/users`, `DELETE /internal/users/{id}`, ghi rõ là nội bộ, không qua gateway.
- `docs/api/DN-01-company-hr.md` (mới, theo mẫu `NT-01-authentication.md`): mục tiêu, bảng DB, API, cách chạy, dữ liệu mẫu, ca kiểm thử, ảnh màn hình trong `docs/api/screenshots/DN-01/`.
- Cập nhật `docs/architecture/service-conventions.md` (route ngoại lệ, gọi nội bộ, mã lỗi mới) và README (cách chạy company-service với profile `dev`, biến môi trường mới).
- Thêm request mẫu cho các endpoint mới (ví dụ `docs/api/DN-01.http`).

## 9. Để người làm tự quyết

- Tên lớp, cách chia service/impl, tên file migration cụ thể.
- Nội dung chữ trong email và thông báo trên giao diện (tiếng Việt, rõ ràng).
- Chi tiết giao diện trong khuôn bố cục B: khoảng cách, component, icon.
- Cách giả lập identity trong test của company-service.
