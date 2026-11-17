# DN-02 · JD: nhu cầu tuyển dụng theo vị trí, kỹ năng yêu cầu — Thiết kế

Task: **[Doanh nghiệp & JD · DN-02] JD: nhu cầu tuyển dụng theo vị trí, kỹ năng yêu cầu**. Nhánh `feature/DN-02-jobs` từ `test`, PR vào `test`, tiêu đề bắt đầu bằng `[DN-02]`. Phụ thuộc DN-01, DN-05 (đã merge) và HS-01/HS-02 (danh mục kỹ năng, `GET /skills`, đã merge).

Ký hiệu: **(chốt)** là điều người giao task đã quyết; **(người thiết kế chọn)** là quyết định trong lúc thiết kế, người làm có thể nêu lại nếu thấy sai.

## 1. Mục đích

HR tạo JD có kỹ năng yêu cầu rõ ràng để hệ thống tính được điểm khớp với ứng viên. JD ngoài đời viết mơ hồ ("nắm chắc Java", "dùng Git thành thạo"); màn hình nhập JD buộc HR chọn kỹ năng **từ danh mục** kèm **mức tối thiểu trên thang 100**, và tách rõ **bắt buộc** với **ưu tiên** (chốt):

- Thiếu kỹ năng bắt buộc khác hẳn thiếu kỹ năng ưu tiên. Cờ `required` là ranh giới cứng cho phần tính điểm sau này (task khác), không chỉ là trọng số. DN-02 chỉ lưu dữ liệu đủ rõ để làm được điều đó.
- Văn bản tự do (mô tả, yêu cầu khác, quyền lợi) vẫn có nhưng chỉ cho người đọc, hệ thống không so khớp trên đó. Những yêu cầu không có trong danh mục (tiếng Nhật N3, Redis, "đã thực tập") ghi vào phần "Yêu cầu khác".
- Năm thông tin ứng viên nhìn đầu tiên là tiêu đề/vị trí, cấp độ, lương, địa điểm, hình thức làm việc; chúng là trường có cấu trúc và nằm trên cùng form, không chôn trong mô tả.

JD mẫu dùng xuyên suốt (thiết kế, seed, kiểm tra tay): **Junior Backend Developer (Java/Spring Boot)**, Junior, toàn thời gian, Hybrid, Hà Nội, 12–20 triệu. Bắt buộc: `JAVA` 70, `OOP` 70, `SPRING_BOOT` 40, `SQL` 40, `REST_API` 40, `GIT` 70. Ưu tiên: `DOCKER` 40, `ENGLISH` 40, `AGILE` 20.

### Tiêu chí nghiệm thu (chốt)

1. JD không có kỹ năng bắt buộc → không gửi duyệt được.
2. Deadline trong quá khứ hoặc lương min > max → báo lỗi.
3. hr1 sửa JD của công ty B → 404.
4. Sửa JD đã duyệt → trạng thái về PENDING.
5. Lưu được JD loại `INTERNSHIP` (TT-02 dùng).
6. PR vào `test` (đề ghi "develop" là ghi nhầm, nhóm dùng `test` như DN-01/DN-05), tiêu đề bắt đầu `[DN-02]`, có cách chạy, dữ liệu mẫu, ảnh màn hình (đính kèm trong mô tả PR); OpenAPI cập nhật endpoint mới. Làm đủ DB, BE, FE.

### Phạm vi

Trong DN-02 (chốt):

- Bảng `jobs`, `job_skills`; API HR tạo/sửa/xem/gửi duyệt/đóng JD của công ty mình.
- Form tạo/sửa JD 2 bước và trang danh sách JD cho HR.
- `GET /internal/jobs/{jobId}/application-context` theo hợp đồng UT-01 đã merge (mục 4.5).
- Thay `jdCount = 0` tạm thời của DN-05 bằng số JD thật (mục 4.6).

Ngoài phạm vi (chốt):

- Duyệt / trả lại JD, `GET /staff/jobs`, `company_actions`, job hằng ngày đóng JD quá hạn, màn "Duyệt JD": **DN-07**.
- STAFF/ADMIN tạo hoặc sửa JD hộ công ty. JD luôn thuộc một công ty; STAFF quản lý JD (duyệt, xếp đơn ứng tuyển) qua các task DN-07 và UT.
- Trạng thái "Tạm dừng" và tự đóng khi "đủ người": chưa có task nào; khi cần thì thêm bằng migration mở rộng CHECK.
- Trang việc làm cho ứng viên (`GET /jobs` danh sách, chi tiết JD cho ứng viên, `job_views`): **DN-04**.
- Tính điểm khớp: **DN-06**. UT-02 (đã merge) gọi `GET ${MATCH_SERVICE_URL}/jobs/{id}/match/{userId}`, mặc định company-service; endpoint đó do DN-06 làm trên dữ liệu `jobs`/`job_skills` của DN-02. DN-02 không chiếm đường `/jobs/{id}/match/**`.

