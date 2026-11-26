# DN-07 · Cán bộ duyệt JD, quản lý doanh nghiệp — Thiết kế

Task: **[Doanh nghiệp & JD · DN-07] Cán bộ duyệt JD, quản lý doanh nghiệp**. Nhánh `feature/DN-07-job-review` từ `test`, PR vào `test` (đề ghi "develop" là ghi nhầm, nhóm dùng `test` như DN-01/02/05), tiêu đề bắt đầu `[DN-07]`. Phụ thuộc DN-02 (bảng `jobs`), DN-05 (màn doanh nghiệp), đều đã merge; DN-06 (điểm khớp) đã merge.

Ký hiệu: **(chốt)** là điều người giao task đã quyết; **(người thiết kế chọn)** là quyết định trong lúc thiết kế, người làm có thể nêu lại nếu thấy sai.

## 1. Mục đích

Cán bộ tuyển dụng (STAFF) kiểm soát JD trước khi ứng viên thấy, và khoá được doanh nghiệp vi phạm. Ứng viên chỉ thấy JD đã được duyệt, còn hạn, của công ty không bị khoá.

### Tiêu chí nghiệm thu (chốt)

1. JD `PENDING` không hiện ở trang việc làm; duyệt xong thì hiện.
2. Trả lại không có lý do → 400; HR thấy lý do, sửa và gửi lại được (phần HR đã có từ DN-02).
3. Khoá công ty A → JD của A biến khỏi trang việc làm, hr1 không đăng nhập được; mở khoá → hiện lại.
4. Lịch sử ghi người làm, thời điểm, lý do.
5. Duyệt JD đang `DRAFT` → 409.
6. PR vào `test` có cách chạy, dữ liệu mẫu, ảnh màn hình; OpenAPI cập nhật endpoint mới. Làm đủ DB, BE, FE.

### Phạm vi

Trong DN-07 (chốt):

- Duyệt / trả lại / **đóng** JD của cán bộ (đóng JD: người dùng chốt thêm, đề chỉ ghi trong sơ đồ trạng thái), hàng chờ `GET /staff/jobs`.
- Khoá / mở khoá công ty; HR của công ty bị khoá không đăng nhập được (kể cả token đang còn hạn).
- Bảng `company_actions` (thêm giá trị `CLOSE_JD`), `GET /staff/companies/{id}/history`, tab "Lịch sử thao tác".
- Job hằng ngày đóng JD quá hạn.
- Màn "Duyệt JD" `/staff/jobs`.
- **Backend của trang việc làm** (chốt phương án A, để demo tiêu chí 1 và 3 trên giao diện): quy tắc "ứng viên thấy JD nào", `GET /jobs` và `GET /jobs/{id}` dạng ứng viên thấy. **Cập nhật 26/11:** giao diện ứng viên `/candidate/jobs` đã được DN-04 (người khác) làm và merge vào `test`; `candidate-job.service.ts` (commit `d7198e6`) đã đọc được hợp đồng `{items: PublicJobSummary[]}` / `PublicJobDetail` của mục 4.3 khi đặt `NEXT_PUBLIC_JOB_API_PATH=/jobs`. DN-07 **không sửa** trang ứng viên; demo trang việc làm bằng cấu hình đó (chốt).
- Tab **"JD của công ty"** trong trang chi tiết doanh nghiệp của cán bộ (chốt trong spec DN-05 làm mới giao diện `2026-11-24-dn-ui-refresh-design.md`, dời sang DN-07).

Ngoài phạm vi:

- Phần còn lại của DN-04 (cùng người làm): bộ lọc `q/position/level/type/city/skillIds`, `sort=match`, `matchScore`, điểm khớp chi tiết, `job_views`, trang việc làm cho khách chưa đăng nhập, các chỉ mục `jobs(city)`, `job_skills(skill_id)`. DN-07 thiết kế để DN-04 chỉ **thêm vào** endpoint và component của DN-07, không sửa lại (chốt).
- Gửi email báo HR khi JD được duyệt / bị trả lại / bị đóng (người thiết kế chọn: HR xem ngay trên `/hr/jobs`).
- ADMIN duyệt JD hay khoá công ty: chỉ STAFF, như DN-05 (người thiết kế chọn theo đề).
- Ghi lịch sử cho việc hệ thống tự đóng JD quá hạn (chốt: lịch sử chỉ ghi thao tác của cán bộ).
- Trạng thái "Tạm dừng", tự đóng khi "đủ người".

## 2. Ranh giới service (chốt phương án A cho khoá công ty)

