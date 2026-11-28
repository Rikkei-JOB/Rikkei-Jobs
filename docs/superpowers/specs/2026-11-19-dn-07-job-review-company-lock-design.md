# DN-07 · Cán bộ duyệt JD, quản lý doanh nghiệp — Thiết kế

Task: **[Doanh nghiệp & JD · DN-07] Cán bộ duyệt JD, quản lý doanh nghiệp**. Nhánh `feature/DN-07-job-review` từ `test`, PR vào `test` (đề ghi "develop" là ghi nhầm, nhóm dùng `test` như DN-01/02/05), tiêu đề bắt đầu `[DN-07]`. Phụ thuộc DN-02 (bảng `jobs`), DN-05 (màn doanh nghiệp), đều đã merge; DN-06 (điểm khớp) đã merge.

Ký hiệu: **(chốt)** là điều người giao task đã quyết; **(người thiết kế chọn)** là quyết định trong lúc thiết kế, người làm có thể nêu lại nếu thấy sai.

> **Cập nhật 27/11 — UI-01 là nền.** Trong lúc làm DN-07, task **UI-01** (người khác) đã merge vào `test` một phần trùng phạm vi: trang việc làm công khai `GET /public/jobs`, `GET /public/jobs/{id}`, `GET /public/companies[/{id}]` (`PublicJobController`, `PublicJobSpecifications`, trang web `/jobs`, `/companies`) **và** hàng chờ của cán bộ `GET /staff/jobs`, `GET /staff/jobs/summary`, `GET /staff/jobs/{id}`, `POST /staff/jobs/{id}/approve|reject` (`StaffJobController`/`StaffJobService`, quyền `STAFF_OR_ADMIN`) cùng màn `/staff/jobs`, `/staff/jobs/[id]`.
>
> Người dùng chốt: **UI-01 là nền, DN-07 bổ sung lên trên**. Các phần DN-07 tự làm rồi bị bỏ: stack `GET /jobs` + `GET /jobs/{id}` công khai của DN-07 (`PublicJobMapper`, `PublicJobSummary`, `findVisibleToCandidates*`), `StaffJobReviewService`/`StaffJobController` riêng của DN-07, màn Duyệt JD riêng và `JobPublicView`. Chi tiết JD của HR **quay lại `GET /jobs/{id}`** (đường công khai nay là `/public/**`), nên mục 4.3 không còn dời đường dẫn này nữa.
>
> **Phần DN-07 còn lại (nội dung thật của nhánh này):** `POST /staff/jobs/{id}/close` body `{reason}`; ghi `company_actions` cho `APPROVE_JD`/`REJECT_JD`/`CLOSE_JD`; mã lỗi `JOB_EXPIRED`; `ReasonRules` (trim, kể cả khoảng trắng không ngắt, 1–1000 ký tự); khoá / mở khoá công ty (company-service + cờ `users.company_locked_at` của identity + `HrLockedFilter`); `GET /staff/companies/{id}/history` và tab "Lịch sử thao tác"; tab "JD của công ty"; job tự đóng JD quá hạn; nút "Đóng JD…" trên màn `/staff/jobs/[id]` của UI-01.
>
> Những chỗ trong tài liệu này còn viết theo thiết kế cũ được đánh dấu **(UI-01 thay thế)** ngay tại mục tương ứng; mục 4.3, 4.4 và 5 đã viết lại theo trạng thái thật của code.

## 1. Mục đích

Cán bộ tuyển dụng (STAFF) kiểm soát JD trước khi ứng viên thấy, và khoá được doanh nghiệp vi phạm. Ứng viên chỉ thấy JD đã được duyệt, còn hạn, của công ty không bị khoá.

### Tiêu chí nghiệm thu (chốt)

1. JD `PENDING` không hiện ở trang việc làm; duyệt xong thì hiện.
2. Trả lại không có lý do → 400; HR thấy lý do, sửa và gửi lại được (phần HR đã có từ DN-02).
3. Khoá công ty A → JD của A biến khỏi trang việc làm, hr1 không đăng nhập được; mở khoá → hiện lại.
4. Lịch sử ghi người làm, thời điểm, lý do.
5. Duyệt JD đang `DRAFT` → lỗi, không duyệt được: **409** `INVALID_JOB_STATUS` "JD vẫn là bản nháp, HR chưa gửi duyệt nên cán bộ không xử lý được.", giống `REJECTED`/`APPROVED`/`CLOSED`, và áp dụng cho cả `reject` và `close`. **404 chỉ dành cho id không có trong DB.** Phần *xem* giữ nguyên hành vi của UI-01: JD nháp không có trong `GET /staff/jobs`, không tính vào `/staff/jobs/summary`, và `GET /staff/jobs/{id}` của JD nháp vẫn trả 404 (cán bộ không được xem bản nháp của HR). Lỗi ném trước khi đổi dữ liệu nên không ghi dòng `company_actions` nào. **Cập nhật 27/11 (lần 2):** bản UI-01 trả 404 cho cả thao tác; DN-07 sửa `StaffJobService` tách hai cách đọc JD (`visibleJob` cho phần xem, `reviewableJob` cho phần thao tác) để đúng tiêu chí này, kéo theo sửa test `draftsAreHiddenAndOnlyStaffAllowed` của UI-01.
6. PR vào `test` có cách chạy, dữ liệu mẫu, ảnh màn hình; OpenAPI cập nhật endpoint mới. Làm đủ DB, BE, FE.