## 2. Ranh giới service (chốt)

- JD nằm ở **company-service**, schema `company`. Lý do: JD thuộc nhóm task "Doanh nghiệp & JD"; `company_hr`, trạng thái khoá công ty, `HrLockedFilter` và tra email HR qua identity đều đã ở đây; UT-01 (đã merge) gọi `COMPANY_SERVICE_URL` + `/internal/jobs/{jobId}/application-context`.
- recruitment-service chỉ giữ phần ứng tuyển (UT-01). Không sửa code UT-01.
- Danh mục kỹ năng thuộc profile-service (schema `profile`). company-service chỉ lưu `skill_id`, không khoá ngoại chéo schema, đọc danh mục qua `GET /skills` (mục 4.4).
- HR thuộc công ty nào: tra `company_hr` theo `sub` của JWT, như DN-01. Không dùng claim `companyId`, không đọc id công ty trong body.

## 3. Dữ liệu

Migration mới `server/services/company-service/src/main/resources/db/migration/V3__jobs.sql` (V1, V2 đã có). SQL chạy được trên cả PostgreSQL và H2 `MODE=PostgreSQL`, không ghi tên schema (quy ước của DN-01). Cập nhật `docs/database/erd.md`.

### 3.1 `jobs`

Các cột theo đề (chốt) cộng ba cột để sẵn cho DN-07 (chốt). Kiểu và độ dài do người thiết kế chọn.

| Cột | Kiểu | Ràng buộc |
| --- | --- | --- |
| `id` | UUID | PK |
| `company_id` | UUID | NOT NULL, FK → `companies(id)`, index cùng `status` |
| `title` | VARCHAR(255) | NOT NULL. Tên hiển thị, ví dụ "Junior Backend Developer (Java/Spring Boot)" |
| `position` | VARCHAR(255) | NULL. Tên vị trí ngắn, ví dụ "Backend Developer" |
| `level` | VARCHAR(10) | NULL, CHECK IN (`INTERN`, `FRESHER`, `JUNIOR`, `MIDDLE`, `SENIOR`) |
| `type` | VARCHAR(12) | NULL, CHECK IN (`FULLTIME`, `PARTTIME`, `INTERNSHIP`) |
| `quantity` | INTEGER | NULL, CHECK `quantity >= 1` |
| `city` | VARCHAR(100) | NULL |
| `address` | VARCHAR(255) | NULL |
| `work_mode` | VARCHAR(10) | NULL, CHECK IN (`ONSITE`, `REMOTE`, `HYBRID`) |
| `salary_min` | BIGINT | NULL, `>= 0`. Đơn vị VND |
| `salary_max` | BIGINT | NULL, `>= 0`. CHECK `salary_min IS NULL OR salary_max IS NULL OR salary_min <= salary_max` |
| `description` | TEXT | NULL |
| `requirements` | TEXT | NULL. "Yêu cầu khác" dạng văn bản |
| `benefits` | TEXT | NULL |
| `deadline` | DATE | NULL. Ngày cuối nhận hồ sơ, tính theo giờ `Asia/Ho_Chi_Minh`, nhận hồ sơ hết ngày đó |
| `status` | VARCHAR(10) | NOT NULL DEFAULT `DRAFT`, CHECK IN (`DRAFT`, `PENDING`, `APPROVED`, `REJECTED`, `CLOSED`) |
| `reviewed_by` | UUID | NULL. DN-07 ghi |
| `reviewed_at` | TIMESTAMP WITH TIME ZONE | NULL. DN-07 ghi |
| `reject_reason` | VARCHAR(1000) | NULL. DN-07 ghi khi trả lại |
| `created_by` | UUID | NOT NULL (user id của HR tạo) |
| `created_at`, `updated_at` | TIMESTAMP WITH TIME ZONE | NOT NULL DEFAULT CURRENT_TIMESTAMP |

Các cột nội dung để NULL được vì bản nháp chỉ bắt buộc `title`; điều kiện đầy đủ kiểm ở bước gửi duyệt (mục 4.3). Lương để trống được (JD "thoả thuận"). Entity dùng `Instant` cho cột thời gian, `LocalDate` cho `deadline`, enum Java cho `level`/`type`/`workMode`/`status` lưu dạng chuỗi.

Index (người thiết kế chọn): `(company_id, status, updated_at DESC)` cho danh sách của HR và đếm `jdCount`.

### 3.2 `job_skills`

| Cột | Kiểu | Ràng buộc |
| --- | --- | --- |
| `job_id` | UUID | NOT NULL, FK → `jobs(id)` ON DELETE CASCADE |
| `skill_id` | UUID | NOT NULL. `skills.id` của profile-service, không FK chéo schema |
| `required` | BOOLEAN | NOT NULL |
| `min_score` | INTEGER | NOT NULL, CHECK `min_score BETWEEN 1 AND 100` (chốt, xem ghi chú dưới) |
| | | PK `(job_id, skill_id)` |

