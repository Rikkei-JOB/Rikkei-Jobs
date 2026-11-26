# DN-07 · Cán bộ duyệt JD, quản lý doanh nghiệp

| Mục | Giá trị |
| --- | --- |
| Mã task | DN-07 |
| Service | `company-service` (cổng 8085) qua `api-gateway`; `identity-service` (cổng 8081) thêm cờ khoá theo công ty và API nội bộ `POST /internal/users/company-lock` |
| Frontend | `/staff/jobs` (Duyệt JD), `/staff/companies/[id]` (khoá / mở khoá công ty, tab "JD của công ty", "Lịch sử thao tác") trong `web/src/app/(pages)/(app)/staff`; khối chi tiết dùng chung `web/src/app/components/job/job-public-view.tsx` |
| Phụ thuộc | DN-02 (bảng `jobs`, vòng đời của HR), DN-05 (màn doanh nghiệp, khoá riêng HR, `HrLockedFilter`), DN-06 (điểm khớp, `match_scores`), DN-04 (trang `/candidate/jobs` đọc được hợp đồng `GET /jobs`) |
| OpenAPI | [company-openapi.yaml](company-openapi.yaml) (tag `public-job`, `staff-job`, `staff`), [identity-openapi.yaml](identity-openapi.yaml) (tag `internal`) |
| Request mẫu | [DN-07.http](DN-07.http) |
| Thiết kế | [2026-11-19-dn-07-job-review-company-lock-design.md](../superpowers/specs/2026-11-19-dn-07-job-review-company-lock-design.md) |

## 1. Mục tiêu

- **Cán bộ (`STAFF`)** kiểm soát JD trước khi ứng viên thấy: hàng chờ, duyệt, trả lại (bắt buộc lý do), đóng JD đang tuyển (bắt buộc lý do).
- **Khoá / mở khoá công ty** vi phạm: HR của công ty bị khoá không đăng nhập được (kể cả token đang còn hạn), JD của công ty biến khỏi trang việc làm; mở khoá là mọi thứ trở lại.
- **Lịch sử thao tác** của cán bộ trên từng công ty: người làm, thời điểm, hành động, JD, lý do.
- **Trang việc làm công khai** `GET /jobs`, `GET /jobs/{id}`: ứng viên (và khách) chỉ thấy JD đã duyệt, còn hạn, của công ty không bị khoá.
- **Job hằng ngày** tự đóng JD đang tuyển đã quá hạn nộp.

Tiêu chí nghiệm thu:

1. JD `PENDING` không hiện ở trang việc làm; duyệt xong thì hiện.
2. Trả lại không có lý do → 400; HR thấy lý do, sửa và gửi lại được.
3. Khoá công ty A → JD của A biến khỏi trang việc làm, hr1 không đăng nhập được; mở khoá → hiện lại.
4. Lịch sử ghi người làm, thời điểm, lý do.
5. Duyệt JD đang `DRAFT` → 409.
6. PR vào `test` có cách chạy, dữ liệu mẫu, ảnh màn hình; OpenAPI cập nhật endpoint mới; làm đủ DB, BE, FE.

Ngoài phạm vi: bộ lọc / sắp theo điểm khớp / `matchScore` / `job_views` của trang việc làm (DN-04 sẽ **thêm vào** chính `GET /jobs` và `JobPublicView`); email báo HR khi JD được duyệt / trả lại / đóng; ADMIN duyệt JD hay khoá công ty; ghi lịch sử cho việc hệ thống tự đóng JD; trạng thái "Tạm dừng". DN-07 **không sửa** trang `/candidate/jobs` (thuộc DN-04), chỉ demo bằng cấu hình (mục 6).

## 2. Cơ sở dữ liệu

Sơ đồ ở [erd.md](../database/erd.md). SQL chạy được trên cả PostgreSQL và H2 `MODE=PostgreSQL`, không ghi tên schema.

### company-service — `V6__job_review.sql`

(V5 là `V5__drop_english_skill.sql` của HS-04.)

`company_actions` — lịch sử thao tác của cán bộ, chỉ thêm, không sửa, không xoá:

| Cột | Kiểu | Ghi chú |
| --- | --- | --- |
| `id` | UUID, PK | |
| `company_id` | UUID, NOT NULL, FK → `companies.id` | chỉ mục `idx_company_actions_company_created (company_id, created_at)` cho trang lịch sử |
| `action` | VARCHAR(12), NOT NULL | CHECK `LOCK`, `UNLOCK`, `APPROVE_JD`, `REJECT_JD`, `CLOSE_JD` |
| `target_id` | UUID | id JD với `*_JD`; NULL với `LOCK`/`UNLOCK`. Không FK (giữ lịch sử kể cả khi sau này có xoá JD) |
| `reason` | VARCHAR(1000) | bắt buộc ở tầng API với `LOCK`, `REJECT_JD`, `CLOSE_JD` |
| `staff_id` | UUID, NOT NULL | userId cán bộ lấy từ JWT |
| `created_at` | TIMESTAMPTZ, NOT NULL, mặc định `CURRENT_TIMESTAMP` | |

Cùng migration:

- `CREATE INDEX idx_jobs_status_deadline ON jobs (status, deadline)` cho `GET /jobs` và job tự đóng (DN-04 không tạo lại).
- Backfill `UPDATE jobs SET reviewed_at = updated_at WHERE status IN ('APPROVED','REJECTED','CLOSED') AND reviewed_at IS NULL;` — JD seed và JD duyệt tạm bằng SQL trước đây chưa có `reviewed_at`. `reviewed_by` để NULL (không bịa người duyệt).