### Phạm vi

Trong DN-07 (chốt), sau khi lấy UI-01 làm nền:

- **Đóng JD của cán bộ** `POST /staff/jobs/{id}/close` body `{reason}` (đóng JD: người dùng chốt thêm, đề chỉ ghi trong sơ đồ trạng thái) và nút "Đóng JD…" trên màn `/staff/jobs/[id]` của UI-01.
- **Ghi lịch sử** `company_actions` cho mọi thao tác JD của cán bộ (`APPROVE_JD`, `REJECT_JD`, `CLOSE_JD`) — UI-01 duyệt / trả lại nhưng không ghi lịch sử.
- **`JOB_EXPIRED`** (duyệt JD đã quá hạn nộp) và **`ReasonRules`** (lý do bắt buộc, trim kể cả khoảng trắng không ngắt, 1–1000 ký tự) dùng chung cho `reject`, `close`, `lock`.
- Khoá / mở khoá công ty; HR của công ty bị khoá không đăng nhập được (kể cả token đang còn hạn).
- Bảng `company_actions` (gồm giá trị `CLOSE_JD`), `GET /staff/companies/{id}/history`, tab "Lịch sử thao tác".
- Job hằng ngày đóng JD quá hạn.
- Tab **"JD của công ty"** trong trang chi tiết doanh nghiệp của cán bộ (chốt trong spec DN-05 làm mới giao diện `2026-11-24-dn-ui-refresh-design.md`, dời sang DN-07), dùng `GET /staff/jobs?companyId=` của UI-01.

Đã có từ **UI-01** (nền, DN-07 không làm lại): trang việc làm công khai `/jobs`, `/companies` và API `GET /public/jobs`, `/public/jobs/{id}`, `/public/companies[/{id}]` cùng quy tắc "ứng viên thấy JD nào" (`PublicJobSpecifications.visible`); hàng chờ và màn duyệt của cán bộ `GET /staff/jobs`, `/staff/jobs/summary`, `/staff/jobs/{id}`, `POST /staff/jobs/{id}/approve|reject`, trang `/staff/jobs` + `/staff/jobs/[id]`.

Ngoài phạm vi:

- Bộ lọc / `sort=match` / `matchScore` / điểm khớp chi tiết / `job_views` của trang việc làm. **Cập nhật 27/11:** UI-01 đã có `q`, `skillIds`, `city`, `level`, `workMode`, `type`, `salaryFrom/To`, `companyId`, `sort=newest|salary` trên `GET /public/jobs`; phần điểm khớp vẫn chưa làm.
- Gửi email báo HR khi JD được duyệt / bị trả lại / bị đóng (người thiết kế chọn: HR xem ngay trên `/hr/jobs`).
- Ghi lịch sử cho việc hệ thống tự đóng JD quá hạn (chốt: lịch sử chỉ ghi thao tác của cán bộ).
- Trạng thái "Tạm dừng", tự đóng khi "đủ người".
- Trang `/candidate/jobs` và `/candidate/jobs/{id}` (DN-04, đã merge): DN-07 không sửa. Trang việc làm để demo tiêu chí 1 và 3 là `/jobs` của UI-01, không cần cấu hình gì thêm.

**Quyền.** Thiết kế ban đầu ghi "chỉ STAFF, không ADMIN" (người thiết kế chọn theo đề). Thực tế theo UI-01: `/staff/jobs/**` (gồm `close` của DN-07) là **`STAFF_OR_ADMIN`** theo ma trận quyền NT-01, còn `/staff/companies/**` của DN-05/DN-07 (chi tiết công ty, khoá / mở khoá, lịch sử) vẫn **chỉ `STAFF`**. Giữ nguyên hai mức này; ADMIN duyệt / trả lại / đóng được JD nhưng không khoá được công ty và không xem được lịch sử.

## 2. Ranh giới service (chốt phương án A cho khoá công ty)

- **company-service**: duyệt / trả lại / đóng JD, khoá / mở khoá công ty, `company_actions`, lịch sử, job tự đóng, và (từ UI-01) quy tắc "ứng viên thấy JD nào" cùng trang việc làm công khai `GET /public/jobs`, `GET /public/jobs/{id}`. Bảng `companies.status` vẫn là nguồn sự thật của trạng thái công ty.
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

`jobs`: không đổi cột (`status`, `reviewed_by`, `reviewed_at`, `reject_reason` đã có từ DN-02). Thêm chỉ mục `idx_jobs_status_deadline (status, deadline)` cho trang việc làm công khai và job tự đóng (DN-04 có liệt kê chỉ mục này; DN-07 tạo trước, DN-04 không tạo lại).

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