- **company-service**: duyệt / trả lại / đóng JD, khoá / mở khoá công ty, `company_actions`, lịch sử, job tự đóng, quy tắc "ứng viên thấy JD nào", `GET /jobs`, chi tiết JD cho ứng viên. Bảng `companies.status` vẫn là nguồn sự thật của trạng thái công ty.
- **identity-service**: thêm cờ **khoá theo công ty** `users.company_locked_at`, tách khỏi cờ khoá riêng từng HR của DN-05 (`users.locked_at`). company-service bật / tắt cờ này cho danh sách HR (lấy từ `company_hr`) qua API nội bộ mới. Mọi điểm đang chặn HR bị khoá riêng thì chặn cả HR bị khoá theo công ty.
- **recruitment-service**: không sửa. `HrLockedFilter` của nó hỏi `POST /internal/users/lookup`; khi `lookup` trả `status = LOCKED` cho HR bị khoá theo công ty thì UT-02 tự được bao.
- Phương án đã loại: (B) company-service khoá lần lượt từng HR bằng API khoá riêng của DN-05 — nhiều lời gọi không nguyên tử, lẫn lý do khoá riêng với khoá công ty, cán bộ mở khoá một HR là HR đăng nhập lại được trong khi công ty vẫn bị khoá; (C) identity hỏi company-service mỗi lần đăng nhập — vòng phụ thuộc company ↔ identity, company tắt thì HR không đăng nhập được, bộ lọc JWT phải gọi sang company mỗi request.

## 3. Dữ liệu

### 3.1 company-service — `V6__job_review.sql`

Migration hiện có: V1–V5 (V4 của DN-06, V5 `drop_english_skill` của HS-04). SQL chạy được trên cả PostgreSQL và H2 `MODE=PostgreSQL`, không ghi tên schema.

`company_actions` (cột theo đề, thêm `CLOSE_JD`; kiểu do người thiết kế chọn):

| Cột | Kiểu | Ràng buộc |
| --- | --- | --- |
| `id` | UUID | PK |
| `company_id` | UUID | NOT NULL, FK → `companies(id)` |
| `action` | VARCHAR(12) | NOT NULL, CHECK IN (`LOCK`, `UNLOCK`, `APPROVE_JD`, `REJECT_JD`, `CLOSE_JD`) |
| `target_id` | UUID | NULL. Id JD với `*_JD`; NULL với `LOCK`/`UNLOCK`. Không FK (giữ lịch sử kể cả khi sau này có xoá JD) |
| `reason` | VARCHAR(1000) | NULL. Bắt buộc ở tầng API với `LOCK`, `REJECT_JD`, `CLOSE_JD` |
| `staff_id` | UUID | NOT NULL (user id cán bộ, từ JWT) |
| `created_at` | TIMESTAMP WITH TIME ZONE | NOT NULL DEFAULT CURRENT_TIMESTAMP |

Chỉ mục `idx_company_actions_company_created (company_id, created_at)` cho trang lịch sử. Bảng chỉ thêm, không sửa, không xoá.

`jobs`: không đổi cột (`status`, `reviewed_by`, `reviewed_at`, `reject_reason` đã có từ DN-02). Thêm chỉ mục `idx_jobs_status_deadline (status, deadline)` cho `GET /jobs` và job tự đóng (DN-04 có liệt kê chỉ mục này; DN-07 tạo trước, DN-04 không tạo lại).

Backfill trong cùng migration (người thiết kế chọn): `UPDATE jobs SET reviewed_at = updated_at WHERE status IN ('APPROVED', 'REJECTED', 'CLOSED') AND reviewed_at IS NULL;` — các JD seed của DN-02/DN-06 và JD duyệt tạm bằng SQL chưa có `reviewed_at`, sẽ làm `publishedAt` rỗng và thứ tự "mới nhất" khác nhau giữa PostgreSQL và H2. `reviewed_by` để NULL (không bịa người duyệt).

`companies`: không đổi cột. Thông tin "khoá lúc nào, bởi ai, lý do" lấy từ dòng `LOCK` mới nhất trong `company_actions` (người thiết kế chọn, tránh chép trùng dữ liệu).

### 3.2 identity-service — `V3__company_lock.sql`

`ALTER TABLE users ADD COLUMN company_locked_at TIMESTAMP WITH TIME ZONE;` — khác NULL nghĩa là HR đang bị khoá theo công ty. Cột `locked_at`/`locked_reason` của DN-05 giữ nguyên ý nghĩa (khoá riêng).

Entity `User` thêm `companyLockedAt` và phương thức `isLoginBlocked()` = `isAdminLocked() || companyLockedAt != null`. `isAdminLocked()` giữ nguyên cho logic mở khoá riêng (DN-05).

### 3.3 Dữ liệu mẫu (người thiết kế chọn)

Mở rộng `DevDataSeeder` của company-service (profile `dev`, chạy lại không trùng, UUID cố định, cùng transaction cho mỗi JD như DN-02): thêm cho **Công ty B** (hr2) hai JD — một `APPROVED` (ví dụ "Thực tập sinh Marketing", INTERNSHIP, hạn nộp hôm nay + 45 ngày, một kỹ năng bắt buộc) và một `PENDING` (ví dụ "Nhân viên vận hành sàn TMĐT"). Mục đích: hàng chờ có JD của hai công ty, và khi khoá Công ty A thì trang việc làm vẫn còn JD của B (thấy rõ chỉ JD của A biến mất). Seeder đặt `reviewed_at` cho mọi JD seed ở trạng thái đã xử lý (`APPROVED`/`REJECTED`/`CLOSED`). Không seed `company_actions`. JD của Công ty A giữ nguyên: hiện có **7** JD (5 của DN-02 + JD6, JD7 của DN-06), trong đó 3 JD `APPROVED` còn hạn (hiện trên trang việc làm) và 1 JD `PENDING`; sau khi thêm 2 JD của B, trang việc làm có 4 JD, hàng chờ có 2 JD.