Ghi chú `min_score`: đề ghi 0–100, nhưng UT-02 (đã merge) coi mọi kỹ năng có `minScore <= 0` trong kết quả điểm khớp của DN-06 là lỗi hợp đồng (502 `MATCH_CONTRACT_ERROR`, xem `HttpJobMatchClient`). Vì vậy chốt 1–100 (người giao task đã xác nhận) để JD lưu được không làm hỏng màn chi tiết đơn của HR sau này; API, zod và thanh trượt cùng giới hạn này.

ID kỹ năng ổn định: HS-01 đã cam kết sửa mã thì giữ nguyên `id` (V4 của profile-service đã làm vậy).

### 3.3 Dữ liệu mẫu (người thiết kế chọn)

Mở rộng `DevDataSeeder` của company-service (cùng cờ `app.dev-data.enabled`, profile `dev`, chạy lại không tạo trùng; dùng UUID cố định cho JD). Công ty A, `created_by` = hr1, 5 JD, mỗi JD một trạng thái:

| Tiêu đề | Cấp độ | Trạng thái | Ghi chú |
| --- | --- | --- | --- |
| Junior Backend Developer (Java/Spring Boot) | JUNIOR | REJECTED | `reject_reason` = "Mức lương chưa rõ đơn vị, phần quyền lợi còn sơ sài.", đủ 9 kỹ năng như mục 1 |
| Fresher ReactJS | FRESHER | PENDING | kỹ năng `REACTJS`, `JAVASCRIPT`, `HTML_CSS` |
| Thực tập sinh Tester | INTERN | APPROVED | `type = INTERNSHIP` (tiêu chí 5); `TESTING`; deadline xa trong tương lai để UT-01 ứng tuyển được |
| Middle Java Developer | MIDDLE | DRAFT | chưa có deadline, chưa có kỹ năng |
| Senior DevOps | SENIOR | CLOSED | deadline đã qua |

Seeder không gọi profile-service: dùng thẳng các UUID kỹ năng cố định của `V2__skill_catalog.sql` (ví dụ `JAVA` = `00000000-0000-0000-0001-000000000001`). Deadline tính tương đối theo `Clock` (ví dụ hôm nay + 45 ngày) để seed không hết hạn theo thời gian.

## 4. API

Lỗi theo `ErrorResponse {code, message, errors?}`, message tiếng Việt. Đường dẫn ở service; qua gateway thêm `/api/v1`.

### 4.1 Gateway

Thêm `/api/v1/jobs/**` vào predicate route `company` hiện có trong `server/api-gateway/src/main/resources/application.yml`, giữ `StripPrefix=2`. `/api/v1/companies/**` đã có nên `/companies/me/jobs` không cần thêm. Không thêm route id mới; thêm ca vào `CompanyRouteIntegrationTest`. Đường ứng tuyển của UT-01 nằm dưới `/api/v1/recruitment/**`, không đụng. `/internal/**` không có route gateway.

### 4.2 Quyền chung cho API HR

- `@PreAuthorize(Roles.HR)` trên mọi method. Token STAFF/ADMIN/CANDIDATE → 403 `FORBIDDEN`.
- Công ty của người gọi: `company_hr` theo `principal.getUserId()`; không có → 404. HR bị khoá đã bị `HrLockedFilter` chặn 401 `ACCOUNT_DISABLED` trước khi vào controller.
- JD của công ty khác, hoặc id không tồn tại → **404 `NOT_FOUND`** giống hệt nhau (lọc bằng `findByIdAndCompanyId`).
- Công ty `LOCKED`: đọc (`GET`) vẫn 200; mọi thao tác ghi (`POST /jobs`, `PUT`, `submit`, `close`) → 403 `COMPANY_LOCKED` (mã đã có của DN-01).
- Body không bao giờ quyết định `companyId`, `status`, `createdBy`, `reviewed*`, `rejectReason`. DTO không có các field đó; field lạ bị Jackson bỏ qua.

### 4.3 Vòng đời và quy tắc

```text
DRAFT ──submit──▶ PENDING ──(DN-07 duyệt)──▶ APPROVED ──close──▶ CLOSED
                     ▲         └──(DN-07 trả lại)──▶ REJECTED
                     │                                   │
                     └──────────── sửa rồi submit ───────┘
APPROVED ──HR sửa (PUT /jobs/{id} hoặc PUT /jobs/{id}/skills), qua kiểm tra như submit──▶ PENDING
```