Route `company` đã gom `/api/v1/staff/jobs/**`, `/api/v1/public/jobs/**`, `/api/v1/public/companies/**` (UI-01) cùng `/api/v1/jobs/**`, `/api/v1/companies/**`, `/api/v1/staff/companies/**` (giữ `StripPrefix=2`, không thêm route id). `CompanyRouteIntegrationTest` có ca cho các tiền tố này.

### 4.2 Quy tắc "ứng viên thấy JD nào" (chốt) — **UI-01 thay thế cách viết**

Một JD **hiện với ứng viên** khi và chỉ khi: `status = APPROVED` **và** `deadline >= hôm nay` theo `Asia/Ho_Chi_Minh` (bean `Clock`, `JobRules.today`) **và** công ty của JD có `status = ACTIVE`. Viết đúng một chỗ: `PublicJobSpecifications.visible(today)` của UI-01 (Criteria + subquery `companies` đang `ACTIVE`), dùng cho `GET /public/jobs`, `GET /public/jobs/{id}`, `GET /public/companies` (đếm `openJobs`). Khác thiết kế ban đầu một điểm: JD **không ghi hạn nộp** (`deadline IS NULL`) cũng hiện — JD seed nháp chưa có hạn, còn JD đã duyệt thì bắt buộc có hạn nộp nên thực tế không gặp. `application-context` của UT-01 giữ nguyên (đã trả đủ dữ liệu để UT-01 tự quyết).

### 4.3 Trang việc làm (công khai) — **UI-01, DN-07 không làm lại**

`CompanySecurityConfig` mở `permitAll` cho `GET /public/jobs`, `/public/jobs/*`, `/public/companies`, `/public/companies/*` (mẫu một đoạn, nên `/jobs/{id}/match/**` của DN-06 vẫn cần đăng nhập). Token thiếu hoặc sai vẫn 200; token hợp lệ vẫn chạy đủ bộ lọc, nên token `mustChangePassword` nhận 403 và **token của HR thuộc công ty đang bị khoá nhận 401** `ACCOUNT_DISABLED` ngay trên `/public/**` (`HrLockedFilter` chạy sau `JwtAuthenticationFilter`, trước controller) — chấp nhận, HR không dùng trang việc làm; phía web axios gặp 401 thì đưa về `/login`.

- **`GET /public/jobs`** → `200` `PageResponse<PublicJobItem>`. Tham số: `q` (tiêu đề, vị trí, tên công ty), `skillIds`, `city` (khớp một phần), `level`, `workMode`, `type`, `salaryFrom`, `salaryTo`, `companyId`, `sort` (`newest` mặc định hoặc `salary`), `page` từ 1, `size` ≤ 100; sai → 400 `BAD_REQUEST`. Sắp `createdAt` giảm dần rồi `id` (`sort=salary`: lương cao trước).
- `PublicJobItem`: `id, title, level, type, workMode, city, salaryMin, salaryMax, deadline, createdAt`, `skills [{skillId, required, minScore}]`, `company {id, name, logoUrl, industry, size, city}`.
- **`GET /public/jobs/{id}`** → `200` `PublicJobDetail` nếu JD hiện với ứng viên; ngược lại **404** `NOT_FOUND` (JD chờ duyệt, nháp, bị trả lại, đã đóng, quá hạn, công ty bị khoá, id không tồn tại đều cùng một 404). `PublicJobDetail` thêm `position, quantity, address, description, requirements, benefits`. Không trả `status`, `rejectReason`, `reviewedBy`, `createdBy`.
- **`GET /public/companies`** → `200` mảng `PublicCompanyItem` (`id, name, logoUrl, industry, size, city, openJobs`), không phân trang, nhiều tin trước rồi theo tên. **`GET /public/companies/{id}`** → `PublicCompanyDetail` (thêm `address, website, description, openJobs`); công ty không tồn tại hoặc `LOCKED` → 404.
- Backend không trả tên kỹ năng (frontend ghép với `GET /skills` như DN-02).

**Chi tiết JD của HR vẫn là `GET /jobs/{id}`** (thay đổi 27/11, huỷ quyết định dời sang `GET /companies/me/jobs/{id}` của bản nháp spec): đường công khai nay nằm dưới `/public/**` nên `/jobs/{id}` không còn bị tranh chấp. `@PreAuthorize(Roles.HR)` ở từng method của `JobController`, trả `JobResponse`, JD của công ty khác → 404. `web/src/services/job.service.ts` (`get`), `DN-02-jobs.md`, `DN-02.http` và OpenAPI đã trả lại đường này.

### 4.4 Cán bộ xử lý JD — **UI-01 là nền, DN-07 bổ sung `close` + lịch sử + `JOB_EXPIRED`**