## 4. API

Lỗi theo `ErrorResponse {code, message, errors?}`, message tiếng Việt. Đường dẫn ở service; qua gateway thêm `/api/v1`.

### 4.1 Gateway

Thêm `/api/v1/staff/jobs/**` vào predicate route `company` (giữ `StripPrefix=2`, không thêm route id). `/api/v1/jobs/**` và `/api/v1/staff/companies/**` đã có. Thêm ca vào `CompanyRouteIntegrationTest`.

### 4.2 Quy tắc "ứng viên thấy JD nào" (chốt)

Một JD **hiện với ứng viên** khi và chỉ khi: `status = APPROVED` **và** `deadline >= hôm nay` theo `Asia/Ho_Chi_Minh` (bean `Clock`, `JobRules.today`) **và** công ty của JD có `status = ACTIVE`. Viết đúng một chỗ (một truy vấn repository có join `companies`, hoặc một `Specification` dùng lại được) để `GET /jobs`, chi tiết cho ứng viên và DN-04 sau này cùng dùng. `application-context` của UT-01 giữ nguyên (đã trả đủ dữ liệu để UT-01 tự quyết).

### 4.3 Trang việc làm (công khai)

`CompanySecurityConfig` thêm `permitAll` cho `GET /jobs` và `GET /jobs/{id}` (chỉ đúng hai mẫu này; `/jobs/{id}/match/**` của DN-06 vẫn cần đăng nhập). Token hợp lệ vẫn được đọc nếu có (DN-04 dùng để tính `matchScore`).

**`GET /jobs?page=1&size=20`** → `200` `PageResponse<PublicJobSummary>`. Chỉ JD hiện với ứng viên, sắp `COALESCE(reviewed_at, updated_at)` giảm dần (thời điểm duyệt gần nhất = "mới nhất"), rồi `id` — `COALESCE` là lưới an toàn cho dữ liệu cũ, để thứ tự giống nhau trên PostgreSQL và H2. Phân trang theo quy ước (page từ 1, size ≤ 100, sai → 400). DN-04 sẽ thêm các tham số lọc và `sort` vào chính endpoint này.

`PublicJobSummary`: `id, title, level, type, workMode, city, salaryMin, salaryMax, deadline, publishedAt` (= `reviewedAt`, hoặc `updatedAt` khi `reviewedAt` rỗng), `company {id, name, logoUrl}`. Danh sách lấy công ty bằng join hoặc một truy vấn gom, không N+1.

**`GET /jobs/{id}`** → `200` `PublicJobDetail` nếu JD hiện với ứng viên; ngược lại **404** `NOT_FOUND` (JD chờ duyệt, nháp, đã đóng, quá hạn, công ty bị khoá, id không tồn tại đều cùng một 404).

`PublicJobDetail`: các trường của `PublicJobSummary` + `position, quantity, address, description, requirements, benefits`, `skills [{skillId, required, minScore}]` (sắp như DN-02), `company {id, name, logoUrl, industry, size, city, website, description}`. Không trả `status`, `rejectReason`, `reviewedBy`, `createdBy`. Backend không trả tên kỹ năng (frontend ghép với `GET /skills` như DN-02; trang cho khách của DN-04 sẽ cần xử lý riêng vì `GET /skills` đang yêu cầu đăng nhập).

**Chi tiết JD của HR chuyển đường dẫn** (người thiết kế chọn): `GET /jobs/{id}` của DN-02 (chỉ HR, trả `JobResponse`) chuyển thành **`GET /companies/me/jobs/{id}`** (cùng quyền, cùng hành vi, cùng `JobResponse`), để `GET /jobs/{id}` thuộc về trang việc làm như DN-04 mô tả. Sửa `web/src/services/job.service.ts` (`get`), `DN-02-jobs.md`, `DN-02.http`, OpenAPI. Các endpoint ghi của DN-02 (`POST/PUT /jobs...`) giữ nguyên.

### 4.4 Cán bộ xử lý JD

Mọi endpoint `@PreAuthorize(Roles.STAFF)`; token HR/CANDIDATE/ADMIN → 403. Mỗi thao tác chạy trong một transaction, dùng `@Version` của `jobs` (xung đột → 409 `CONFLICT`, handler đã có), ghi `reviewed_by = principal.userId()`, `reviewed_at = now`, và **một** dòng `company_actions` trong cùng transaction.

**`GET /staff/jobs?status=PENDING&companyId=&page=1&size=20`** → `200` `PageResponse<StaffJobItem>`. `status` **tuỳ chọn** (cập nhật 26/11): một trong 5 trạng thái, bỏ trống = mọi trạng thái (dùng cho tab "JD của công ty"); màn Duyệt JD luôn gửi `status`. `companyId` tuỳ chọn; sai → 400. Sắp: `PENDING` theo `updated_at` **tăng dần** (gửi sớm nhất trước — JD đang `PENDING` không sửa được nên `updated_at` chính là lúc gửi); trạng thái khác và khi bỏ trống `status` theo `updated_at` giảm dần. `StaffJobItem`: `id, title, status, deadline, submittedAt` (= `updatedAt`), `company {id, name}`. Kèm số lượng theo từng trạng thái cho tab là tuỳ người làm (có thể gọi thêm hoặc trả `counts` riêng).