`companies` và `jobs` không đổi cột. `companies.status` vẫn là nguồn sự thật của trạng thái công ty; "khoá lúc nào, bởi ai, lý do" lấy từ dòng `LOCK` mới nhất của `company_actions`.

### identity-service — `V3__company_lock.sql`

`ALTER TABLE users ADD COLUMN company_locked_at TIMESTAMP WITH TIME ZONE;` — khác NULL nghĩa là HR đang bị **khoá theo công ty**. Tách khỏi `locked_at`/`locked_reason` của DN-05 (**khoá riêng**). `User.isLoginBlocked()` = khoá riêng hoặc khoá theo công ty.

### Sửa DB dev đã chạy số cũ `V5__job_review.sql`

Bản nháp đầu của nhánh này đặt migration là `V5__job_review.sql`; sau khi merge `test` (có `V5__drop_english_skill.sql`) đổi thành `V6`. Ai đã chạy company-service của nhánh này trước đó sẽ bị Flyway báo lệch phiên bản. Kiểm tra `SELECT version, script FROM company.flyway_schema_history;` — **chỉ khi** có dòng version `5` script `V5__job_review.sql`, tắt company-service rồi chạy:

```sql
DELETE FROM company.flyway_schema_history WHERE version = '5' AND script = 'V5__job_review.sql';
DROP TABLE IF EXISTS company.company_actions;
DROP INDEX IF EXISTS company.idx_jobs_status_deadline;
```

Khởi động lại company-service: Flyway chạy `V5__drop_english_skill` rồi `V6__job_review`.

## 3. API

Qua gateway `http://localhost:8080/api/v1` (route `company` thêm `/api/v1/staff/jobs/**`, `StripPrefix=2`; `/api/v1/jobs/**`, `/api/v1/staff/companies/**` đã có). Thành công trả thẳng DTO, lỗi là `{code, message, errors?}`. Đặc tả đầy đủ: [company-openapi.yaml](company-openapi.yaml).

| Endpoint | Vai trò | Thành công | Lỗi chính |
| --- | --- | --- | --- |
| `GET /jobs?page=1&size=20` | công khai | 200 `PageResponse<PublicJobSummary>` | 400 `BAD_REQUEST` (page/size) |
| `GET /jobs/{id}` | công khai | 200 `PublicJobDetail` | 404 `NOT_FOUND` (mọi trường hợp không hiện) |
| `GET /companies/me/jobs/{id}` | `HR` | 200 `JobResponse` (chi tiết JD của HR, trước DN-07 là `GET /jobs/{id}`) | 404 |
| `GET /staff/jobs?status=&companyId=&page=&size=` | `STAFF` | 200 `PageResponse<StaffJobItem>` | 400 |
| `GET /staff/jobs/{id}` | `STAFF` | 200 `StaffJobDetail` | 404 |
| `POST /staff/jobs/{id}/approve` | `STAFF` | 200 `StaffJobDetail`, `APPROVED` | 404, 409 `INVALID_JOB_STATUS` / `JOB_EXPIRED` / `CONFLICT` |
| `POST /staff/jobs/{id}/reject` body `{reason}` | `STAFF` | 200 `StaffJobDetail`, `REJECTED` | 400 `VALIDATION_ERROR` (`errors.reason`), 404, 409 |
| `POST /staff/jobs/{id}/close` body `{reason}` | `STAFF` | 200 `StaffJobDetail`, `CLOSED` | 400, 404, 409 |
| `POST /staff/companies/{id}/lock` body `{reason}` | `STAFF` | 200 `StaffCompanyDetail` (kèm `lock`) | 400, 404, 409 `CONFLICT`, 503 `EXTERNAL_UNAVAILABLE` |
| `POST /staff/companies/{id}/unlock` | `STAFF` | 200 `StaffCompanyDetail` (`lock` null) | 404, 409 `CONFLICT`, 503 |
| `GET /staff/companies/{id}/history?page=&size=` | `STAFF` | 200 `PageResponse<CompanyActionItem>` | 400, 404 |

Mọi endpoint cần token còn có 401 `UNAUTHORIZED`, 403 `FORBIDDEN` (sai vai trò: token HR/CANDIDATE/ADMIN gọi API cán bộ), 403 `MUST_CHANGE_PASSWORD`. Id trên đường dẫn không phải UUID → 400 `BAD_REQUEST`.

### 3.1 Quy tắc "ứng viên thấy JD nào"

Một JD hiện với ứng viên khi và chỉ khi `status = APPROVED` **và** `deadline >= hôm nay` theo `Asia/Ho_Chi_Minh` (bean `Clock`, `JobRules.today`) **và** công ty `ACTIVE`. Viết đúng một chỗ: hằng `VISIBLE_TO_CANDIDATES` trong `JobRepository` (một truy vấn join `companies`), dùng cho `findVisibleToCandidates` (danh sách) và `findVisibleToCandidatesById` (chi tiết); DN-04 dùng lại. JD có hạn nộp đúng hôm nay (giờ VN) vẫn hiện, hôm qua thì ẩn.

### 3.2 Trang việc làm công khai