Mọi endpoint `/staff/jobs/**` là `@PreAuthorize(Roles.STAFF_OR_ADMIN)` (UI-01, theo ma trận quyền NT-01); token HR/CANDIDATE → 403. Cán bộ **không thấy JD nháp**: `GET /staff/jobs` chỉ liệt kê `PENDING`/`APPROVED`/`REJECTED`/`CLOSED` và `GET /staff/jobs/{id}` của JD nháp trả 404 `NOT_FOUND` "Không tìm thấy JD." (`visibleJob`). Nhưng **thao tác** trên JD nháp đang tồn tại trả 409 `INVALID_JOB_STATUS` (`reviewableJob`), vì JD có thật, chỉ sai trạng thái; 404 dành cho id không có trong DB (tiêu chí 5). Mỗi thao tác chạy trong một transaction, `saveAndFlush` để xung đột `@Version` của `jobs` lộ ra ngay (→ 409 `CONFLICT`, handler đã có), ghi `reviewed_by = principal.userId()`, `reviewed_at = now`, và **một** dòng `company_actions` trong cùng transaction (phần DN-07 thêm vào UI-01).

**`GET /staff/jobs?status=&q=&companyId=&page=1&size=20`** → `200` `PageResponse<StaffJobItem>`. `status` tuỳ chọn (bỏ trống = mọi trạng thái trừ `DRAFT`, dùng cho tab "JD của công ty"); `status=DRAFT` → 400 "Cán bộ không xem được JD nháp."; `q` tìm theo tiêu đề, vị trí, tên công ty; `companyId` tuỳ chọn, sai → 400. Sắp: `status=PENDING` theo `updated_at` **tăng dần** (gửi sớm nhất trước — JD đang `PENDING` không sửa được nên `updated_at` chính là lúc gửi); các trường hợp khác `updated_at` giảm dần. `StaffJobItem`: `id, title, level, status, deadline, submittedAt` (= `updatedAt`), `reviewedAt, requiredSkillCount, rejectReason` (chỉ khác null khi `REJECTED`), `companyId, companyName, companyStatus`.

**`GET /staff/jobs/summary`** → `200` `{total, byStatus}` với đủ 4 khoá `PENDING`/`APPROVED`/`REJECTED`/`CLOSED` (số JD toàn hệ thống, không tính `DRAFT`); màn Duyệt JD lấy số trên tab từ đây.

**`GET /staff/jobs/{id}`** → `200` `StaffJobDetail` = `{job: JobResponse (như HR thấy, gồm status, rejectReason, reviewedAt), reviewedBy: UUID | null, company {id, name, status, industry, size, city, logoUrl}}`. Mọi trạng thái trừ `DRAFT`, mọi công ty (kể cả công ty bị khoá — `company.status = LOCKED` là nguồn của nhãn "Công ty đang bị khoá"). Không tồn tại hoặc nháp → 404 (chỉ phần xem; thao tác trên JD nháp là 409 — xem dưới). `reviewedBy` là **UUID**, không kèm tên: màn hình tra tên cán bộ qua tab Lịch sử thao tác.

**`POST /staff/jobs/{id}/approve`** (không body) → `200` `StaffJobDetail`.
- Chỉ từ `PENDING` → `APPROVED`; `REJECTED`/`APPROVED`/`CLOSED` → **409** `INVALID_JOB_STATUS` "JD không ở trạng thái chờ duyệt (có thể cán bộ khác vừa xử lý)."; `DRAFT` → **409** `INVALID_JOB_STATUS` "JD vẫn là bản nháp, HR chưa gửi duyệt nên cán bộ không xử lý được." (tiêu chí 5); id không có → 404.
- `deadline < hôm nay` → **409** `JOB_EXPIRED` (mã mới của DN-07, message "Hạn nộp của JD đã qua. Hãy trả lại để HR cập nhật hạn nộp trước khi duyệt.").
- Xoá `reject_reason`. Ghi `APPROVE_JD`, `reason` NULL.
- Công ty bị khoá vẫn duyệt được; JD chỉ hiện khi công ty được mở khoá (chốt).

**`POST /staff/jobs/{id}/reject`** body `{reason}` → `200` `StaffJobDetail`.
- `reason` bắt buộc, trim, 1–1000 ký tự; thiếu, rỗng hoặc chỉ khoảng trắng → **400** `VALIDATION_ERROR`, `errors.reason` (tiêu chí 2).
- Chỉ từ `PENDING` → `REJECTED`; khác → 409 `INVALID_JOB_STATUS` (kể cả `DRAFT`, với thông báo riêng của bản nháp); id không có → 404.
- Ghi `reject_reason = reason`, action `REJECT_JD` kèm lý do. HR thấy lý do trên form DN-02 và gửi lại được (đã có).

**`POST /staff/jobs/{id}/close`** body `{reason}` → `200` `StaffJobDetail` (endpoint của DN-07).
- `reason` bắt buộc như trên (người thiết kế chọn: đóng do vi phạm phải có lý do).
- Chỉ từ `APPROVED` → `CLOSED`; khác → 409 `INVALID_JOB_STATUS` "Chỉ đóng được JD đang đăng (đã duyệt)." (`DRAFT` cũng 409, với thông báo riêng của bản nháp); id không có → 404. Ghi `CLOSE_JD` kèm lý do. `reject_reason` không đổi.
- Đóng JD **ghi đè `reviewed_by`/`reviewed_at`** bằng người đóng và thời điểm đóng (dùng chung đường `review(...)` với duyệt / trả lại): người duyệt trước đó chỉ còn trong `company_actions`. Lý do đóng cũng chỉ nằm trong lịch sử, không có trong `StaffJobDetail`.