| Trạng thái | Sửa (`PUT /jobs/{id}`, `PUT /jobs/{id}/skills`) | `submit` | `close` |
| --- | --- | --- | --- |
| DRAFT | được, giữ DRAFT | → PENDING | 409 |
| REJECTED | được, giữ REJECTED | → PENDING, xoá `reject_reason` | 409 |
| APPROVED | được, **chuyển PENDING** (chốt), giữ `reviewed_by/at` cũ. Dữ liệu sau khi sửa phải qua đủ kiểm tra "gửi duyệt" bên dưới (người thiết kế chọn: sửa JD đã duyệt chính là gửi duyệt lại, không được để JD PENDING thiếu kỹ năng bắt buộc) | 409 | → CLOSED |
| PENDING | **409** `JOB_NOT_EDITABLE` (chốt) | 409 | 409 |
| CLOSED | 409 `JOB_NOT_EDITABLE` | 409 | 409 |

Mã lỗi mới, thêm vào `CompanyErrorCode`: `JOB_NOT_EDITABLE` → 409 ("JD đang chờ duyệt hoặc đã đóng, không sửa được"), `INVALID_JOB_STATUS` → 409 (submit/close sai trạng thái). Trong lúc chờ duyệt lại sau khi sửa JD APPROVED, ứng viên không thấy JD (người thiết kế chọn; context của UT-01 trả `status = PENDING` nên UT-01 tự chặn ứng tuyển).

Kiểm tra dữ liệu, chia hai mức (người thiết kế chọn mức nháp; mức gửi duyệt theo đề):

- **Mọi lần lưu** (`POST`, `PUT`): `title` bắt buộc, ≤ 255; enum đúng giá trị; trường nào có giá trị thì phải đúng: `quantity >= 1`, `salaryMin >= 0`, `salaryMax >= 0`, `salaryMin <= salaryMax` khi có cả hai, `deadline >= hôm nay` theo `Asia/Ho_Chi_Minh` (tiêu chí 2, áp dụng cả khi lưu nháp; seeder ghi thẳng DB nên JD đã đóng vẫn có deadline cũ); độ dài chuỗi theo cột. Sai → 400 `VALIDATION_ERROR` kèm `errors.<field>` (`errors.salaryMax` cho lỗi min > max). Riêng JSON hỏng, enum sai giá trị, id trên đường dẫn không phải UUID: giữ hành vi sẵn có của `GlobalExceptionHandler` (400 `BAD_REQUEST`, không có `errors`); không viết handler mới (người thiết kế chọn). Frontend dùng select nên không gửi enum sai.
- **Kỹ năng** (`skills` trong `POST`/`PUT /jobs/{id}`, hoặc `PUT /jobs/{id}/skills`): mảng tối đa 20 phần tử (bằng cỡ danh mục), không trùng `skillId`, `minScore` nguyên 1–100 (xem mục 3.2), `required` không null, mọi `skillId` có trong danh mục (mục 4.4). Mảng rỗng hợp lệ (xoá hết). Sai → 400 `VALIDATION_ERROR` với `errors.skills` (hoặc `errors["skills[i].minScore"]`).
- **Gửi duyệt** (`submit`), kiểm thêm trên dữ liệu đã lưu: đủ `title`, `position`, `level`, `type`, `quantity`, `city`, `workMode`, `deadline`, `description`; `deadline >= hôm nay` theo `Asia/Ho_Chi_Minh` (dùng `Clock` bean đã có để test được); có ≥ 1 kỹ năng `required = true`; lương thoả quy tắc trên. Thiếu → 400 `VALIDATION_ERROR`, `errors` liệt kê **mọi** điều kiện sai cùng lúc (khoá là tên field, `skills` cho điều kiện kỹ năng bắt buộc) để form chỉ đúng chỗ. Gửi duyệt không kiểm lại danh mục kỹ năng (đã kiểm lúc lưu).

Mỗi lần lưu hoặc đổi trạng thái chạy trong một transaction và chống ghi đè đồng thời bằng `@Version` (cột `version BIGINT NOT NULL DEFAULT 0` trong `jobs`; người thiết kế chọn). Lưu chỉ kỹ năng cũng phải tăng `version` (cập nhật `updated_at` của `jobs`). Xung đột → 409 `CONFLICT`. `version` chỉ dùng nội bộ, không đưa ra DTO: không chặn trường hợp hai HR cùng công ty mở form cũ rồi lưu đè lần lượt (chấp nhận được ở phạm vi demo).

### 4.4 Endpoint cho HR

**`POST /jobs`** → `201` `JobResponse`, status `DRAFT`, `companyId` từ `company_hr`, `createdBy = principal.userId()`. Body: `JobRequest` (dưới).

**`PUT /jobs/{id}`** → `200` `JobResponse`. Thay toàn bộ phần thông tin chung (giống `PUT /companies/me`): field không gửi hoặc `null` bị xoá, trừ `title`.

`JobRequest`: `title`, `position`, `level`, `type`, `quantity`, `city`, `address`, `workMode`, `salaryMin`, `salaryMax`, `description`, `requirements`, `benefits`, `deadline` (`yyyy-MM-dd`), và **`skills` tuỳ chọn** `[{skillId, required, minScore}]`.