**`GET /staff/jobs/{id}`** → `200` `StaffJobDetail` = `PublicJobDetail` + `status, rejectReason, reviewedAt, reviewedBy {id, fullName}`. Mọi trạng thái, mọi công ty (kể cả công ty bị khoá). Không tồn tại → 404.

**`POST /staff/jobs/{id}/approve`** (không body) → `200` `StaffJobDetail`.
- Chỉ từ `PENDING` → `APPROVED`; trạng thái khác → **409** `INVALID_JOB_STATUS` (tiêu chí 5).
- `deadline < hôm nay` → **409** `JOB_EXPIRED` (mã mới, message "JD đã quá hạn nộp, hãy trả lại để HR sửa hạn nộp") (người thiết kế chọn).
- Xoá `reject_reason`. Ghi `APPROVE_JD`, `reason` NULL.
- Công ty bị khoá vẫn duyệt được; JD chỉ hiện khi công ty được mở khoá (chốt).

**`POST /staff/jobs/{id}/reject`** body `{reason}` → `200` `StaffJobDetail`.
- `reason` bắt buộc, trim, 1–1000 ký tự; thiếu hoặc rỗng → **400** `VALIDATION_ERROR`, `errors.reason` (tiêu chí 2).
- Chỉ từ `PENDING` → `REJECTED`; khác → 409 `INVALID_JOB_STATUS`.
- Ghi `reject_reason = reason`, action `REJECT_JD` kèm lý do. HR thấy lý do trên form DN-02 và gửi lại được (đã có).

**`POST /staff/jobs/{id}/close`** body `{reason}` → `200` `StaffJobDetail`.
- `reason` bắt buộc như trên (người thiết kế chọn: đóng do vi phạm phải có lý do).
- Chỉ từ `APPROVED` → `CLOSED`; khác → 409 `INVALID_JOB_STATUS`. Ghi `CLOSE_JD` kèm lý do. `reject_reason` không đổi.

Mã lỗi mới trong `CompanyErrorCode`: `JOB_EXPIRED` → 409. Thêm `@ApiResponse` cho từng mã.

### 4.5 Khoá / mở khoá công ty

**`POST /staff/companies/{id}/lock`** body `{reason}` → `200` `StaffCompanyDetail`.
1. `reason` bắt buộc, trim, 1–1000 → thiếu 400 `VALIDATION_ERROR` `errors.reason`.
2. Không có công ty → 404. Công ty đang `LOCKED` → 409 `CONFLICT` ("Công ty đã bị khoá").
3. Đặt `companies.status = LOCKED`, ghi `company_actions` `LOCK` kèm lý do.
4. Lấy mọi `user_id` trong `company_hr` của công ty (kể cả HR đang bị khoá riêng), gọi identity `POST /internal/users/company-lock` `{userIds, locked: true}` theo lô ≤ 100.
5. identity tắt / timeout / 5xx → 503 `EXTERNAL_UNAVAILABLE` (message riêng "Không cập nhật được tài khoản HR, vui lòng thử lại."), transaction rollback, không có gì đổi (gọi identity **trước** khi commit). Nếu identity đã nhận một phần lô rồi mới lỗi: gọi bù `locked: false` cho các lô đã gửi, ghi log ERROR nếu gọi bù cũng lỗi. Công ty không có HR → không gọi identity.
6. Đọc công ty bằng khoá dòng (`@Lock(PESSIMISTIC_WRITE)` / `SELECT … FOR UPDATE`) ở đầu transaction để hai lời gọi khoá/mở khoá đồng thời chạy lần lượt (bảng `companies` không có `@Version`), và việc tạo HR mới (DN-01) không lọt vào giữa. Nếu commit DB lỗi **sau khi** identity đã nhận: đăng ký `TransactionSynchronization.afterCompletion` để gọi bù chiều ngược lại khi transaction rollback, ghi log ERROR nếu gọi bù lỗi (người thiết kế chọn). Đây là ngoại lệ có chủ đích so với DN-05 (gọi HTTP ngoài transaction): khoá công ty cần trạng thái công ty và cờ của HR đổi cùng nhau.

**`POST /staff/companies/{id}/unlock`** (không body) → `200` `StaffCompanyDetail`.
- Chỉ từ `LOCKED` → `ACTIVE`; đang `ACTIVE` → 409 `CONFLICT` ("Công ty không bị khoá"). Ghi `UNLOCK`, `reason` NULL. Gọi identity `locked: false` cho mọi HR của công ty; lỗi xử lý như khoá.
- HR đang bị khoá **riêng** (DN-05) vẫn bị khoá sau khi mở khoá công ty (chốt): identity chỉ xoá `company_locked_at`.

`StaffCompanyDetail` thêm `lock` (NULL khi `ACTIVE`): `{lockedAt, reason, lockedBy {id, fullName}}` lấy từ dòng `LOCK` mới nhất. `GET /staff/companies/{id}` trả cùng trường này (cho banner).