**Lý do (`ReasonRules`, dùng chung cho `reject`, `close`, `lock`)**: strip hai đầu rồi kiểm; chuỗi chỉ gồm khoảng trắng (kể cả khoảng trắng không ngắt U+00A0, U+2007, U+202F) coi như thiếu → `errors.reason` = "Vui lòng nhập lý do"; dài hơn 1000 ký tự sau khi strip → "Lý do tối đa 1000 ký tự". Hai thông điệp này là bản chuẩn; `RejectJobRequest` của UI-01 dùng cùng chuỗi (trước 27/11 nó ghi "Vui lòng nhập lý do trả lại." / "Lý do tối đa 1.000 ký tự.").

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
- JD `APPROVED` biến khỏi `GET /public/jobs`, `GET /public/jobs/{id}` → 404; `application-context` trả `companyLocked = true` (UT-01 đã chặn ứng tuyển). JD giữ nguyên trạng thái, mở khoá là hiện lại (tiêu chí 3).
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

company-service `HrLockedFilter`: thêm điều kiện chặn 401 `ACCOUNT_DISABLED` khi HR thuộc công ty `LOCKED` (một truy vấn join `company_hr`–`companies`, hoặc đọc kèm khi đã tra `company_hr`), message "Công ty của bạn đang bị khoá, liên hệ cán bộ tuyển dụng". Hệ quả có chủ đích (theo tiêu chí 3: HR của công ty bị khoá không vào được hệ thống): HR của công ty `LOCKED` mất **mọi** quyền truy cập company-service, kể cả đọc. Các nhánh 403 `COMPANY_LOCKED` của DN-01/DN-02 vẫn giữ trong code làm lớp phòng thủ thứ hai nhưng HR không còn chạm tới; sửa các test đang khẳng định "công ty LOCKED: GET 200, ghi 403" (`JobApiTest`, `JobLifecycleApiTest`, `MyCompanyApiTest`, `CompanyLogoApiTest`) thành 401 `ACCOUNT_DISABLED`, và sửa câu tương ứng trong `DN-01-company-hr.md`, `DN-02-jobs.md`. Endpoint công khai `GET /public/jobs` cũng trả 401 với token của HR này (bộ lọc chạy sau `JwtAuthenticationFilter`, trước controller) — chấp nhận, vì HR không dùng trang việc làm; phía web axios gặp 401 thì đưa về `/login`. Token phải đổi mật khẩu nhận 403 ở `/public/**` là hành vi sẵn có của common-core.

`StaffHrAccount` (DN-05) giữ nguyên nghĩa `status` = **khoá riêng** (`LOCKED` khi và chỉ khi `lockedAt != null`), và thêm field `companyLocked` (boolean). company-service **không** chép thẳng `lookup.status` vào `StaffHrAccount.status` nữa: `IdentityUserStatus` thêm `companyLocked`, `StaffHrAccountMapping` tính `status` từ `lockedAt` (người thiết kế chọn). Bảng HR (DN-05) thêm nhãn "Khoá theo công ty" khi `companyLocked`; nút Khoá / Mở khoá **riêng** vẫn hiện theo `status` riêng và vẫn dùng được (khoá riêng một HR trong lúc công ty đang khoá, để khi mở khoá công ty HR đó vẫn bị khoá). Cập nhật `DN-05-company-hr-ui.md` và OpenAPI.

### 4.8 Job tự đóng JD quá hạn

Bật `@EnableScheduling` trong company-service (chưa có). Một bean `JobAutoCloseScheduler` với `@Scheduled(cron = "0 5 0 * * *", zone = "Asia/Ho_Chi_Minh")`: một câu `UPDATE` đóng mọi JD `APPROVED` có `deadline < JobRules.today(clock)` → `CLOSED`, tăng `version`, cập nhật `updated_at`; không đổi `reviewed_*`, không ghi `company_actions`. Ghi log INFO số JD đã đóng. Cờ `app.jobs.auto-close.enabled` (mặc định `true`; test đặt `false` và gọi thẳng phương thức để kiểm). Chạy lại nhiều lần không sai (idempotent). Một instance nên không cần khoá phân tán (người thiết kế chọn, `@Scheduled` theo boring-by-default).

## 5. Frontend