- `skills` có mặt (kể cả mảng rỗng) → thay toàn bộ danh sách kỹ năng **trong cùng transaction** với thông tin chung, chỉ một lần đổi trạng thái. `skills` vắng mặt (`null`) → giữ nguyên kỹ năng (người thiết kế chọn).
- Lý do: lưu bằng hai lời gọi lần lượt sẽ hỏng với JD APPROVED (lời gọi đầu chuyển JD sang PENDING, lời gọi thứ hai bị 409) và có thể lưu dở dang khi profile-service lỗi ở lời gọi thứ hai. Frontend luôn lưu bằng **một** lời gọi `POST /jobs` hoặc `PUT /jobs/{id}` kèm `skills`.

**`PUT /jobs/{id}/skills`** → `200` `JobResponse`. Body: `[{skillId, required, minScore}]`, thay toàn bộ danh sách. Giữ theo đề cho client chỉ sửa kỹ năng (Postman, task sau); cùng quy tắc trạng thái và kiểm tra như `PUT /jobs/{id}`. Frontend của DN-02 không gọi endpoint này.

Kiểm danh mục: company-service gọi `GET ${PROFILE_SERVICE_URL}/skills`, **chuyển tiếp header `Authorization` của HR** (endpoint chỉ cần đăng nhập). Client mới `client/SkillCatalogClient` (RestClient + `JdkClientHttpRequestFactory`, timeout kết nối/đọc 5 giây, không thử lại, biến `PROFILE_SERVICE_URL` mặc định `http://localhost:8082`). Chỉ gọi khi request có `skills`. profile-service tắt, timeout, 5xx → 503 `EXTERNAL_UNAVAILABLE`, nhưng message riêng "Không kiểm tra được danh mục kỹ năng, vui lòng thử lại." (message mặc định của mã này nói về dịch vụ tài khoản). Lỗi xảy ra trước khi ghi nên không có lưu dở dang. Không cache (danh mục 20 dòng).

**`GET /companies/me/jobs?status=&page=1&size=20`** → `200` `PageResponse<JobSummary>`. `status` tuỳ chọn, một trong 5 giá trị; sai → 400. Phân trang theo quy ước (page từ 1, size ≤ 100). Sắp `updated_at` giảm dần, rồi `id`. `JobSummary`: `id, title, level, status, deadline, updatedAt`. Controller đặt trong company-service cạnh `MyCompanyController` (map `/companies/me/jobs`).

**`GET /jobs/{id}`** → `200` `JobResponse`. DN-02 chỉ mở cho HR. DN-04 (chi tiết JD cho ứng viên) và DN-07 (STAFF xem) sẽ mở rộng **cùng method** này theo vai trò, vì Spring không map hai method cùng đường dẫn. Vì vậy controller để phân quyền ở tầng service theo vai trò (method `@PreAuthorize(Roles.HR)` ở DN-02, task sau nới ra), không đặt cứng ở mức class.

**`POST /jobs/{id}/submit`** → `200` `JobResponse`. **`POST /jobs/{id}/close`** → `200` `JobResponse`. Không có body.

`JobResponse`: mọi cột của `jobs` trừ `created_by`/`version`, dạng camelCase (`companyId, title, position, level, type, quantity, city, address, workMode, salaryMin, salaryMax, description, requirements, benefits, deadline, status, rejectReason, reviewedAt, createdAt, updatedAt`) và `skills: [{skillId, required, minScore}]` sắp `required` trước rồi `minScore` giảm dần. `rejectReason` chỉ khác null khi `status = REJECTED`. Backend không trả tên kỹ năng; frontend ghép với danh mục `GET /skills` đã tải cho ô chọn (người thiết kế chọn).

### 4.5 API nội bộ cho UT-01

**`GET /internal/jobs/{jobId}/application-context`** → `200`, hợp đồng đúng như UT-01 đã dựng ([UT-01-applications.md](../../api/UT-01-applications.md), [ut01-dependencies-openapi.yaml](../../api/ut01-dependencies-openapi.yaml), record `JobApplicationContext` của recruitment-service). Không đổi hợp đồng.

```json
{
  "jobId": "…", "companyId": "…", "jobTitle": "Thực tập sinh Tester", "companyName": "Công ty A",
  "status": "APPROVED", "deadline": "2027-01-01T00:00:00+07:00", "companyLocked": false,
  "hrEmails": ["hr1@rikkeijobs.test"]
}
```