Khi công ty bị khoá:
- HR không đăng nhập được (identity), token đang còn hạn bị chặn ở identity, company-service và recruitment-service (mục 4.7).
- JD `APPROVED` biến khỏi `GET /jobs`, `GET /jobs/{id}` → 404; `application-context` trả `companyLocked = true` (UT-01 đã chặn ứng tuyển). JD giữ nguyên trạng thái, mở khoá là hiện lại (tiêu chí 3).
- Tạo HR mới cho công ty bị khoá đã bị chặn 403 `COMPANY_LOCKED` từ DN-01; khoá / mở khoá riêng một HR của công ty bị khoá vẫn làm được (chỉ đổi cờ khoá riêng).

### 4.6 Lịch sử thao tác

**`GET /staff/companies/{id}/history?page=1&size=20`** → `200` `PageResponse<CompanyActionItem>`, mới nhất trước (`created_at` giảm dần, rồi `id`). Công ty không tồn tại → 404.

`CompanyActionItem`: `id, action, createdAt, reason, staff {id, fullName}, job {id, title} | null`. Tên cán bộ tra bằng `IdentityClient.lookupUsers` (đã có, theo lô ≤ 100) cho các `staff_id` của trang; identity lỗi → vẫn trả 200 với `fullName = null` (người thiết kế chọn: lịch sử không được hỏng vì identity, frontend hiện id rút gọn). Tiêu đề JD lấy từ `jobs` bằng một truy vấn cho cả trang.

### 4.7 identity-service

**`POST /internal/users/company-lock`** (bảo vệ bởi `InternalKeyFilter` có sẵn) body `{userIds: [UUID] (1–100), locked: boolean}` → `200` `List<InternalUserStatusResponse>`. Chỉ áp dụng cho user role `HR`; id không phải HR hoặc không tồn tại bị bỏ qua (idempotent: khoá lại HR đã khoá theo công ty giữ nguyên `company_locked_at` cũ). Sai body → 400.

Các điểm hiện dùng `isAdminLocked()` để **chặn** chuyển sang `isLoginBlocked()`:

| Điểm | Hành vi khi bị khoá theo công ty |
| --- | --- |
| `LoginServiceImpl` (đăng nhập email/mật khẩu) | 403 `ACCOUNT_DISABLED`, message "Công ty của bạn đang bị khoá, liên hệ cán bộ tuyển dụng" (khoá riêng giữ message cũ). Cập nhật 26/11: theo `b66749e` trên `test`, khoá chỉ được báo **sau khi** mật khẩu đúng (chống dò email bị khoá); kiểm tra khoá theo công ty đặt cùng chỗ với khoá riêng |
| `SsoUserServiceImpl` | như trên (HR không đăng nhập SSO nhưng giữ đồng bộ) |
| `IdentityJwtAuthenticationFilter` | 401 `ACCOUNT_DISABLED` |
| `PasswordResetServiceImpl` (quên / đặt lại mật khẩu) | chặn như khoá riêng |
| `InternalUserStatusResponse` (`lookup`) | `status = LOCKED`; thêm field `companyLocked` (boolean) |

`unlockHr` (DN-05) không đụng `company_locked_at`. Ghi các mã và hành vi này vào `NT-01-authentication.md`/`DN-05` nếu bảng lỗi ở đó liệt kê.

company-service `HrLockedFilter`: thêm điều kiện chặn 401 `ACCOUNT_DISABLED` khi HR thuộc công ty `LOCKED` (một truy vấn join `company_hr`–`companies`, hoặc đọc kèm khi đã tra `company_hr`), message "Công ty của bạn đang bị khoá, liên hệ cán bộ tuyển dụng". Hệ quả có chủ đích (theo tiêu chí 3: HR của công ty bị khoá không vào được hệ thống): HR của công ty `LOCKED` mất **mọi** quyền truy cập company-service, kể cả đọc. Các nhánh 403 `COMPANY_LOCKED` của DN-01/DN-02 vẫn giữ trong code làm lớp phòng thủ thứ hai nhưng HR không còn chạm tới; sửa các test đang khẳng định "công ty LOCKED: GET 200, ghi 403" (`JobApiTest`, `JobLifecycleApiTest`, `MyCompanyApiTest`, `CompanyLogoApiTest`) thành 401 `ACCOUNT_DISABLED`, và sửa câu tương ứng trong `DN-01-company-hr.md`, `DN-02-jobs.md`. Endpoint công khai `GET /jobs` cũng trả 401 với token của HR này (bộ lọc chạy trước) — chấp nhận, vì HR không dùng trang việc làm. Token phải đổi mật khẩu nhận 403 ở `GET /jobs` là hành vi sẵn có của common-core.