Đọc `web/AGENTS.md` và `web/node_modules/next/dist/docs/` trước khi viết code Next. TanStack Query (khoá cache kèm user id), `api` sẵn có, `@base-ui/react` (Dialog, Tabs, Menu). **Cập nhật 27/11:** UI-01 đã thay `ManagerShell` bằng các khung theo vai trò (`web/src/app/components/shell/`), font Lexend và `web/DESIGN.md`; trang của cán bộ dùng `TaskBanner` + bảng `sjr-*` trong `web/src/app/components/staff-jobs/staff-jobs.css`. Màn hình DN-07 đi theo khung và bảng màu nhãn sẵn có (`web/src/app/components/staff-jobs/job-labels.ts`), **không** đặt lại token màu. Mục "Duyệt JD" đã có trong thanh điều hướng của cán bộ (NT-02). Mockup `.superpowers/brainstorm/1505-1795060628/content/` chỉ còn là tham khảo: bố cục A hai cột đã bị UI-01 thay bằng danh sách + trang chi tiết riêng.

### 5.1 Khối chi tiết JD dùng chung — **đã bỏ**

`JobPublicView` của DN-07 không còn: UI-01 dựng sẵn khối chi tiết JD trong `web/src/app/components/staff-jobs/staff-job-review.tsx` (đọc `StaffJobDetail`, hiện công ty, chip cấp độ / loại hình / hình thức / lương / số lượng / hạn nộp, kỹ năng Bắt buộc – Ưu tiên ghép tên từ `GET /skills`, Mô tả / Yêu cầu / Quyền lợi) và trang công khai `/jobs/[jobId]` dùng khối riêng của nó.

### 5.2 Màn "Duyệt JD" `/staff/jobs` + `/staff/jobs/[id]` — **UI-01, DN-07 thêm nút Đóng JD**

UI-01 (nền):

- `/staff/jobs`: `TaskBanner` "Duyệt JD", ô tìm `q`, tab **Chờ duyệt / Đang tuyển / Bị trả lại / Đã đóng** (số lấy từ `GET /staff/jobs/summary`), bảng JD / Cấp bậc / Hạn nộp / Gửi duyệt–Cập nhật / Trạng thái, phân trang; tab và trang lưu trên URL. Tab Chờ duyệt xếp JD gửi sớm nhất lên trên.
- `/staff/jobs/[id]`: khối chi tiết JD, thanh thao tác **Duyệt JD** (hộp xác nhận) và **Trả lại cho doanh nghiệp** (ô lý do bắt buộc, đếm ký tự, lỗi theo trường ngay dưới ô).

DN-07 thêm:

- Nút **Đóng JD** trên JD đang `APPROVED` (ô lý do bắt buộc như trả lại) kèm câu nhắc: JD ngừng hiển thị, không mở lại được, HR cũng không sửa hay gửi duyệt lại được (`CLOSED` là trạng thái cuối theo DN-02), lý do được ghi vào lịch sử doanh nghiệp.
- JD của công ty đang bị khoá (`company.status = LOCKED`) có dòng nhắc "Công ty đang bị khoá: JD được duyệt vẫn không hiển thị cho ứng viên cho tới khi công ty được mở khoá".
- Nút **Duyệt JD** bị vô hiệu khi JD đã quá hạn nộp (tránh 409 `JOB_EXPIRED` không cần thiết).
- Sau khi duyệt / trả lại / đóng: thông báo, cập nhật cache chi tiết bằng kết quả trả về, làm mới hàng chờ, `summary`, và lịch sử + chi tiết công ty tương ứng; có nút mở JD chờ duyệt kế tiếp. Lỗi 409 (JD vừa bị người khác xử lý, hoặc quá hạn) → thông báo và tải lại.

### 5.3 Chi tiết doanh nghiệp `/staff/companies/{id}` (chốt theo mockup)

- Trang dùng `CompanyHeader` có sẵn (DN-05 làm mới). Vùng nút: **+ Thêm HR** và **Khoá công ty** (đang hoạt động) / **Mở khoá** (đang khoá). Tab đặt qua prop `tabs` của `CompanyHeader`.
- Công ty đang khoá: banner đỏ "Công ty đang bị khoá từ <thời điểm> bởi <cán bộ>. Lý do: … HR không đăng nhập được; JD đang tuyển bị ẩn khỏi trang việc làm."
- Ba tab: **Tài khoản HR (n)** (bảng hiện tại của DN-05, thêm nhãn "Khoá theo công ty"), **JD của công ty** (cập nhật 26/11: bảng Tiêu đề / Trạng thái (nhãn màu) / Hạn nộp / Cập nhật, bộ lọc trạng thái Tất cả + 5 trạng thái, phân trang, dùng `GET /staff/jobs?companyId=<id>&status=`; bấm một dòng mở `/staff/jobs?companyId=<id>&status=<trạng thái JD>&jobId=<id>` để xem và xử lý ở màn Duyệt JD) và **Lịch sử thao tác** (bảng Thời điểm / Cán bộ / Hành động có nhãn màu / JD / Lý do, phân trang). Tab đang chọn lưu trên URL (`?tab=hr|jobs|history`).
- Hộp khoá: nêu hậu quả, ô lý do bắt buộc. Mở khoá: hộp xác nhận, không có lý do.

### 5.4 Trang việc làm — **UI-01**