- `CompanySecurityConfig` mở `permitAll` cho `GET /jobs` và `GET /jobs/*` (một đoạn), nên `/jobs/{id}/match/**` (DN-06) vẫn cần đăng nhập. Token thiếu hoặc sai vẫn 200; token hợp lệ vẫn đi qua bộ lọc chung (token `mustChangePassword` → 403, HR của công ty bị khoá → 401).
- `GET /jobs`: sắp `COALESCE(reviewed_at, updated_at)` giảm dần rồi `id` (mới duyệt trước); `size` tối đa 100 (trang ứng viên gọi `size=100`). Công ty lấy bằng một truy vấn gom cho cả trang.
- `PublicJobSummary`: `id, title, level, type, workMode, city, salaryMin, salaryMax, deadline, publishedAt` (= `reviewedAt`, thiếu thì `updatedAt`), `company {id, name, logoUrl}`.
- `PublicJobDetail`: các trường trên + `position, quantity, address, description, requirements, benefits`, `skills [{skillId, required, minScore}]` (bắt buộc trước, rồi `minScore` giảm dần; frontend ghép tên với `GET /skills`), `company {id, name, logoUrl, industry, size, city, website, description}`. Không có `status`, `rejectReason`, `reviewedBy`, `createdBy`.
- `GET /jobs/{id}` với JD chờ duyệt, nháp, bị trả lại, đã đóng, quá hạn, của công ty bị khoá, hoặc id không tồn tại → cùng một 404 "Không tìm thấy JD".

### 3.3 Cán bộ xử lý JD

- `GET /staff/jobs`: `status` tuỳ chọn (bỏ trống = mọi trạng thái, dùng cho tab "JD của công ty"), `companyId` tuỳ chọn. `PENDING` sắp `updated_at` **tăng dần** (gửi sớm nhất trước — JD đang `PENDING` không sửa được nên `updated_at` là lúc gửi); trạng thái khác hoặc bỏ trống sắp giảm dần. `StaffJobItem`: `id, title, status, deadline, submittedAt` (= `updatedAt`), `company {id, name}`. Màn Duyệt JD lấy số "Chờ duyệt (n)" từ `total` của trang `status=PENDING`.
- `StaffJobDetail`: `{job: PublicJobDetail, status, rejectReason, reviewedAt, reviewedBy {id, fullName} | null, companyLocked}`. Mọi trạng thái, mọi công ty (kể cả công ty bị khoá). Tên cán bộ tra qua identity `lookup`; identity lỗi thì `fullName = null`, chi tiết vẫn 200.
- Mỗi thao tác chạy trong một transaction: kiểm tra trạng thái, đổi JD, ghi `reviewed_by = cán bộ`, `reviewed_at = bây giờ`, và **một** dòng `company_actions`. Lỗi trạng thái ném trước khi ghi nên không có dòng lịch sử. `saveAndFlush` để xung đột `@Version` (hai cán bộ cùng xử lý) lộ ra ngay → 409 `CONFLICT`.

| Thao tác | Từ | Sang | Lý do | Lịch sử | Lỗi riêng |
| --- | --- | --- | --- | --- | --- |
| `approve` | `PENDING` | `APPROVED`, xoá `rejectReason` | không | `APPROVE_JD` | hạn nộp trước hôm nay → 409 `JOB_EXPIRED` "JD đã quá hạn nộp, hãy trả lại để HR sửa hạn nộp" |
| `reject` | `PENDING` | `REJECTED`, `rejectReason = lý do` | bắt buộc | `REJECT_JD` + lý do | |
| `close` | `APPROVED` | `CLOSED` (`rejectReason` không đổi) | bắt buộc | `CLOSE_JD` + lý do | |

Trạng thái khác → 409 `INVALID_JOB_STATUS`. Lý do: bắt buộc, cắt khoảng trắng hai đầu, 1–1000 ký tự tính sau khi cắt; thiếu, rỗng hoặc chỉ có khoảng trắng → 400 `VALIDATION_ERROR` `errors.reason` "Vui lòng nhập lý do"; dài hơn → "Lý do tối đa 1000 ký tự". JD của công ty bị khoá vẫn duyệt được; nó chỉ hiện ở trang việc làm khi công ty được mở khoá.

### 3.4 Mã lỗi mới (`CompanyErrorCode`)

| Mã | HTTP | Khi nào |
| --- | --- | --- |
| `JOB_EXPIRED` | 409 | cán bộ duyệt JD `PENDING` có hạn nộp trước hôm nay (giờ VN) |

`EXTERNAL_UNAVAILABLE` (503) của khoá / mở khoá công ty có message riêng "Không cập nhật được tài khoản HR, vui lòng thử lại.".

## 4. Vòng đời JD đầy đủ

```text
DRAFT ──HR submit──▶ PENDING ──cán bộ duyệt──▶ APPROVED ──HR close / cán bộ close (lý do)──▶ CLOSED
                       ▲   └──cán bộ trả lại (lý do)──▶ REJECTED                    ▲
                       │                                   │                         │
                       └────────── HR sửa rồi submit ──────┘                         │
APPROVED ──HR sửa (qua kiểm tra gửi duyệt)──▶ PENDING                                │
APPROVED có deadline < hôm nay ──job tự đóng 00:05 hằng ngày──────────────────────────┘
```

| Trạng thái | HR (DN-02) | Cán bộ (DN-07) | Ứng viên thấy |
| --- | --- | --- | --- |
| `DRAFT` | sửa, gửi duyệt | 409 với mọi thao tác | không |
| `PENDING` | chỉ xem | duyệt, trả lại | không |
| `APPROVED` | sửa (về `PENDING`), đóng | đóng (lý do) | có, nếu còn hạn và công ty `ACTIVE` |
| `REJECTED` | sửa, gửi duyệt lại (xoá lý do) | 409 | không |
| `CLOSED` | chỉ xem | 409 | không |