`StaffHrAccount` (DN-05) giữ nguyên nghĩa `status` = **khoá riêng** (`LOCKED` khi và chỉ khi `lockedAt != null`), và thêm field `companyLocked` (boolean). company-service **không** chép thẳng `lookup.status` vào `StaffHrAccount.status` nữa: `IdentityUserStatus` thêm `companyLocked`, `StaffHrAccountMapping` tính `status` từ `lockedAt` (người thiết kế chọn). Bảng HR (DN-05) thêm nhãn "Khoá theo công ty" khi `companyLocked`; nút Khoá / Mở khoá **riêng** vẫn hiện theo `status` riêng và vẫn dùng được (khoá riêng một HR trong lúc công ty đang khoá, để khi mở khoá công ty HR đó vẫn bị khoá). Cập nhật `DN-05-company-hr-ui.md` và OpenAPI.

### 4.8 Job tự đóng JD quá hạn

Bật `@EnableScheduling` trong company-service (chưa có). Một bean `JobAutoCloseScheduler` với `@Scheduled(cron = "0 5 0 * * *", zone = "Asia/Ho_Chi_Minh")`: một câu `UPDATE` đóng mọi JD `APPROVED` có `deadline < JobRules.today(clock)` → `CLOSED`, tăng `version`, cập nhật `updated_at`; không đổi `reviewed_*`, không ghi `company_actions`. Ghi log INFO số JD đã đóng. Cờ `app.jobs.auto-close.enabled` (mặc định `true`; test đặt `false` và gọi thẳng phương thức để kiểm). Chạy lại nhiều lần không sai (idempotent). Một instance nên không cần khoá phân tán (người thiết kế chọn, `@Scheduled` theo boring-by-default).

## 5. Frontend

Đọc `web/AGENTS.md` và `web/node_modules/next/dist/docs/` trước khi viết code Next. react-hook-form + zod, TanStack Query (khoá cache kèm user id), `api` sẵn có, `@base-ui/react` (Dialog, Tabs, Menu), cùng phong cách các màn DN-02/DN-05. **Cập nhật 26/11:** HR và cán bộ dùng khung `ManagerShell` (lớp `admin-theme`) theo `2026-11-24-dn-ui-refresh-design.md` §3: tiêu đề trang `text-xl font-semibold text-[#18233a]`, nút chính cùng hàng bên phải, các trang **không** đặt `max-w-*` riêng; màu chỉ dùng token (`primary` đã là đỏ trong khung). Mục "Duyệt JD" đã có trong nhóm **Duyệt ▾** của thanh cán bộ. Mockup đã chốt nằm ở `.superpowers/brainstorm/1505-1795060628/content/` (không commit).

### 5.1 Khối chi tiết JD dùng chung

Component `JobPublicView` (ví dụ `web/src/app/components/job/job-public-view.tsx`) hiển thị `PublicJobDetail`: logo + tên + ngành/quy mô/thành phố công ty; tiêu đề; chip cấp độ, loại hình, hình thức, lương, số lượng, hạn nộp; địa chỉ; kỹ năng Bắt buộc / Ưu tiên kèm mức và tên bậc (ghép tên với `GET /skills`); Mô tả, Yêu cầu khác, Quyền lợi. Có chỗ để chèn thêm (slot/children). Dùng ở màn Duyệt JD (cập nhật 26/11: trang chi tiết của ứng viên thuộc DN-04, DN-07 không đụng; DN-04 có thể dùng lại component này khi hiển thị các trường mở rộng).

### 5.2 Màn "Duyệt JD" `/staff/jobs` (chốt: bố cục A)

- Tiêu đề "Duyệt JD", ô chọn **Công ty** (Tất cả + danh sách công ty, dùng `GET /staff/companies`), tab **Chờ duyệt (n) / Đang tuyển / Bị trả lại / Đã đóng**.
- Trái: hàng chờ dạng danh sách (tiêu đề, công ty, thời điểm gửi), phân trang. Phải: `JobPublicView` của JD đang chọn (`GET /staff/jobs/{id}`), dưới là thanh nút.
- Tab Chờ duyệt: nút **Trả lại…** (hộp nhập lý do bắt buộc, tối đa 1000) và **Duyệt**. Duyệt / trả lại xong: thông báo, JD rời hàng chờ, tự chọn JD kế tiếp.
- Tab Đang tuyển: nút **Đóng JD…** (hộp lý do bắt buộc).
- Tab Bị trả lại / Đã đóng: chỉ xem, hiện lý do trả lại và người, thời điểm xử lý gần nhất.
- JD thuộc công ty bị khoá có nhãn "Công ty đang bị khoá" ở khối chi tiết.
- Lỗi 409 (JD đã được người khác xử lý, quá hạn) → thông báo và tải lại hàng chờ. Màn hẹp: danh sách trên, chi tiết dưới. Mục "Duyệt JD" trong menu đã có (NT-02).

### 5.3 Chi tiết doanh nghiệp `/staff/companies/{id}` (chốt theo mockup)