Cập nhật 27/11: trang việc làm để demo tiêu chí 1 và 3 là **`/jobs`** và `/jobs/{jobId}` của UI-01 (nhóm route `(public)`, đọc `GET /public/jobs` qua `web/src/services/job-board.service.ts`), kèm `/companies`, `/companies/{companyId}`. Không cần biến môi trường nào.

Trang `/candidate/jobs`, `/candidate/jobs/{jobId}` của DN-04 **không đổi**; nó vẫn đọc endpoint cấu hình bằng `NEXT_PUBLIC_JOB_API_PATH` và chạy được với `NEXT_PUBLIC_JOB_API_PATH=/public/jobs` (ghi dạng chú thích trong `web/.env.example`), nhưng không còn là trang dùng cho tiêu chí nghiệm thu của DN-07.

### 5.5 HR

Không đổi gì: `job.service.get` vẫn là `GET /jobs/{id}` (mục 4.3). Form DN-02 đã hiện lý do trả lại và cho gửi lại.

## 6. Cấu hình

- company-service: `app.jobs.auto-close.enabled` (mặc định `true`). Không có biến môi trường bắt buộc mới (`INTERNAL_SERVICE_KEY`, `IDENTITY_SERVICE_URL` đã có).
- Ghi `.env.example` và mục Cấu hình của `docs/architecture/service-conventions.md` nếu thêm biến.

## 7. Kiểm thử

Test backend không cần Postgres (H2 + Flyway như các service hiện có). Giả lập identity bằng mock bean / `MockRestServiceServer`. Test của recruitment-service chạy theo `docs/api/UT-02-application-pipeline.md` (Testcontainers hoặc DB kiểm thử riêng); DN-07 không sửa recruitment.

| # | Ca | Ở đâu | Tiêu chí |
| --- | --- | --- | --- |
| 1 | `GET /public/jobs`: chỉ JD `APPROVED`, còn hạn (ranh giới hôm nay theo giờ VN), công ty `ACTIVE`; JD `PENDING`/`DRAFT`/`REJECTED`/`CLOSED`/quá hạn/công ty khoá không có; gọi không token → 200; bộ lọc và phân trang (UI-01 đã có test) | company | 1 |
| 2 | `GET /public/jobs/{id}`: JD hiện → 200 không có `status`/`rejectReason`; mọi trường hợp không hiện → cùng 404 | company | 1 |
| 3 | Duyệt JD `PENDING` → 200, `APPROVED`, `reviewedBy/At`, có dòng `APPROVE_JD`; sau đó xuất hiện ở `GET /public/jobs` | company | 1, 4 |
| 4 | Duyệt JD `REJECTED`/`APPROVED`/`CLOSED` → 409 `INVALID_JOB_STATUS`; `approve`/`reject`/`close` trên JD `DRAFT` → 409 `INVALID_JOB_STATUS` (bản nháp), id không có → 404, `GET /staff/jobs/{id}` của JD nháp → 404; duyệt JD `PENDING` đã quá hạn → 409 `JOB_EXPIRED`; không ghi lịch sử | company | 5 |
| 5 | Trả lại thiếu / rỗng / > 1000 lý do → 400 `errors.reason`; hợp lệ → `REJECTED`, `rejectReason`; HR `GET /jobs/{id}` thấy lý do, sửa và `submit` → `PENDING` | company | 2 |
| 6 | Đóng JD `APPROVED` có lý do → `CLOSED`, dòng `CLOSE_JD`; đóng JD khác → 409; thiếu lý do → 400 | company | 4 |
| 7 | Khoá công ty A: `LOCKED`, dòng `LOCK` kèm lý do, identity nhận đúng danh sách HR (lô ≤ 100) với `locked: true`; JD của A biến khỏi `GET /public/jobs`, JD của B còn; `GET /public/jobs/{idA}` → 404; HR của A gọi API company-service → 401 `ACCOUNT_DISABLED`; mở khoá → `ACTIVE`, `UNLOCK`, identity `locked: false`, JD hiện lại | company | 3, 4 |
| 8 | Khoá khi đã khoá / mở khoá khi đang hoạt động → 409; thiếu lý do khoá → 400; identity lỗi → 503 và công ty không đổi, không có dòng lịch sử | company | 3 |
| 9 | Lịch sử: đúng thứ tự mới nhất trước, có `staff.fullName`, `job.title`, `reason`; identity lỗi → 200 với `fullName` null; phân trang | company | 4 |
| 10 | Token HR/CANDIDATE gọi `/staff/jobs/**` → 403; token HR/CANDIDATE/ADMIN gọi lock/unlock/history → 403 (ADMIN **được** gọi `/staff/jobs/**`) | company | — |
| 11 | Xung đột `@Version` khi hai cán bộ cùng duyệt → một bên 409 | company | — |
| 12 | Job tự đóng: JD `APPROVED` hạn hôm qua → `CLOSED`; hạn hôm nay giữ nguyên; JD `PENDING` quá hạn giữ nguyên; chạy hai lần không lỗi | company | — |
| 13 | identity `company-lock`: thiếu / sai khoá nội bộ → 401; khoá → HR đăng nhập 403 `ACCOUNT_DISABLED`, token cũ 401, quên mật khẩu bị chặn, `lookup` `LOCKED` + `companyLocked`; mở khoá → đăng nhập lại được; HR đang khoá riêng vẫn bị khoá sau khi mở khoá công ty; mở khoá riêng không xoá cờ công ty; id không phải HR bị bỏ qua | identity | 3 |
| 14 | HR detail `GET /jobs/{id}` giữ hành vi DN-02 (404 công ty khác, chỉ HR) | company | — |
| 15 | Gateway chuyển `/api/v1/staff/jobs/**` và `/api/v1/public/**` tới company-service | gateway | — |
| 16 | Seeder: thêm 2 JD của Công ty B, đặt `reviewed_at` cho JD đã xử lý, chạy hai lần không trùng; migration V6 backfill `reviewed_at` | company | 6 |
| 17 | Khoá đồng thời hai lần → một lần thành công, một lần 409, chỉ một dòng `LOCK`; commit lỗi sau khi identity đã nhận → identity được gọi bù | company | 3 |
| 18 | `StaffHrAccount`: HR khoá theo công ty có `companyLocked = true`, `status` vẫn theo khoá riêng | company | — |