**Job tự đóng** (`JobAutoCloseScheduler`, `@Scheduled(cron = "0 5 0 * * *", zone = "Asia/Ho_Chi_Minh")`, bật `@EnableScheduling` ở `SchedulingConfig`): một câu `UPDATE` đóng mọi JD `APPROVED` có `deadline < JobRules.today(clock)`, tăng `version`, đặt `updated_at`; không đổi `reviewed_*`, không ghi `company_actions`; ghi log INFO số JD đã đóng; chạy lại nhiều lần không sai. Cờ `app.jobs.auto-close.enabled` (biến `JOB_AUTO_CLOSE_ENABLED`, mặc định `true`). Một instance, không khoá phân tán.

## 5. Khoá công ty

### 5.1 Luồng khoá / mở khoá (company-service)

```text
STAFF ──POST /staff/companies/{id}/lock {reason}──▶ company-service
  kiểm lý do (400) ─▶ transaction:
    SELECT … FOR UPDATE công ty (404; đã LOCKED → 409)
    companies.status = LOCKED, ghi company_actions LOCK + lý do (flush)
    lấy mọi user_id trong company_hr (kể cả HR đang bị khoá riêng), sắp theo id
    ──POST /internal/users/company-lock {userIds ≤ 100, locked: true}──▶ identity   (từng lô, TRƯỚC khi commit)
        lô k lỗi (tắt, timeout, 4xx, 5xx) → gọi bù locked=false cho mọi lô đã gửi (gồm lô k) → 503, rollback
    commit (commit lỗi sau khi identity đã nhận → afterCompletion gọi bù chiều ngược)
  trả StaffCompanyDetail (tra tên cán bộ sau khi commit)
```