- `deadline`: UT-01 so `deadline.isAfter(now)`, nên trả **thời điểm 00:00 ngày kế tiếp** theo `Asia/Ho_Chi_Minh` (JD hạn 31/12 nhận hồ sơ hết ngày 31/12). JD chưa có `deadline` (chỉ có ở DRAFT) trả `deadline` = `createdAt` để không bao giờ "còn hạn"; UT-01 vốn chặn mọi trạng thái khác APPROVED.
- `companyLocked`: `companies.status = LOCKED`.
- `hrEmails`: HR của công ty có `company_hr.locked_at IS NULL`, tra email bằng `IdentityClient.lookupUsers` (đã có, `POST /internal/users/lookup`), chỉ lấy `status = ACTIVE`. Không có HR nào → mảng rỗng. identity lỗi → 503 `EXTERNAL_UNAVAILABLE` (UT-01 coi là dịch vụ chưa sẵn sàng).
- JD không tồn tại → 404; `jobId` không phải UUID → 400 (mã `BAD_REQUEST` sẵn có của common-core; UT-01 chỉ dùng HTTP status).
- Bảo vệ: company-service hiện chưa nhận lời gọi nội bộ nào. Thêm `InternalKeyFilter` cho `/internal/**` theo mẫu identity (`X-Internal-Key` so hằng thời gian với `app.internal.service-key` đã có, khoá rỗng từ chối tất cả, thiếu/sai → 401 `INVALID_SERVICE_KEY`). `CompanySecurityConfig` thêm `permitAll` cho `/internal/**` (JWT không áp dụng), filter nội bộ đặt trước `JwtAuthenticationFilter`. Không ghi `job_views`.

### 4.6 `jdCount` của DN-05

`GET /staff/companies` trả `jdCount` = số JD của công ty, **mọi trạng thái trừ DRAFT** (người thiết kế chọn: cán bộ quan tâm JD đã gửi lên hệ thống). Đếm bằng một truy vấn gom nhóm theo các `company_id` của trang hiện tại, không N+1. Sửa javadoc `StaffCompanyItem`, `@ApiResponse` của `StaffCompanyController`, test `StaffCompanyListApiTest` (đang khẳng định `jdCount = 0`) và tài liệu DN-05. Mục "Việc làm đang tuyển (0)" trong `company-public-view.tsx` để DN-04 làm (cần danh sách JD công khai).

## 5. Frontend

Đọc `web/AGENTS.md` trước khi viết code Next. Dùng react-hook-form + zod, TanStack Query, `api` sẵn có, component trong `src/app/components/ui`, cùng phong cách trang Hồ sơ công ty. Mục "JD" (`/hr/jobs`) đã có trong `src/config/menu.ts`; trang tĩnh thay cho trang tạm `[role]/[...section]`.

File (người thiết kế chọn tên, người làm có thể đổi): `web/src/app/(pages)/(app)/hr/jobs/page.tsx`, `hr/jobs/new/page.tsx`, `hr/jobs/[id]/page.tsx`; `src/services/job.service.ts`, `src/interface/job.ts`, `src/schemas/job.schema.ts`, `src/hooks/use-my-jobs.ts`; component trong `src/app/components/job/`. Danh mục kỹ năng dùng `skillService.getCatalog()` và kiểu `Skill` đã có.

Mockup đã xem nằm ở `.superpowers/brainstorm/1568-1794920669/content/` (`jd-form-layout.html`, `jd-skills-step.html`, `jd-list.html`; không commit). Người dùng chọn phương án **A** ở cả ba màn.

### 5.1 Danh sách `/hr/jobs` (chốt: bảng + tab)

- Tiêu đề "JD của <tên công ty>", nút "+ Tạo JD".
- Tab: Tất cả / Nháp / Chờ duyệt / Đang tuyển / Bị trả lại / Đã đóng (gọi `GET /companies/me/jobs?status=`).
- Bảng: Tiêu đề, Cấp độ, Hạn nộp, Trạng thái (nhãn màu: Nháp xám, Chờ duyệt vàng, Đang tuyển xanh lá, Bị trả lại đỏ, Đã đóng xám đậm), Cập nhật. Phân trang. Không hiện lý do trả lại trong bảng.
- Bấm một dòng: DRAFT/REJECTED/APPROVED mở form sửa; PENDING/CLOSED mở chế độ chỉ xem.
- Công ty bị khoá: banner như trang công ty, ẩn "+ Tạo JD".

### 5.2 Form tạo/sửa (chốt: một cột, 2 bước)

Thanh bước "1. Thông tin chung" → "2. Kỹ năng yêu cầu". Tiêu đề trang kèm nhãn trạng thái. JD `REJECTED`: banner đỏ "Lý do trả lại: …" ở đầu form, cả hai bước.

**Bước 1 — Thông tin chung**, năm thông tin chính trên cùng:

- Tiêu đề (bắt buộc, toàn dòng)
- Vị trí · Cấp độ · Loại hình (đều bắt buộc)
- Lương từ (VND) · đến · Hình thức (bắt buộc)
- Thành phố · Số lượng · Hạn nộp (đều bắt buộc)
- Mô tả công việc (textarea)
- Yêu cầu khác · Quyền lợi (hai textarea), gợi ý dưới "Yêu cầu khác": "Ghi những yêu cầu không có trong danh mục kỹ năng, ví dụ tiếng Nhật N3."