Frontend: `npm run type-check`, `npm run lint`, `npm run build`. `web/` có Playwright e2e (`npm run test:e2e`, `web/e2e/*.spec.ts`, cần stack chạy thật); không bắt buộc thêm e2e cho DN-07, nếu thêm thì một kịch bản duyệt JD → hiện ở trang việc làm.

Kiểm tra tay toàn luồng (dùng cho ảnh màn hình PR): Postgres, MailHog, mock, identity (dev), profile, company (dev), gateway, web. hr1 gửi duyệt một JD → staff1 mở `/staff/jobs` → thấy JD chờ của A và B → mở JD, trả lại không nhập lý do (bị chặn), nhập lý do → trả lại → hr1 thấy lý do trên `/hr/jobs/{id}`, sửa, gửi lại → staff1 duyệt → JD xuất hiện ở trang việc làm `/jobs` (trước khi duyệt thì không) → staff1 đóng một JD đang tuyển (nhập lý do) → staff1 khoá Công ty A (nhập lý do) → JD của A biến khỏi `/jobs` (JD của B còn), hr1 đăng nhập bị từ chối, phiên hr1 đang mở bị đưa về `/login` ở request kế tiếp → tab Lịch sử thao tác có đủ người, thời điểm, lý do → mở khoá → JD hiện lại, hr1 đăng nhập được → duyệt JD `DRAFT` qua API → 409 (và `GET /staff/jobs/{id}` của JD đó → 404). Ca job tự đóng kiểm bằng test (hoặc gọi tay phương thức qua test).

## 8. Tài liệu bàn giao

- `docs/api/company-openapi.yaml`: endpoint mục 4.3–4.6 (gồm `/public/**` của UI-01 và `POST /staff/jobs/{id}/close`), chi tiết JD của HR là `GET /jobs/{id}`, mã `JOB_EXPIRED`, `StaffCompanyDetail.lock`.
- `docs/api/identity-openapi.yaml`: `POST /internal/users/company-lock`, field `companyLocked` của `lookup`, hành vi `ACCOUNT_DISABLED` khi công ty bị khoá.
- `docs/api/DN-07-job-review.md` (mới, theo mẫu `DN-02-jobs.md`: mục tiêu, DB, API, vòng đời đầy đủ, khoá công ty, cách chạy, dữ liệu mẫu, ca kiểm thử, kết quả xác minh; không có mục ảnh màn hình — ảnh đính kèm PR) và `docs/api/DN-07.http`.
- Cập nhật `docs/database/erd.md`, `docs/architecture/service-conventions.md` (`HrLockedFilter` chặn cả công ty khoá, endpoint công khai `GET /public/**`, lịch chạy định kỳ), `docs/api/DN-02-jobs.md` + `DN-02.http` (chi tiết JD của HR vẫn là `GET /jobs/{id}`; bỏ ghi chú "duyệt thuộc DN-07, dùng SQL tạm"; `jdCount` của Công ty A mẫu là 6), `docs/api/DN-05-company-hr-ui.md` (khoá công ty, tab lịch sử) + `DN-05.http`, `docs/api/UT-01-applications.md` (trang việc làm dùng dữ liệu thật), README nếu lệnh chạy đổi.

## 9. Để người làm tự quyết

- Tên lớp, chia service/impl, tên DTO, tên file frontend.
- Cách viết quy tắc hiển thị (truy vấn JPQL có join hay `Specification`), miễn chỉ một chỗ và DN-04 dùng lại được.
- Cách lấy số lượng theo trạng thái cho tab của màn Duyệt JD.
- Nội dung chữ giao diện và thông báo (tiếng Việt, rõ ràng), chi tiết giao diện theo khung và `web/DESIGN.md` của UI-01.
