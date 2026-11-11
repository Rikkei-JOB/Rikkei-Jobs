# Quy ước code base cho các service

## 1. Phân công

| Thành phần | Việc |
| --- | --- |
| `identity-service` | Đăng nhập, cấp JWT (NT-01). Cùng dạng lỗi `{code, message}` với các service khác; có thêm `ErrorCode` riêng (`TICKET_EXPIRED`, `ACCOUNT_LOCKED`, `MUST_CHANGE_PASSWORD`...) trong trường `code` |
| `common-core` | `ErrorResponse`, `PageResponse`, exception, `GlobalExceptionHandler`, xác thực JWT, phân quyền theo role |
| Các service nghiệp vụ | Chỉ kiểm tra JWT (không gọi identity), tự viết controller / service / entity |
| `api-gateway` | Định tuyến `/api/v1/<khu>/**` → service, bỏ tiền tố `/api/v1` |

## 2. Cổng và đường dẫn

| Service | Cổng | Controller map ở | Gateway |
| --- | --- | --- | --- |
| identity-service | 8081 | `/auth/**` | `/api/v1/auth/**` |
| profile-service | 8082 | `/profile/**` | `/api/v1/profile/**` |
| recruitment-service | 8083 | `/recruitment/**` | `/api/v1/recruitment/**` |
| learning-service | 8084 | `/learning/**` | `/api/v1/learning/**` |
| company-service | 8085 | `/company/**`, `/companies/**`, `/staff/companies/**` | `/api/v1/company/**`, `/api/v1/companies/**`, `/api/v1/staff/companies/**` |
| internship-service | 8086 | `/internship/**` | `/api/v1/internship/**` |
| assessment-service | 8087 | `/assessment/**` | `/api/v1/assessment/**` |
| ai-service | 8088 | `/ai/**` | `/api/v1/ai/**` |

Một route của gateway có thể gom nhiều tiền tố (cột cuối của company-service): DN-01 dùng `/companies/**` và `/staff/companies/**` thay vì `/company/**` để đường dẫn đọc tự nhiên (`GET /companies/me`, `POST /staff/companies`). Thêm tiền tố mới cho service nào thì sửa predicate `Path=` của route đó trong `server/api-gateway/src/main/resources/application.yml`.

Ví dụ: trình duyệt gọi `GET http://localhost:8080/api/v1/profile/ping`, gateway chuyển thành `GET http://localhost:8082/profile/ping`. Mỗi service có sẵn một `GET /<khu>/ping` (ví dụ [ProfilePingController](../../server/services/profile-service/src/main/java/com/rikkei/jobs/profile/controller/ProfilePingController.java)) để kiểm tra JWT; dùng nó làm mẫu cho controller mới.

## 3. Định dạng trả về