Trên giao diện các ô bắt buộc có dấu sao; "bắt buộc" nghĩa là bắt buộc **khi gửi duyệt**, lưu nháp chỉ cần Tiêu đề.

**Bước 2 — Kỹ năng yêu cầu** (chốt: hai khối riêng):

- Ô tìm "Thêm kỹ năng…" lọc trong danh mục, ẩn kỹ năng đã chọn. Kỹ năng thêm vào mặc định ở khối **Bắt buộc**, `minScore` mặc định 40.
- Khối **Bắt buộc** (đỏ, chú thích "thiếu là không đạt") và khối **Ưu tiên** (xanh, "có thì cộng điểm"), mỗi khối đếm số kỹ năng. Mỗi dòng: tên kỹ năng, thanh trượt, giá trị + tên bậc, nút xoá, và menu/nút chuyển sang khối kia.
- Thanh trượt (chốt): thang 100 bước 1 (giá trị nhỏ nhất 1, theo mục 3.2), vạch ở 40/70/85, nền chia bốn đoạn màu, nhãn bên phải dạng "70 · Tốt" theo bậc của `docs/conventions.md` (`<40` Cơ bản, `<70` Khá, `<85` Tốt, còn lại Xuất sắc).

**Nút và hành vi** (người thiết kế chọn, người dùng đã đồng ý):

- Chuyển bước không gọi API; dữ liệu hai bước giữ trong một form.
- **Lưu nháp** ở cả hai bước: lần đầu một lời gọi `POST /jobs` kèm `skills`, sau đó chuyển URL sang `/hr/jobs/{id}`; các lần sau một lời gọi `PUT /jobs/{id}` kèm `skills` (mục 4.4). Báo "Đã lưu".
- **Gửi duyệt** chỉ ở bước 2: lưu như trên rồi `POST /jobs/{id}/submit`. Lỗi `VALIDATION_ERROR` từ backend hiện theo từng trường và chuyển về bước có lỗi (`skills` → bước 2, còn lại → bước 1). Thành công → về danh sách, báo "Đã gửi duyệt".
- JD **APPROVED**: nút lưu đổi thành "Lưu và gửi duyệt lại", trước khi lưu có hộp xác nhận "JD sẽ chuyển về Chờ duyệt, ứng viên tạm thời không thấy JD cho tới khi được duyệt lại." Có nút **Đóng JD** kèm hộp xác nhận.
- JD **PENDING/CLOSED**: cùng bố cục, mọi ô chỉ đọc, ẩn nút lưu/gửi; PENDING ghi chú "JD đang chờ duyệt, không sửa được".
- zod cùng luật backend: lưu nháp chỉ kiểm định dạng; gửi duyệt kiểm đủ trường, `salaryMin <= salaryMax`, `quantity >= 1`, hạn nộp ≥ hôm nay, ≥ 1 kỹ năng bắt buộc. Backend vẫn kiểm lại.
- Rời trang khi còn thay đổi chưa lưu: không bắt buộc (người làm tự quyết).

## 6. Cấu hình

- company-service thêm `app.profile.base-url: ${PROFILE_SERVICE_URL:http://localhost:8082}`. Ghi vào `.env.example` (biến đã có cho recruitment) và mục Cấu hình của `docs/architecture/service-conventions.md`.
- `INTERNAL_SERVICE_KEY` đã có; recruitment-service dùng cùng giá trị (UT-01 đọc `RECRUITMENT_COMPANY_SERVICE_KEY` hoặc `INTERNAL_SERVICE_KEY`).

## 7. Kiểm thử

Test backend chạy không cần Postgres (H2 `MODE=PostgreSQL` + Flyway như các test company-service hiện có). Giả lập profile-service và identity bằng `MockRestServiceServer` hoặc mock bean của client.