- Trang dùng `CompanyHeader` có sẵn (DN-05 làm mới). Vùng nút: **+ Thêm HR** và **Khoá công ty** (đang hoạt động) / **Mở khoá** (đang khoá). Tab đặt qua prop `tabs` của `CompanyHeader`.
- Công ty đang khoá: banner đỏ "Công ty đang bị khoá từ <thời điểm> bởi <cán bộ>. Lý do: … HR không đăng nhập được; JD đang tuyển bị ẩn khỏi trang việc làm."
- Ba tab: **Tài khoản HR (n)** (bảng hiện tại của DN-05, thêm nhãn "Khoá theo công ty"), **JD của công ty** (cập nhật 26/11: bảng Tiêu đề / Trạng thái (nhãn màu) / Hạn nộp / Cập nhật, bộ lọc trạng thái Tất cả + 5 trạng thái, phân trang, dùng `GET /staff/jobs?companyId=<id>&status=`; bấm một dòng mở `/staff/jobs?companyId=<id>&status=<trạng thái JD>&jobId=<id>` để xem và xử lý ở màn Duyệt JD) và **Lịch sử thao tác** (bảng Thời điểm / Cán bộ / Hành động có nhãn màu / JD / Lý do, phân trang). Tab đang chọn lưu trên URL (`?tab=hr|jobs|history`).
- Hộp khoá: nêu hậu quả, ô lý do bắt buộc. Mở khoá: hộp xác nhận, không có lý do.

### 5.4 Trang việc làm của ứng viên

Cập nhật 26/11: **không đổi** trang `/candidate/jobs` và `/candidate/jobs/{id}` (thuộc DN-04, đã merge). Khi demo tiêu chí 1 và 3, đặt `NEXT_PUBLIC_JOB_API_PATH=/jobs` và `NEXT_PUBLIC_JOB_PAGE_PATH=/candidate/jobs` trong `web/.env.local`; `candidate-job.service.ts` đọc `GET /jobs` (tham số `size=100`) và `GET /jobs/{id}` của mục 4.3. Ghi cấu hình này vào `.env.example` (dạng chú thích) và tài liệu DN-07. `GET /jobs` phải chấp nhận `size=100` (đúng giới hạn tối đa).

### 5.5 HR

Chỉ đổi `job.service.get` sang `/companies/me/jobs/{id}` (mục 4.3). Form DN-02 đã hiện lý do trả lại và cho gửi lại.

## 6. Cấu hình

- company-service: `app.jobs.auto-close.enabled` (mặc định `true`). Không có biến môi trường bắt buộc mới (`INTERNAL_SERVICE_KEY`, `IDENTITY_SERVICE_URL` đã có).
- Ghi `.env.example` và mục Cấu hình của `docs/architecture/service-conventions.md` nếu thêm biến.

## 7. Kiểm thử

Test backend không cần Postgres (H2 + Flyway như các service hiện có). Giả lập identity bằng mock bean / `MockRestServiceServer`. Test của recruitment-service chạy theo `docs/api/UT-02-application-pipeline.md` (Testcontainers hoặc DB kiểm thử riêng); DN-07 không sửa recruitment.

| # | Ca | Ở đâu | Tiêu chí |
| --- | --- | --- | --- |
| 1 | `GET /jobs`: chỉ JD `APPROVED`, còn hạn (ranh giới hôm nay theo giờ VN), công ty `ACTIVE`; JD `PENDING`/`DRAFT`/`REJECTED`/`CLOSED`/quá hạn/công ty khoá không có; gọi không token → 200; sắp `reviewedAt` giảm dần; phân trang | company | 1 |
| 2 | `GET /jobs/{id}`: JD hiện → 200 không có `status`/`rejectReason`; mọi trường hợp không hiện → cùng 404 | company | 1 |
| 3 | Duyệt JD `PENDING` → 200, `APPROVED`, `reviewedBy/At`, có dòng `APPROVE_JD`; sau đó xuất hiện ở `GET /jobs` | company | 1, 4 |
| 4 | Duyệt JD `DRAFT`/`REJECTED`/`APPROVED`/`CLOSED` → 409 `INVALID_JOB_STATUS`; duyệt JD `PENDING` đã quá hạn → 409 `JOB_EXPIRED`; không ghi lịch sử | company | 5 |
| 5 | Trả lại thiếu / rỗng / > 1000 lý do → 400 `errors.reason`; hợp lệ → `REJECTED`, `rejectReason`; HR `GET /companies/me/jobs/{id}` thấy lý do, sửa và `submit` → `PENDING` | company | 2 |
| 6 | Đóng JD `APPROVED` có lý do → `CLOSED`, dòng `CLOSE_JD`; đóng JD khác → 409; thiếu lý do → 400 | company | 4 |
| 7 | Khoá công ty A: `LOCKED`, dòng `LOCK` kèm lý do, identity nhận đúng danh sách HR (lô ≤ 100) với `locked: true`; JD của A biến khỏi `GET /jobs`, JD của B còn; `GET /jobs/{idA}` → 404; HR của A gọi API company-service → 401 `ACCOUNT_DISABLED`; mở khoá → `ACTIVE`, `UNLOCK`, identity `locked: false`, JD hiện lại | company | 3, 4 |
| 8 | Khoá khi đã khoá / mở khoá khi đang hoạt động → 409; thiếu lý do khoá → 400; identity lỗi → 503 và công ty không đổi, không có dòng lịch sử | company | 3 |
| 9 | Lịch sử: đúng thứ tự mới nhất trước, có `staff.fullName`, `job.title`, `reason`; identity lỗi → 200 với `fullName` null; phân trang | company | 4 |
| 10 | Token HR/CANDIDATE/ADMIN gọi `/staff/jobs/**`, lock/unlock, history → 403 | company | — |
| 11 | Xung đột `@Version` khi hai cán bộ cùng duyệt → một bên 409 | company | — |
| 12 | Job tự đóng: JD `APPROVED` hạn hôm qua → `CLOSED`; hạn hôm nay giữ nguyên; JD `PENDING` quá hạn giữ nguyên; chạy hai lần không lỗi | company | — |
| 13 | identity `company-lock`: thiếu / sai khoá nội bộ → 401; khoá → HR đăng nhập 403 `ACCOUNT_DISABLED`, token cũ 401, quên mật khẩu bị chặn, `lookup` `LOCKED` + `companyLocked`; mở khoá → đăng nhập lại được; HR đang khoá riêng vẫn bị khoá sau khi mở khoá công ty; mở khoá riêng không xoá cờ công ty; id không phải HR bị bỏ qua | identity | 3 |
| 14 | HR detail `GET /companies/me/jobs/{id}` giữ đúng hành vi cũ của `GET /jobs/{id}` (404 công ty khác, quyền HR) | company | — |
| 15 | Gateway chuyển `/api/v1/staff/jobs/**` tới company-service | gateway | — |
| 16 | Seeder: thêm 2 JD của Công ty B, đặt `reviewed_at` cho JD đã xử lý, chạy hai lần không trùng; migration V6 backfill `reviewed_at` | company | 6 |
| 17 | Khoá đồng thời hai lần → một lần thành công, một lần 409, chỉ một dòng `LOCK`; commit lỗi sau khi identity đã nhận → identity được gọi bù | company | 3 |
| 18 | `StaffHrAccount`: HR khoá theo công ty có `companyLocked = true`, `status` vẫn theo khoá riêng | company | — |