Theo [conventions.md](../conventions.md#2-quy-ước-api-nội-bộ): API nằm dưới `/api/v1`, field `camelCase`, thời gian ISO 8601 có múi giờ.

Thành công: trả **thẳng** đối tượng (không bọc `success`/`data`), ví dụ `{ "service": "profile-service", "userId": "...", "role": "MENTOR" }`.

Danh sách có phân trang (`?page=1&size=20`, `page` bắt đầu từ 1, `size` tối đa 100; dùng `PageResponse.of(page, items)`):

```json
{ "items": [ ], "total": 25, "page": 1, "size": 20 }
```

Lỗi (mọi service dùng chung `ErrorResponse`): hai field bắt buộc `code`, `message`; `errors` chỉ có khi cần chi tiết.

```json
{ "code": "VALIDATION_ERROR", "message": "Dữ liệu không hợp lệ", "errors": { "email": "..." } }
```

| Ném / sự kiện | HTTP | `code` |
| --- | --- | --- |
| `BadRequestException` | 400 | `BAD_REQUEST` |
| `@Valid` không đạt | 400 | `VALIDATION_ERROR` |
| không có / sai JWT | 401 | `UNAUTHORIZED` |
| `ForbiddenException`, sai role | 403 | `FORBIDDEN` |
| `ResourceNotFoundException` | 404 | `NOT_FOUND` |
| `ConflictException`, vi phạm ràng buộc DB | 409 | `CONFLICT` |
| đường dẫn không có controller | 404 | `NOT_FOUND` |
| sai phương thức HTTP | 405 | `METHOD_NOT_ALLOWED` |
| sai `Content-Type` | 415 | `UNSUPPORTED_MEDIA_TYPE` |
| thiếu phần multipart bắt buộc | 400 | `VALIDATION_ERROR` |
| lỗi không lường trước | 500 | `INTERNAL_ERROR` (chỉ log, không lộ nội dung lỗi cho client) |

Lỗi không lường trước trả 500 (không gộp vào 400 để khỏi che lỗi code). Cột thời gian của identity dùng `Instant` vì cột là `timestamptz` và cần so sánh thời điểm khoá tài khoản chính xác. Ba dòng 404/405/415 do `GlobalExceptionHandler` của `common-core` xử lý nên mọi service đều trả đúng dạng `{code, message}` (trước đây các trường hợp này bị ném lại cho cơ chế mặc định của Spring, chuyển tiếp sang `/error` vốn không nằm trong `permitAll` và không chạy lại bộ lọc JWT, nên client nhận 401 hoặc một thân lỗi mặc định thay vì `{code, message}`). Service có mã lỗi riêng thì thêm enum và `@RestControllerAdvice` của chính nó, ví dụ company-service (`CompanyErrorCode`): `COMPANY_LOCKED` → 403, `FILE_TOO_LARGE` và `INVALID_FILE_TYPE` → 400, `EXTERNAL_UNAVAILABLE` → 503. Identity có thêm `EMAIL_EXISTS` → 409 và `MAIL_UNAVAILABLE` → 503 (API nội bộ), `INVALID_SERVICE_KEY` → 401. Lỗi riêng của identity (`ACCOUNT_LOCKED` → 429, `MUST_CHANGE_PASSWORD` → 403, `TICKET_*` → 401...) vẫn giữ hình dạng `{code, message}`.

## 4. Xác thực và phân quyền

- Mọi endpoint cần JWT, trừ `/actuator/health`, `/actuator/info`, `/actuator/prometheus`.
- JWT do identity-service cấp, HS256, claims: `sub` (userId), `role`, `companyId`, và `mustChangePassword` (chỉ khi `true`). Mọi service phải dùng chung `JWT_SECRET`. Token có `mustChangePassword` bị `JwtAuthenticationFilter` chặn với 403 `MUST_CHANGE_PASSWORD` trước khi vào controller; service không cần tự kiểm tra.
- Lấy người gọi: `@AuthenticationPrincipal AuthPrincipal principal` (có `getUserId()`, `getRole()`, `getCompanyId()`).
- Phân quyền theo role bằng `@PreAuthorize`, kiểm ở API, không chỉ ẩn trên giao diện:

```java
@PreAuthorize(Roles.HR)                // hasRole('HR')
@PreAuthorize(Roles.STAFF_OR_ADMIN)    // STAFF hoặc ADMIN
@PreAuthorize("hasAnyRole('MENTOR','ADMIN')")
```

- Quyền theo dữ liệu (ví dụ "JD của mình", "ứng viên được giao") kiểm trong service, so `principal.companyId()` / `principal.userId()` với chủ sở hữu rồi ném `ForbiddenException`. Ma trận quyền đầy đủ nằm ở [NT-01-authentication.md](../api/NT-01-authentication.md).
- Endpoint công khai không cần token (ví dụ company-service phục vụ ảnh logo cho thẻ `img`, `GET /companies/logos/**`) phải khai báo trong `SecurityFilterChain` riêng của service đó; chain mặc định chỉ mở các đường `/actuator`.
- Một service cần luật bảo mật riêng thì khai báo bean `SecurityFilterChain` của chính nó, bean mặc định của `common-core` sẽ nhường chỗ.

## 5. Cấu trúc mỗi service

```text
com.rikkei.jobs.<service>/
  controller/   REST, nhận DTO, trả thẳng DTO response (hoặc ResponseEntity khi cần đặt status), @PreAuthorize trên từng method
  service/      interface + impl/ (logic nghiệp vụ, @Transactional)
  repository/   Spring Data JPA (@Repository)
  entity/       JPA entity (@Getter @Setter @NoArgsConstructor @AllArgsConstructor, @Column ghi rõ name/length/nullable)
  dto/          request/ (@Data + jakarta.validation, message tiếng Việt) và response/ (@Data @Builder)
  mapper/       entity <-> DTO (@Component, viết tay)
  client/       gọi service khác (RestClient)
  config/       exception riêng (kế thừa RuntimeException) và cấu hình riêng của service
  util/         hằng số, hàm tiện ích
```

Thêm một chức năng:

1. Entity + migration (`src/main/resources/db/migration/V<n>__<mô_tả>.sql`; thêm `flyway-core` và `flyway-database-postgresql` vào pom của service khi cần, mẫu ở identity-service).
2. Repository, DTO request/response, mapper.
3. Interface service + impl, ném `ResourceNotFoundException`, `BadRequestException`, `ConflictException`, `ForbiddenException` của `common-core` khi vi phạm nghiệp vụ.
4. Controller trả thẳng DTO, gắn `@PreAuthorize`.
5. Test (mẫu: `*PingControllerTest`; sinh token bằng `JwtUtils.generateJwtToken(userId, role, companyId)`).

## 6. Cơ sở dữ liệu và API nội bộ

**Schema riêng cho mỗi service dùng Flyway.** Service mới dùng Postgres tạo schema theo tên của mình, đặt `spring.jpa.properties.hibernate.default_schema` và `spring.flyway.schemas`/`default-schema` cùng giá trị, ví dụ company-service: biến `COMPANY_DB_SCHEMA` (mặc định `company`). Flyway tự tạo schema và chạy `V1__...sql` trong đó, nên câu SQL migration không ghi tên schema và chỉ dùng cú pháp chạy được cả trên PostgreSQL lẫn H2 `MODE=PostgreSQL` (test dùng H2). Các service có thể dùng chung một DB nhưng không đọc bảng của nhau; tham chiếu sang service khác chỉ bằng ID (ví dụ `company_hr.user_id` là `users.id` của identity, không có khoá ngoại chéo schema).

**API nội bộ giữa service (`/internal/**`).** Dành cho service gọi service (company-service → identity-service), không dành cho trình duyệt:

- Gateway **không** có route cho `/internal/**`; chỉ gọi thẳng cổng của service (identity: `http://localhost:8081/internal/...`).
- Bảo vệ bằng header `X-Internal-Key` so với biến `INTERNAL_SERVICE_KEY` (mặc định `dev-internal-key`, hai bên phải giống nhau). Thiếu hoặc sai → 401 `INVALID_SERVICE_KEY`; khoá cấu hình rỗng thì từ chối tất cả.
- Bên gọi dùng một lớp trong `client/` (RestClient + `JdkClientHttpRequestFactory`) tự gắn khoá, địa chỉ lấy từ `IDENTITY_SERVICE_URL`. Khoá không bao giờ đưa ra frontend.

## 7. Cấu hình

Mỗi service đọc `JWT_SECRET` (khoá HS256, ≥ 32 byte), cổng `<TÊN>_SERVICE_PORT`, DB `<TÊN>_DB_URL/_USER/_PASSWORD` hoặc `POSTGRES_*`. Biến môi trường mẫu nằm ở `.env.example`. company-service thêm: `INTERNAL_SERVICE_KEY`, `IDENTITY_SERVICE_URL`, `COMPANY_UPLOAD_DIR` (thư mục lưu logo, mặc định `./data/uploads`), `COMPANY_DB_SCHEMA`, `DEV_DATA_ENABLED` (seed dữ liệu mẫu, chỉ bật bằng profile `dev`).

## 8. Chưa làm

- Ngoài identity-service (NT-01), company-service (DN-01) và profile-service (HS-01), các service khác chưa có entity, migration nào. `ddl-auto: validate` sẽ báo lỗi ngay khi có entity mà chưa có migration.