| # | Ca | Ở đâu | Tiêu chí |
| --- | --- | --- | --- |
| 1 | hr1 `POST /jobs` chỉ có `title` → 201 DRAFT, `companyId` = A dù body gửi `companyId`/`status` khác; `POST`/`PUT` với `type = INTERNSHIP` → lưu và đọc lại đúng | company | 5 |
| 2 | `PUT /jobs/{id}/skills` hợp lệ → 200, đọc lại đúng thứ tự; `skillId` lạ → 400 `errors.skills`; trùng `skillId`, `minScore` 101 hoặc 0 → 400; profile tắt → 503 và JD không đổi; `PUT /jobs/{id}` kèm `skills` lưu cả hai phần trong một transaction, không kèm `skills` thì giữ nguyên kỹ năng | company | — |
| 3 | `submit` khi chỉ có kỹ năng ưu tiên hoặc không có kỹ năng → 400 `errors.skills`, thiếu trường → 400 liệt kê đủ các lỗi; đủ điều kiện → PENDING | company | 1 |
| 3b | `POST`/`PUT` với deadline hôm qua → 400 `errors.deadline`; deadline đúng hôm nay (theo `Clock` cố định giờ VN) → được; `salaryMin > salaryMax` → 400 `errors.salaryMax` | company | 2 |
| 4 | Ma trận trạng thái mục 4.3: sửa PENDING/CLOSED → 409 `JOB_NOT_EDITABLE`; sửa APPROVED (cả thông tin chung lẫn kỹ năng) → PENDING; sửa APPROVED mà bỏ hết kỹ năng bắt buộc → 400, JD vẫn APPROVED; submit REJECTED → PENDING và `rejectReason` null; close DRAFT/PENDING → 409 `INVALID_JOB_STATUS`; close APPROVED → CLOSED | company | 4 |
| 5 | hr1 `GET/PUT/submit/close` JD của công ty B → 404; `GET /companies/me/jobs` của hr1 không thấy JD của B | company | 3 |
| 6 | Token STAFF, CANDIDATE gọi API HR → 403; HR bị khoá → 401 `ACCOUNT_DISABLED` | company | — |
| 7 | Công ty LOCKED: GET → 200; POST/PUT/submit/close → 403 `COMPANY_LOCKED` | company | — |
| 8 | `GET /companies/me/jobs?status=` lọc đúng, phân trang `total`, `status` sai → 400 | company | — |
| 9 | `/internal/jobs/{id}/application-context`: đúng body (deadline 00:00 ngày kế tiếp +07:00, `hrEmails` bỏ HR bị khoá), thiếu/sai `X-Internal-Key` → 401, id lạ → 404, id sai định dạng → 400; JWT hợp lệ không thay được khoá nội bộ | company | — |
| 10 | `jdCount` đếm đúng, bỏ DRAFT | company | — |
| 11 | Seeder: chạy hai lần không trùng, đủ 5 JD/5 trạng thái | company | 6 |
| 12 | Gateway chuyển `/api/v1/jobs/**` tới company-service | gateway | — |

Frontend không có test runner: chạy `npm run type-check`, `npm run lint`, `npm run build`.

Kiểm tra tay toàn luồng: Postgres, identity, profile, company (profile `dev`), recruitment, gateway, web. hr1 đăng nhập → `/hr/jobs` thấy 5 JD → mở JD bị trả lại, thấy lý do → sửa lương, bước 2 chỉnh kỹ năng → Gửi duyệt → thành Chờ duyệt, mở lại chỉ xem được → tạo JD mới, lưu nháp ở bước 1, thêm kỹ năng, thử gửi khi chưa có kỹ năng bắt buộc (báo lỗi ở bước 2) → gọi `application-context` của JD Đang tuyển bằng `X-Internal-Key` → candidate1 ứng tuyển JD đó bằng cách gọi thẳng `POST /api/v1/recruitment/jobs/{jobId}/apply` với `jobId` seed (trang việc làm của ứng viên vẫn đọc mock qua `NEXT_PUBLIC_JOB_API_PATH` cho tới DN-04) → **sau đó** mở JD Đang tuyển, sửa, xác nhận, thành Chờ duyệt; `application-context` giờ trả `PENDING`.

## 8. Tài liệu bàn giao

- `docs/api/company-openapi.yaml`: thêm endpoint mục 4.4, 4.5 (ghi rõ `/internal/**` không qua gateway), mã lỗi mới. Springdoc `@Operation`/`@ApiResponse` trên controller.
- `docs/api/DN-02-jobs.md` (mới, theo mẫu `DN-01-company-hr.md`): mục tiêu, bảng DB, API, vòng đời, cách chạy, dữ liệu mẫu, ca kiểm thử.
- `docs/api/DN-02.http`: request mẫu chạy được.
- Cập nhật `docs/database/erd.md`, `docs/architecture/service-conventions.md` (company-service nhận `/internal/**`, mã lỗi mới, `PROFILE_SERVICE_URL`), `docs/api/DN-05-company-hr-ui.md` (`jdCount` thật), README nếu lệnh chạy đổi.
- Ghi chú cho UT-01 trong `docs/api/UT-01-applications.md`: producer thật của `application-context` là company-service (DN-02).

## 9. Để người làm tự quyết

- Tên lớp, cách chia service/impl, tên DTO, tên file frontend.
- Nội dung chữ trên giao diện và thông báo lỗi (tiếng Việt, rõ ràng).
- Chi tiết giao diện trong khuôn đã chốt: khoảng cách, icon, component cụ thể cho ô tìm kỹ năng và thanh trượt (`@base-ui/react` có Slider).
- Cách chuyển kỹ năng giữa hai khối (nút, menu hay kéo thả).
- Có cảnh báo rời trang khi chưa lưu hay không.