Frontend: `npm run type-check`, `npm run lint`, `npm run build`. `web/` có Playwright e2e (`npm run test:e2e`, `web/e2e/*.spec.ts`, cần stack chạy thật); không bắt buộc thêm e2e cho DN-07, nếu thêm thì một kịch bản duyệt JD → hiện ở trang việc làm.

Kiểm tra tay toàn luồng (dùng cho ảnh màn hình PR): Postgres, MailHog, mock, identity (dev), profile, company (dev), recruitment, gateway, web. staff1 mở `/staff/jobs` → thấy JD chờ của A và B → mở "Fresher ReactJS", trả lại không nhập lý do (bị chặn), nhập lý do → trả lại → hr1 thấy banner lý do trên `/hr/jobs/{id}`, sửa, gửi lại → staff1 duyệt → candidate1 thấy JD trên `/candidate/jobs` (trước khi duyệt thì không), mở chi tiết, ứng tuyển → staff1 khoá Công ty A (nhập lý do) → JD của A biến khỏi `/candidate/jobs` (JD của B còn), hr1 đăng nhập bị từ chối, phiên hr1 đang mở bị đăng xuất ở request kế tiếp → tab Lịch sử có đủ người, thời điểm, lý do → mở khoá → JD hiện lại, hr1 đăng nhập được → đóng một JD đang tuyển từ tab Đang tuyển. Ca job tự đóng kiểm bằng test (hoặc gọi tay phương thức qua test).

## 8. Tài liệu bàn giao

- `docs/api/company-openapi.yaml`: endpoint mục 4.3–4.6, đổi đường dẫn chi tiết JD của HR, mã `JOB_EXPIRED`, `StaffCompanyDetail.lock`.
- `docs/api/identity-openapi.yaml`: `POST /internal/users/company-lock`, field `companyLocked` của `lookup`, hành vi `ACCOUNT_DISABLED` khi công ty bị khoá.
- `docs/api/DN-07-job-review.md` (mới, theo mẫu `DN-02-jobs.md`: mục tiêu, DB, API, vòng đời đầy đủ, khoá công ty, cách chạy, dữ liệu mẫu, ca kiểm thử, kết quả xác minh; không có mục ảnh màn hình — ảnh đính kèm PR) và `docs/api/DN-07.http`.
- Cập nhật `docs/database/erd.md`, `docs/architecture/service-conventions.md` (`HrLockedFilter` chặn cả công ty khoá, endpoint công khai `GET /jobs`, lịch chạy định kỳ), `docs/api/DN-02-jobs.md` + `DN-02.http` (đường dẫn chi tiết HR; bỏ ghi chú "duyệt thuộc DN-07, dùng SQL tạm"), `docs/api/DN-05-company-hr-ui.md` (khoá công ty, tab lịch sử), `docs/api/UT-01-applications.md` (trang việc làm dùng dữ liệu thật), README nếu lệnh chạy đổi.

## 9. Để người làm tự quyết

- Tên lớp, chia service/impl, tên DTO, tên file frontend.
- Cách viết quy tắc hiển thị (truy vấn JPQL có join hay `Specification`), miễn chỉ một chỗ và DN-04 dùng lại được.
- Cách lấy số lượng theo trạng thái cho tab của màn Duyệt JD.
- Nội dung chữ giao diện và thông báo (tiếng Việt, rõ ràng), chi tiết giao diện trong khuôn mockup đã chốt.