- Công ty không có HR → không gọi identity.
- Mở khoá giống hệt, chiều ngược (`ACTIVE`, `UNLOCK` không lý do, `locked: false`); đang `ACTIVE` → 409 "Công ty không bị khoá".
- Đây là **ngoại lệ có chủ đích** so với quy ước "không gọi HTTP trong transaction" của DN-05: trạng thái công ty và cờ của HR phải đổi cùng nhau ([service-conventions.md](../architecture/service-conventions.md#6-cơ-sở-dữ-liệu-và-api-nội-bộ)). Gọi bù lỗi chỉ ghi log ERROR (cần sửa tay).
- Khoá dòng công ty làm hai lần khoá / mở khoá đồng thời chạy lần lượt (một bên 409). Ghi `company_hr` khi tạo HR (DN-01) lấy cùng khoá dòng và kiểm lại `LOCKED`, nên HR mới không lọt vào giữa; tạo HR cho công ty bị khoá → 403 `COMPANY_LOCKED`.
- `StaffCompanyDetail.lock = {lockedAt, reason, lockedBy {id, fullName}}` từ dòng `LOCK` mới nhất; null khi `ACTIVE`. `GET /staff/companies/{id}` trả cùng trường (cho banner).
- `StaffHrAccount.status` vẫn chỉ nói về **khoá riêng** (`LOCKED` khi và chỉ khi `lockedAt != null`), thêm `companyLocked`. company-service không chép thẳng `status` của identity nữa. Khoá / mở khoá riêng một HR vẫn làm được khi công ty đang khoá; HR đang bị khoá riêng vẫn bị khoá sau khi mở khoá công ty.

### 5.2 identity-service

`POST /internal/users/company-lock` (`InternalKeyFilter`, header `X-Internal-Key`) body `{userIds: [UUID] 1–100, locked: boolean}` → 200 mảng `InternalUserStatusResponse`. Chỉ áp dụng cho user role `HR`; id không tồn tại hoặc không phải HR bị bỏ qua; khoá lại HR đã khoá theo công ty giữ nguyên `company_locked_at` cũ. Thiếu / sai khoá → 401 `INVALID_SERVICE_KEY`; body sai → 400 `VALIDATION_ERROR`.

| Điểm | Khi HR bị khoá theo công ty |
| --- | --- |
| `POST /auth/login` | 403 `ACCOUNT_DISABLED` "Công ty của bạn đang bị khoá, liên hệ cán bộ tuyển dụng", **chỉ khi mật khẩu đúng** (sai mật khẩu vẫn 401 `INVALID_CREDENTIALS`, chống dò email bị khoá). Khoá riêng giữ message "Tài khoản đã bị khoá" và được báo trước |
| `POST /auth/sso` | như trên |
| `IdentityJwtAuthenticationFilter` (mọi đường dẫn, kể cả `/auth/**`) | 401 `ACCOUNT_DISABLED` |
| Quên / đặt lại mật khẩu | quên mật khẩu vẫn 200 nhưng không cấp token, không gửi thư; đặt lại không mở khoá |
| `POST /internal/users/lookup` | `status = LOCKED` + field mới `companyLocked: true` |
| `unlock` riêng (DN-05) | không đụng `company_locked_at` |

### 5.3 Chặn token đang còn hạn

| Service | Cách chặn |
| --- | --- |
| identity-service | `IdentityJwtAuthenticationFilter` đọc `users` mỗi request (`isLoginBlocked()`) → 401 `ACCOUNT_DISABLED` |
| company-service | `HrLockedFilter`: một truy vấn join `company_hr`–`companies` (`findAccessState`); khoá riêng báo "Tài khoản đã bị khoá", công ty `LOCKED` báo "Công ty của bạn đang bị khoá, liên hệ cán bộ tuyển dụng", cả hai 401 `ACCOUNT_DISABLED` |
| recruitment-service | không sửa: `HrLockedFilter` của nó hỏi `lookup`, `status = LOCKED` nên tự được bao |

Hệ quả có chủ đích: HR của công ty `LOCKED` mất **mọi** quyền ở company-service, kể cả đọc (`GET /companies/me`, `/companies/me/jobs`...). Các nhánh 403 `COMPANY_LOCKED` của DN-01/DN-02 còn giữ làm lớp phòng thủ thứ hai. `GET /jobs` cũng trả 401 với token của HR này (bộ lọc chạy trước) — chấp nhận, HR không dùng trang việc làm. Phía web, axios gặp 401 thì đưa về `/login`.

### 5.4 Khi công ty bị khoá

- JD `APPROVED` biến khỏi `GET /jobs`; `GET /jobs/{id}` → 404; JD giữ nguyên trạng thái, mở khoá là hiện lại.
- `application-context` trả `companyLocked = true` (UT-01 đã chặn ứng tuyển); `hrEmails` rỗng vì HR có `status = LOCKED` ở identity.
- Đơn ứng tuyển đã nộp không bị ảnh hưởng.

### 5.5 Lịch sử thao tác

`GET /staff/companies/{id}/history`: mới nhất trước (`created_at` giảm dần, rồi `id`), phân trang. `CompanyActionItem`: `id, action, createdAt, reason, staff {id, fullName}, job {id, title} | null`. Tên cán bộ tra theo lô ≤ 100 qua `lookup`; identity lỗi → vẫn 200 với `fullName = null` (giao diện hiện id rút gọn). Tiêu đề JD lấy bằng một truy vấn cho cả trang.

## 6. Cách chạy

Cần Docker (Postgres, MailHog), Java 21 + Maven, Node 24. Mỗi lệnh chạy từ gốc repo, mỗi tiến trình dài một terminal. Không có biến môi trường bắt buộc mới.

```bash
docker compose --env-file .env -f infra/compose.yaml up -d postgres mailhog
(cd mock && npm ci && npm start)                                      # mock LMS :9000 (SSO staff1, candidate1)
mvn -f server/pom.xml -pl services/identity-service,services/profile-service,services/company-service,api-gateway -am install -DskipTests
mvn -f server/services/identity-service/pom.xml spring-boot:run "-Dspring-boot.run.profiles=dev"   # :8081, chạy V3__company_lock
mvn -f server/services/profile-service/pom.xml spring-boot:run                                     # :8082, danh mục kỹ năng
mvn -f server/services/company-service/pom.xml spring-boot:run "-Dspring-boot.run.profiles=dev"    # :8085, chạy V6__job_review, seed 9 JD
mvn -f server/api-gateway/pom.xml spring-boot:run                                                  # :8080
(cd web && npm ci && npm run dev -- --port 3000)                                                   # http://localhost:3000
```

Trang việc làm của ứng viên (`/candidate/jobs`, DN-04) đọc JD thật khi `web/.env.local` có:

```env
NEXT_PUBLIC_JOB_API_PATH=/jobs
NEXT_PUBLIC_JOB_PAGE_PATH=/candidate/jobs
```

(khởi động lại `npm run dev` sau khi đổi). Muốn thử ứng tuyển, chạy thêm recruitment-service và mock như [DN-02 mục 6](DN-02-jobs.md#6-cách-chạy) (mock với `JWT_SECRET` giống identity và `MOCK_CV_OWNER_ID` = id của candidate1; recruitment với `PROFILE_SERVICE_URL=http://localhost:9000`, `INTERNAL_SERVICE_KEY=dev-internal-key`). Chạy web ở cổng khác 3000 thì đặt `WEB_BASE_URL=http://localhost:<cổng>` cho gateway (CORS) và mock (CORS của danh sách CV mẫu).

Biến cấu hình: `JOB_AUTO_CLOSE_ENABLED` (`app.jobs.auto-close.enabled`, mặc định `true`) tắt job tự đóng JD quá hạn. Lưu ý Maven trên PowerShell (đặt `-D...` trong nháy, không dùng `-am spring-boot:run`) xem [DN-01](DN-01-company-hr.md#5-cách-chạy). Đã chạy nhánh này với số migration cũ `V5__job_review.sql`: sửa DB dev theo [mục 2](#sửa-db-dev-đã-chạy-số-cũ-v5__job_reviewsql).

Đặt lại JD mẫu trên DB dev (chỉ xoá dữ liệu JD và lịch sử, không đụng bảng khác), rồi khởi động lại company-service với profile `dev`:

```sql
DELETE FROM company.match_scores;
DELETE FROM company.company_actions;
DELETE FROM company.job_skills;
DELETE FROM company.jobs;
```

Test backend: `mvn -f server/pom.xml -B clean test` (company-service, identity dùng H2 `MODE=PostgreSQL` + Flyway; identity được giả lập bằng mock bean / `MockRestServiceServer`). Test recruitment-service cần Testcontainers hoặc DB kiểm thử riêng qua `UT01_TEST_DB_URL` ([UT-02](UT-02-application-pipeline.md)), và `InterviewLiveFlowTest` chạy `node mock/server.js` nên cần `npm ci` trong `mock/` trước. Frontend: `npm run type-check`, `npm run lint`, `npm run build` trong `web/`.

Đi các kịch bản bằng [DN-07.http](DN-07.http) và giao diện `/staff/jobs`, `/staff/companies/{id}`.

## 7. Dữ liệu mẫu

Chỉ có khi chạy company-service với profile `dev` (`DEV_DATA_ENABLED=true`), chạy lại không trùng, UUID cố định `00000000-0000-0000-0003-00000000000{1..9}`. Hạn nộp tính theo `Clock` (hôm nay + 45 ngày). Seeder đặt `reviewed_at` cho mọi JD seed ở trạng thái đã xử lý (`APPROVED`/`REJECTED`/`CLOSED`), không seed `company_actions`.

| id (đuôi) | Công ty | Tiêu đề | Trạng thái | Ghi chú |
| --- | --- | --- | --- | --- |
| `…0001` | A | Junior Backend Developer (Java/Spring Boot) | `REJECTED` | lý do trả lại mẫu |
| `…0002` | A | Fresher ReactJS | `PENDING` | dùng cho kịch bản trả lại → HR sửa → duyệt |
| `…0003` | A | Thực tập sinh Tester | `APPROVED` | hiện ở trang việc làm |
| `…0004` | A | Middle Java Developer | `DRAFT` | duyệt → 409 (tiêu chí 5) |
| `…0005` | A | Senior DevOps | `CLOSED` | hạn nộp đã qua |
| `…0006`, `…0007` | A | Backend Developer Java / Fresher Java (mẫu DN-06) | `APPROVED` | hiện ở trang việc làm |
| `…0008` | B | Thực tập sinh Marketing | `APPROVED` | INTERNSHIP; bắt buộc `COMMUNICATION` 40; còn hiện khi khoá Công ty A |
| `…0009` | B | Nhân viên vận hành sàn TMĐT | `PENDING` | bắt buộc `COMMUNICATION` 40; hàng chờ có JD của hai công ty |

Sau khi seed: trang việc làm có **4** JD (`…0003`, `…0006`, `…0007`, `…0008`), hàng chờ có **2** JD (`…0002`, `…0009`). Tài khoản: `staff1` (SSO `lms-st-001`), `hr1@rikkeijobs.test` và `hr-new@rikkeijobs.test` (Công ty A, id `…0000a1`), `hr2@rikkeijobs.test` (Công ty B, id `…0000b2`), mật khẩu `Test@123`; `candidate1` SSO `lms-sv-001`.

## 8. Ca kiểm thử

| # | Ca | Cách thử | Kết quả mong đợi | Test tự động |
| --- | --- | --- | --- | --- |
| 1 | Trang việc làm (tiêu chí 1) | `DN-07.http` 2a–2c | chỉ JD `APPROVED`, còn hạn, công ty `ACTIVE`; hạn đúng hôm nay giờ VN còn, hôm qua ẩn; không token / token sai → 200; mới duyệt trước; `size=101` → 400 | `PublicJobApiTest` (`onlyVisibleJobsListed`, `deadlineTodayVisibleYesterdayHidden`, `publicListIgnoresMissingOrInvalidToken`, `sortedByPublishedDescAndPaged`, `listCarriesCompanyRef`) |
| 2 | Chi tiết công khai | 2d, 2e | JD hiện → 200 không có `status`/`rejectReason`; mọi trường hợp không hiện → cùng 404; `/jobs/{id}/match/**` vẫn cần token | `PublicJobApiTest` (`detailOfVisibleJob`, `detailOfHiddenJobIs404`, `matchEndpointStillNeedsAuth`) |
| 3 | Duyệt (tiêu chí 1, 4) | 4f, 4h | 200 `APPROVED`, `reviewedBy/At`, dòng `APPROVE_JD`; JD có ở `GET /jobs` | `StaffJobReviewApiTest.approvePendingWritesHistoryAndPublishes` |
| 4 | Duyệt sai trạng thái (tiêu chí 5) | 4e, 4g | `DRAFT`/`REJECTED`/`APPROVED`/`CLOSED` → 409 `INVALID_JOB_STATUS`; `PENDING` quá hạn → 409 `JOB_EXPIRED`; không ghi lịch sử | `StaffJobReviewApiTest` (`approveNonPendingIs409`, `approveExpiredPendingIs409`) |
| 5 | Trả lại (tiêu chí 2) | 4a–4d | thiếu / chỉ khoảng trắng / > 1000 → 400 `errors.reason`; hợp lệ → `REJECTED`; HR thấy lý do ở `GET /companies/me/jobs/{id}`, sửa, `submit` → `PENDING` | `StaffJobReviewApiTest` (`rejectNeedsReason`, `blankReasonRejected`, `reasonLengthCountsTrimmedText`, `rejectNonPendingIs409`) |
| 6 | Đóng JD | 5a, 5b | `APPROVED` + lý do → `CLOSED`, dòng `CLOSE_JD`; trạng thái khác → 409 | `StaffJobReviewApiTest.closeApprovedOnly` |
| 7 | Khoá / mở khoá (tiêu chí 3) | 6b, 6d–6h, 7a–7d | `LOCKED`, dòng `LOCK`, identity nhận đúng HR (lô ≤ 100) `locked: true`; JD của A biến mất, JD của B còn; HR của A → 401 ở company-service và identity, đăng nhập 403; mở khoá → hiện lại, đăng nhập được | `StaffCompanyLockApiTest` (`lockCompanyHidesJobsAndBlocksHr`, `unlockRestores`), `HrLockedFilterTest.hrOfLockedCompanyRejected` |
| 8 | Khoá sai | 6a, 6c, 7b | khoá khi đã khoá / mở khi đang hoạt động → 409; thiếu lý do → 400; identity lỗi → 503, công ty không đổi, không có lịch sử; lô lỗi giữa chừng được gọi bù; không có HR → không gọi identity | `StaffCompanyLockApiTest` (`lockTwiceOrUnlockActiveIs409`, `blankLockReasonRejected`, `identityDownLeavesCompanyUntouched`, `partialBatchFailureCompensates`, `timedOutBatchIsCompensated`, `lockCompanyWithoutHrSkipsIdentity`) |
| 9 | Lịch sử (tiêu chí 4) | 6i | mới nhất trước, có `staff.fullName`, `job.title`, `reason`; identity lỗi → 200, `fullName` null; phân trang | `CompanyHistoryApiTest` |
| 10 | Vai trò | 3e | token HR/CANDIDATE/ADMIN gọi `/staff/jobs/**`, lock/unlock, history → 403 | `rolesForbidden` ở `StaffJobReviewApiTest`, `StaffCompanyLockApiTest`, `CompanyHistoryApiTest` |
| 11 | Khoá đồng thời, commit lỗi | — | hai lần khoá cùng lúc → một 200, một 409, một dòng `LOCK`; commit lỗi sau khi identity đã nhận → gọi bù | `StaffCompanyLockApiTest` (`concurrentLockSerialized`, `rollbackAfterIdentityCompensates`) |
| 12 | Duyệt JD của công ty bị khoá | — | `APPROVED` nhưng không có ở `GET /jobs` tới khi mở khoá | `StaffCompanyLockApiTest.approvedJobOfLockedCompanyStaysHiddenUntilUnlock` |
| 13 | Job tự đóng | — | `APPROVED` hạn hôm qua → `CLOSED`; hạn hôm nay giữ; `PENDING` quá hạn giữ; chạy hai lần không lỗi | `JobAutoCloseSchedulerTest` |
| 14 | identity `company-lock` | 8a, 8b, 6j | thiếu khoá → 401; khoá → đăng nhập 403, token cũ 401, quên mật khẩu bị chặn, `lookup` `LOCKED` + `companyLocked`; mở khoá → đăng nhập lại được; khoá riêng còn sau khi mở khoá công ty; mở khoá riêng không xoá cờ công ty; id không phải HR bị bỏ qua | `InternalUserCompanyLockApiTest` |
| 15 | `StaffHrAccount` | `GET /staff/companies/{id}` khi khoá | `companyLocked = true`, `status` vẫn theo khoá riêng | `StaffCompanyLockApiTest.staffHrAccountShowsCompanyLock`, `individuallyLockedHrStaysLockedAfterCompanyUnlock` |
| 16 | Chi tiết JD của HR | 4c | `GET /companies/me/jobs/{id}` giữ hành vi cũ (404 công ty khác, chỉ HR) | `PublicJobApiTest.hrDetailMovedToCompaniesMe` |
| 17 | Gateway | mọi request `{{gw}}/staff/jobs/...` | `/api/v1/staff/jobs/**` tới company-service | `CompanyRouteIntegrationTest` |
| 18 | Seeder, migration | khởi động profile `dev` hai lần | thêm `…0008`, `…0009`, đặt `reviewed_at`, không trùng; `company_actions` ghi / đọc đúng | `DevDataSeederTest`, `CompanyActionPersistenceTest`, `HttpIdentityClientCompanyLockTest` |

Tên lớp test là các lớp trong `server/services/company-service/src/test`, `server/services/identity-service/src/test` và `server/api-gateway/src/test`. Chưa có test riêng cho xung đột `@Version` khi hai cán bộ cùng duyệt một JD (code dùng `saveAndFlush` để handler `CONFLICT` sẵn có bắt).

## 9. Kết quả xác minh

Đã chạy trên nhánh `feature/DN-07-job-review` (HEAD `c84af66` cộng tài liệu), ngày 26/11/2026:

- `mvn -f server/pom.xml clean test` với `UT01_TEST_DB_URL=jdbc:postgresql://localhost:5432/rikkei_jobs_test` (DB kiểm thử riêng, không phải DB dev), `"-Dspring.datasource.hikari.maximum-pool-size=2"`: **BUILD SUCCESS, 12 module, 955 test, 0 lỗi, 1 test có sẵn bị skip** (common-core 14, api-gateway 25, identity-service 274, profile-service 167 (1 skip), company-service 278, internship/assessment/learning mỗi service 5, recruitment-service 177, ai-service 5). Lần chạy đầu recruitment-service có 1 lỗi ở `InterviewLiveFlowTest` ("Mock did not start") vì worktree chưa `npm ci` trong `mock/`; sau khi cài, chạy lại toàn bộ thì đạt.
- `web/`: `npm run type-check` thành công; `npm run lint` 0 lỗi, 1 cảnh báo có sẵn `import/no-anonymous-default-export` ở `commitlint.config.mjs`; `npm run build` thành công (có `/staff/jobs`, `/staff/companies/[id]`, `/candidate/jobs`, `/candidate/jobs/[jobId]`).
- `postman spec lint docs/api/company-openapi.yaml --issueType=syntax` và `... identity-openapi.yaml ...`: "No issues found"; parse bằng `js-yaml` (trong `web/node_modules`) được 28 và 14 path, mọi `$ref` đều có schema.
- Chạy toàn luồng (Postgres và MailHog trong Docker; mock; identity và company-service profile `dev`; profile-service; recruitment-service với PDF CV từ mock; gateway; web dev ở cổng 3100 với `NEXT_PUBLIC_JOB_API_PATH=/jobs`, `NEXT_PUBLIC_JOB_PAGE_PATH=/candidate/jobs`). DB dev trước đó có `V5__job_review.sql` nên đã sửa theo mục 2; khởi động lại thì Flyway áp `V5__drop_english_skill` và `V6__job_review`. Trước khi chạy đã xoá `company.match_scores`, `company.job_skills`, `company.jobs` (bảng `company_actions` vừa được tạo lại rỗng), seeder tạo lại 9 JD. Trên DB dev Công ty A tên "RikkeiSoft", Công ty B tên "FPT Software". Quan sát:
  - Trước khi duyệt: `GET /api/v1/jobs?size=100` (không token) → `total` 4 (Thực tập sinh Marketing, Fresher Java, Backend Developer Java, Thực tập sinh Tester); `GET /jobs/{…0002}` → 404; token sai → 200. candidate1 ở `/candidate/jobs`: "4 vị trí", không có "Fresher ReactJS".
  - staff1 mở `/staff/jobs`: tab "Chờ duyệt (2)", hàng chờ "Fresher ReactJS · RikkeiSoft" và "Nhân viên vận hành sàn TMĐT · FPT Software", chi tiết JD bên phải với nút "Trả lại…" / "Duyệt". "Trả lại…" → bấm "Trả lại JD" khi để trống: "Vui lòng nhập lý do", không gửi. Nhập lý do → "Đã trả lại JD", hàng chờ còn 1 và tự chọn JD kế tiếp.
  - hr1 mở `/hr/jobs/{…0002}`: banner "Lý do trả lại: Mô tả công việc còn sơ sài, bổ sung phần quyền lợi và mức lương cụ thể."; sửa Quyền lợi → bước 2 → "Gửi duyệt" → về danh sách, "Đã gửi duyệt".
  - staff1 tải lại hàng chờ: "Chờ duyệt (2)", JD của B (gửi lúc seed) đứng trước "Fresher ReactJS" (vừa gửi lại), chi tiết hiện Quyền lợi mới. "Duyệt" → "Đã duyệt".
  - candidate1 tải lại `/candidate/jobs`: "5 vị trí", "Fresher ReactJS" đứng đầu; mở chi tiết, "Ứng tuyển" → chọn CV mẫu của mock → gửi → chuyển tới trang đơn ứng tuyển; DB `recruitment.applications` có đơn `SUBMITTED` cho `…0002`.
  - staff1 ở `/staff/companies/{A}` → "Khoá công ty" → hộp nêu hậu quả, nhập lý do → "Đã khoá công ty"; banner "Công ty đang bị khoá từ 26/11/2026 18:22 bởi Hoàng Thu Hà. Lý do: … HR không đăng nhập được; JD đang tuyển bị ẩn khỏi trang việc làm."; tiêu đề ghi "Đã khoá"; nút đổi thành "Mở khoá"; cả 3 HR của A có nhãn "Khoá theo công ty", nút "Khoá" riêng vẫn còn.
  - Phiên hr1 đang mở bấm sang JD → `GET /companies/me` và `GET /companies/me/jobs/{id}` trả 401, giao diện về `/login`. hr1 đăng nhập lại: "Công ty của bạn đang bị khoá, liên hệ cán bộ tuyển dụng" (403); sai mật khẩu → 401 `INVALID_CREDENTIALS`.
  - `GET /jobs` → `total` 1 (chỉ Thực tập sinh Marketing của B); `GET /jobs/{…0002}` → 404; `/candidate/jobs` "1 vị trí". `application-context` của `…0002` → `companyLocked` true, `hrEmails` []; identity `lookup` hr1 → `status` `LOCKED`, `companyLocked` true.
  - Tab "Lịch sử thao tác" (`?tab=history`): 3 dòng mới nhất trước — Khoá công ty (lý do), Duyệt JD "Fresher ReactJS", Trả lại JD "Fresher ReactJS" (lý do), cán bộ "Hoàng Thu Hà". Tab "JD của công ty" (`?tab=jobs`): 7 JD của A với nhãn trạng thái, hạn nộp, thời điểm cập nhật, bộ lọc Tất cả + 5 trạng thái.
  - "Mở khoá" → hộp xác nhận → "Đã mở khoá công ty"; `GET /jobs` → `total` 5 (JD của A hiện lại); hr1 đăng nhập được, vào `/hr`; DB `users.company_locked_at` của mọi HR về null, `companies.status` `ACTIVE`; đơn của candidate1 vẫn `SUBMITTED`.
  - Tab "Đang tuyển" ở `/staff/jobs`, chọn "Thực tập sinh Tester" → "Đóng JD…" → nhập lý do → "Đã đóng JD"; `GET /jobs` còn 4 JD.
  - Gọi API bằng token staff1: duyệt JD `DRAFT` → 409 `INVALID_JOB_STATUS`; trả lại với lý do `"   "` → 400 `errors.reason` "Vui lòng nhập lý do"; đóng JD `PENDING` → 409; khoá với lý do rỗng → 400; mở khoá công ty đang hoạt động → 409 "Công ty không bị khoá"; `GET /staff/jobs?companyId={A}` (không `status`) → 7 JD mọi trạng thái; lịch sử → `CLOSE_JD`, `UNLOCK`, `LOCK`, `APPROVE_JD`, `REJECT_JD` đủ tên cán bộ, tiêu đề JD, lý do. Token hr2: `POST /staff/jobs/{id}/approve` → 403 `FORBIDDEN`; `GET /companies/me/jobs/{…0009}` → 200.
- Chưa kiểm tay: job tự đóng lúc 00:05 (có `JobAutoCloseSchedulerTest`); identity tắt khi khoá (503, gọi bù), khoá đồng thời, commit lỗi (có test ở mục 8); `JOB_EXPIRED` trên giao diện (seed không có JD chờ duyệt đã quá hạn); request trong `DN-07.http` mới được đối chiếu một phần qua các lệnh gọi ở trên, chưa chạy lần lượt cả file.
