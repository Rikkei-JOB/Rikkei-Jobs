# DN-06 · Điểm khớp ứng viên với JD theo kỹ năng — Thiết kế

Task: **[Doanh nghiệp & JD · DN-06] Điểm khớp ứng viên với JD theo kỹ năng (API)**. Phụ thuộc HS-01 (`skill_scores`), DN-02 (`jobs`, `job_skills`), UT-01 (`applications`), NT-02 (guard, springdoc). Nhánh `feature/DN-06-job-match` từ `test`, PR vào **`test`** (đề ghi "develop" là ghi nhầm), tiêu đề `[DN-06] Điểm khớp ứng viên với JD theo kỹ năng`.

Ký hiệu: **(chốt)** người giao task đã quyết; **(người thiết kế chọn)** quyết định trong lúc thiết kế, người làm có thể nêu lại nếu thấy sai.

## 1. Mục đích

Tính **mức khớp 0–100** giữa một ứng viên và một JD dựa trên kỹ năng, kèm giải thích (kỹ năng đạt, kỹ năng thiếu, cờ `belowMinimum`). Đây là phần lõi dùng chung: DN-03, DN-04, AI-01, DT-01 sẽ gọi API này và gắn component thẻ điểm khớp.

### Tiêu chí nghiệm thu (chốt)

1. PR có ≥ 3 ví dụ tính tay, khớp kết quả API.
2. Ứng viên không có kỹ năng nào → 0 điểm, `missing` liệt kê đủ.
3. Kỹ năng chỉ tự khai không được cộng điểm.
4. Đổi `min_score` của JD → gọi lại ra điểm mới, không trả bản đệm cũ.
5. Có unit test cho công thức.
6. PR vào `test` có cách chạy, dữ liệu mẫu, ảnh màn hình; OpenAPI cập nhật endpoint mới.
7. Một bạn trong nhóm review trước, mentor duyệt PR.

### Ngoài phạm vi

- Gắn thẻ điểm khớp vào trang của DN-03 / DN-04 / UT-01 / UT-02 (chốt: DN-06 chỉ làm component; các task đó tự gắn).
- Tính hàng loạt / xếp hạng ứng viên theo điểm (DN-03 làm, dùng lại API hoặc lớp tính điểm).
- Đổi công thức: phải thống nhất cả nhóm và sửa `docs/matching.md` trước.

## 2. Cách làm đã chọn

| Câu hỏi | Các cách đã xét | Chọn |
| --- | --- | --- |
| Điểm khớp nằm ở đâu | company-service (giữ JD) / profile-service (giữ điểm kỹ năng) / service mới | **company-service** (người thiết kế chọn): JD, `job_skills`, quyền HR theo công ty đều ở đây; chỉ cần lấy điểm kỹ năng từ profile-service |
| Đánh `stale` khi `skill_scores` của ứng viên đổi (ở profile-service) | **(a)** company-service kiểm tra lại mỗi lần đọc; (b) profile-service báo sang company-service | **(a)** (chốt): luôn đúng ("không trả bản đệm cũ"), không sửa logic HS-01; đổi lại mỗi lần đọc tốn một lời gọi nội bộ |
| HR xem điểm của ai | mọi ứng viên / chỉ ứng viên đã ứng tuyển JD | **chỉ ứng viên đã ứng tuyển vào JD đó** (chốt) |
| Giao diện | gắn vào trang UT-01/UT-02 / chỉ component | **chỉ component** + trang `/staff/match-preview` để xem mẫu và chụp ảnh (chốt) |
| Hợp đồng trả về (UT-02 **đã gọi sẵn** `GET /jobs/{id}/match/{userId}` qua `recruitment-service HttpJobMatchClient`, kiểm tra chặt theo `docs/api/ut02-openapi.yaml` `Match`/`MatchSkill`) | **(A)** theo đúng hợp đồng UT-02; (B) đổi hợp đồng và sửa UT-02 | **(A)** (chốt): không sửa code UT-02; `skill` là chuỗi, `candidateScore` luôn là số (0 khi chưa có) |
| Dữ liệu mẫu cho đường HR (cần đơn ứng tuyển; recruitment-service chưa có seeder, nộp đơn thật cần CV của HS-04 chưa có) | (a) thêm seeder đơn ứng tuyển ở recruitment-service; **(b)** chỉ chứng minh bằng test tự động | **(b)** (chốt): đường HR chứng minh bằng test MockMvc; ảnh màn hình chụp ở `/staff/match-preview` (STAFF không cần đơn ứng tuyển) |

## 3. Công thức (chốt, ghi nguyên vào `docs/matching.md`)

Với mỗi kỹ năng của JD (`job_skills`: `required` bắt buộc/ưu tiên, `min_score` 1–100):

- `v` = `verified_score` của ứng viên cho kỹ năng đó (profile-service, 0–100). Không có → `v = 0`. **`self_score` (tự khai) không bao giờ được dùng.**
- Điểm kỹ năng `s = min(v / min_score, 1)`.
- `R` = trung bình `s` của các kỹ năng bắt buộc, `P` = trung bình `s` của các kỹ năng ưu tiên.
- Điểm khớp:
  - có cả bắt buộc và ưu tiên: `70 × R + 30 × P`;
  - không có ưu tiên: `100 × R`;
  - không có bắt buộc, có ưu tiên: `100 × P` (người thiết kế chọn);
  - JD không có kỹ năng nào: `0`, `met` và `missing` rỗng (người thiết kế chọn).
- `belowMinimum = true` khi có ít nhất một kỹ năng **bắt buộc** với `v < 0.5 × min_score`.
- API trả `score` **làm tròn số nguyên** (0,5 làm tròn lên, `RoundingMode.HALF_UP`) (người thiết kế chọn); tính trung gian bằng `BigDecimal` hoặc `double`, chỉ làm tròn ở bước cuối.
- Kỹ năng **đạt** (`met`): `v ≥ min_score` (tức `s = 1`); kỹ năng **thiếu** (`missing`): `v < min_score`.

`docs/matching.md` gồm: công thức trên, bảng dữ liệu vào, **3 ví dụ tính tay** từng bước (xem §7) và câu "muốn đổi công thức phải thống nhất cả nhóm".

## 4. Dữ liệu

### 4.1 company-service — `V4__match_scores.sql`

```sql
CREATE TABLE match_scores (
    job_id      UUID                     NOT NULL,
    user_id     UUID                     NOT NULL,
    score       INTEGER                  NOT NULL,
    detail      TEXT                     NOT NULL,   -- JSON (xem dưới)
    stale       BOOLEAN                  NOT NULL DEFAULT FALSE,
    computed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_match_scores PRIMARY KEY (job_id, user_id),
    CONSTRAINT fk_match_scores_job FOREIGN KEY (job_id) REFERENCES jobs (id) ON DELETE CASCADE,
    CONSTRAINT ck_match_scores_score CHECK (score BETWEEN 0 AND 100)
);
```

- `detail` lưu dạng `TEXT` chứa JSON (người thiết kế chọn: chạy được cả PostgreSQL lẫn H2; không cần truy vấn bên trong JSON). Nội dung: kết quả trả về (`belowMinimum`, `met`, `missing`) **và dữ liệu vào** đã dùng để tính: `inputs: [{skillId, required, minScore, verifiedScore|null}]`.
- `job_skills` có sẵn từ DN-02 (chốt, bảng chung), không đổi.

### 4.2 Dữ liệu mẫu

- Dùng điểm kỹ năng seed sẵn của HS-01 (`lms-sv-001` có một kỹ năng **chỉ tự khai**, dùng cho tiêu chí 3).
- Seed DN-02 hiện chỉ có một JD `APPROVED` (Tester, một kỹ năng bắt buộc), không đủ cho §7. **Thêm vào `DevDataSeeder` của company-service** (profile `dev`, id cố định, chạy lại không trùng) hai JD `APPROVED` mẫu (người thiết kế chọn): một JD có cả kỹ năng bắt buộc và ưu tiên, một JD chỉ có kỹ năng bắt buộc với `min_score` đủ cao để có ví dụ `belowMinimum` (chốt: "chưa có JD thật thì seed job_skills mẫu"). Kỹ năng chọn theo điểm seed thật của các ứng viên mẫu.
- Không seed đơn ứng tuyển (chốt (b)); đường HR được chứng minh bằng test tự động.

## 5. API

Lỗi theo `ErrorResponse {code, message, errors?}`, tiếng Việt. Qua gateway `http://localhost:8080/api/v1` (route `company` đã có `/api/v1/jobs/**`).

### 5.1 Public (company-service)

**`GET /jobs/{id}/match/me`** — `@PreAuthorize(Roles.CANDIDATE)`

- Chỉ JD có `status = APPROVED` (người thiết kế chọn); khác → 404 `NOT_FOUND` (không lộ JD chưa công bố).

**`GET /jobs/{id}/match/{userId}`** — `@PreAuthorize("hasAnyRole('HR','STAFF')")`

- HR: JD phải thuộc công ty của HR (cách xác định công ty và trả 404 như `GET /jobs/{id}` của DN-02 / `OwnershipGuard` NT-02) **và** `userId` phải có đơn ứng tuyển vào JD này (§5.3, mọi trạng thái kể cả `REJECTED`, `WITHDRAWN` — người thiết kế chọn). Không thoả → **404** `NOT_FOUND`, body như id không tồn tại.
- STAFF: mọi JD, mọi ứng viên, không kiểm tra ứng tuyển (người thiết kế chọn).
- Đường `me` phải được ưu tiên hơn `{userId}` (Spring ưu tiên đường tĩnh; `me` không phải UUID nên `{userId}` kiểu `UUID` cũng không khớp).

**Response 200** (cả hai) — **đúng hợp đồng UT-02 đã dùng** (chốt (A); `docs/api/ut02-openapi.yaml` `Match`, `MatchSkill`; record `MatchResponse` của recruitment-service):

```json
{
  "score": 72,
  "belowMinimum": false,
  "met":     [{ "skill": "Java",   "required": true,  "minScore": 70, "candidateScore": 85 }],
  "missing": [{ "skill": "Docker", "required": false, "minScore": 60, "candidateScore": 0 }]
}
```

- **Không thêm trường nào khác** (không `jobId`, `userId`, `computedAt`) (người thiết kế chọn: client UT-02 kiểm tra chặt, thêm trường là rủi ro không cần thiết).
- `skill`: **tên** kỹ năng trong danh mục (ví dụ "Spring Boot") (người thiết kế chọn: UT-02 hiển thị thẳng chuỗi này).
- `candidateScore`: luôn là số; không có `verified_score` → `0` (đúng câu "không có = 0" của đề). Giữ phần thập phân như profile trả.
- Thứ tự trong `met` / `missing`: bắt buộc trước, ưu tiên sau, cùng nhóm theo tên (người thiết kế chọn).
- Tên kỹ năng lấy từ danh mục qua `SkillCatalogClient` (DN-02). Client này hiện chỉ trả id — **mở rộng để trả cả `code` và `name`** (người thiết kế chọn), giữ nguyên hành vi kiểm tra id của DN-02. Danh mục lỗi → 503 `EXTERNAL_UNAVAILABLE`.
- Test hợp đồng ở company-service: JSON trả về có đúng 4 trường trên, mỗi phần tử đúng 4 trường, `candidateScore` không null — để DN-06 không làm vỡ UT-02.

### 5.2 Luồng tính (`MatchService.getOrCompute(jobId, userId)`)

1. Đọc `job_skills` của JD.
2. Lấy `verified_score` hiện tại của ứng viên qua §5.3 (profile-service). Lỗi/tắt → 503 `EXTERNAL_UNAVAILABLE`.
3. Đọc `match_scores(job_id, user_id)`. Nếu có, `stale = false` **và** `inputs` trong `detail` trùng với dữ liệu vào hiện tại (cùng tập kỹ năng, `required`, `minScore`, `verifiedScore`) → trả bản đã lưu.
4. Ngược lại: tính bằng lớp thuần `MatchCalculator` (§6), ghi đè `match_scores` (`stale = false`, `computed_at = now` từ `Clock`), trả kết quả.

**Đánh stale (chốt):** mọi chỗ DN-02 thay kỹ năng của JD (`PUT /jobs/{id}` có `skills`, `PUT /jobs/{id}/skills`) gọi `matchScoreRepository.markStaleByJobId(jobId)` **trong cùng transaction**. (Bước 3 đã tự phát hiện đổi `min_score` qua `inputs`; `stale` là lưới an toàn và để DN-03 biết dòng nào cần tính lại.) Thay đổi `skill_scores` được phát hiện ở bước 3 (cách (a)).

Gọi HTTP không nằm trong transaction DB đang mở (như DN-01/DN-05).

### 5.3 API nội bộ mới (không qua gateway, header `X-Internal-Key`, `INTERNAL_SERVICE_KEY`)

- **profile-service `GET /internal/profiles/{userId}/verified-skills`** → `200 [{skillId, verifiedScore, updatedAt}]`, chỉ các dòng `verified_score` khác null. Không có → `[]`.
- **recruitment-service `GET /internal/applications/exists?jobId=&userId=`** → `200 {applied: boolean}` (mọi trạng thái đơn). Thiếu tham số → 400.
- **Cách bảo vệ (người thiết kế chọn): theo đúng mẫu `/internal/profiles/{id}/phone` đã có**, không dựng lại chuỗi bảo mật của profile-service / recruitment-service: company-service gửi kèm `X-Internal-Key` **và chuyển tiếp nguyên header `Authorization` của người gọi** (như `HttpCandidateContactClient` của recruitment). Endpoint đặt `@PreAuthorize("isAuthenticated()")` (profile: mọi vai trò vì đường `me` dùng token ứng viên; recruitment: `hasAnyRole('HR','STAFF')`) và kiểm tra khoá trong controller bằng `MessageDigest.isEqual`, khoá cấu hình rỗng từ chối tất cả. Gateway không có route `/internal/**`.
- Khoá: profile-service đọc `app.profile.internal-service-key` (`INTERNAL_SERVICE_KEY`, mặc định rỗng); recruitment thêm thuộc tính tương tự. Không đổi mặc định hiện có; tài liệu DN-06 ghi rõ phải đặt `INTERNAL_SERVICE_KEY` giống nhau ở company, profile, recruitment khi chạy local (người thiết kế chọn).
- Client ở company-service (`client/`): `ProfileSkillClient.verifiedSkills(UUID userId)`, `ApplicationClient.hasApplied(UUID jobId, UUID userId)`; `RestClient` + `JdkClientHttpRequestFactory`, timeout 5 s, không tự thử lại; lỗi mạng/5xx/401 → `CompanyException(EXTERNAL_UNAVAILABLE)`. Biến môi trường mới: `PROFILE_SERVICE_URL` (mặc định `http://localhost:8082`), `RECRUITMENT_SERVICE_URL` (mặc định `http://localhost:8083`) — trùng tên biến gateway đang dùng.

## 6. Lớp tính điểm dùng chung

`MatchCalculator` (company-service, không phụ thuộc Spring/DB): `MatchResult calculate(List<JobSkillInput> jobSkills, Map<UUID, BigDecimal> verifiedScores)`. Mọi API và DN-03 sau này dùng lớp này; AI-01 / DT-01 ở service khác gọi API (người thiết kế chọn: không đưa công thức vào `common-core` khi chưa có service thứ hai cần tính tại chỗ).

Unit test `MatchCalculatorTest` (tiêu chí 5): 3 ví dụ ở §7, không kỹ năng → 0 và `missing` đủ, chỉ tự khai → không cộng (truyền `self_score` không có đường vào), JD chỉ bắt buộc / chỉ ưu tiên / rỗng, `v > min_score` bị chặn ở 1, `belowMinimum` ở biên (`v = 0.5 × min_score` → không bật; nhỏ hơn → bật), làm tròn 0,5.

## 7. Ví dụ tính tay (tiêu chí 1)

Ba ví dụ dùng **dữ liệu mẫu thật** (JD seed + điểm seed của ứng viên), ghi trong `docs/matching.md` từng bước, mỗi ví dụ có request tương ứng trong `docs/api/DN-06.http` và test trong `MatchCalculatorTest` cho ra đúng số đó:

1. JD có cả kỹ năng bắt buộc và ưu tiên, ứng viên đạt một phần.
2. JD chỉ có kỹ năng bắt buộc, có một kỹ năng dưới 50% `min_score` → `belowMinimum = true`.
3. Ứng viên không có kỹ năng nào (hoặc chỉ tự khai, ví dụ `lms-sv-001` với kỹ năng tự khai) → 0, `missing` đủ.

Người làm đọc giá trị seed thực tế để tính; nếu seed thay đổi thì cập nhật cả tài liệu và test.

## 8. Frontend

- Component **`MatchScoreCard`** (`web/src/app/components/match/match-score-card.tsx`), props là kết quả API:
  - vòng tròn % bằng SVG (không thêm thư viện); màu theo điểm: < 50 đỏ, 50–79 vàng, ≥ 80 xanh (người thiết kế chọn);
  - banner "Thiếu kỹ năng bắt buộc tối thiểu" khi `belowMinimum`;
  - "Kỹ năng đạt" (xanh): tên, (bắt buộc/ưu tiên), `candidateScore / minScore`;
  - "Kỹ năng thiếu" (đỏ): tên, (bắt buộc/ưu tiên), `candidateScore / minScore`, "thiếu X điểm" với `X = minScore − candidateScore` làm tròn lên số nguyên;
  - UT-02 đã có thẻ riêng `web/src/app/components/recruitment/match-card.tsx`; DN-06 **không sửa** thẻ đó (chốt (A)), chỉ thêm `MatchScoreCard` mới cho DN-03/DN-04. Kiểu dữ liệu TypeScript dùng lại hoặc tương thích với `MatchScore` trong `web/src/interface/hr-application.ts`;
  - trạng thái tải, lỗi (dùng `getErrorMessage`), JD không có kỹ năng ("JD chưa khai báo kỹ năng").
- Hook (`web/src/hooks/use-job-match.ts`): `useJobMatch(jobId, userId)` (HR/STAFF), `useMyJobMatch(jobId)` (ứng viên); TanStack Query, khoá cache có user id đang đăng nhập; service `web/src/services/match.service.ts`.
- Trang **`/staff/match-preview`** (STAFF, chốt): ô nhập JD id và ứng viên id (hoặc chọn từ dữ liệu mẫu), nút "Xem", hiện `MatchScoreCard`. Dùng để chụp ảnh và làm mẫu cho DN-03/DN-04. Không thêm vào menu chính (người thiết kế chọn; mở bằng URL, ghi trong tài liệu).
- Đọc `web/AGENTS.md` trước khi viết code Next.

## 9. Kiểm thử

| Ca | Ở đâu | Tiêu chí |
| --- | --- | --- |
| `MatchCalculatorTest` (§6) | company (unit) | 1, 2, 3, 5 |
| Candidate `match/me`: JD `APPROVED` → 200 đúng điểm; JD `DRAFT`/`PENDING` → 404 | company (MockMvc, mock client) | — |
| HR công ty sở hữu + ứng viên đã ứng tuyển → 200; chưa ứng tuyển → 404; JD công ty khác → 404; STAFF → 200 không gọi recruitment (đây là bằng chứng duy nhất của đường HR, chốt (b)) | company | — |
| Hợp đồng UT-02: JSON đúng 4 trường `score, belowMinimum, met, missing`, phần tử đúng `skill` (chuỗi), `required`, `minScore`, `candidateScore` (không null) | company | — |
| Đổi `min_score` qua `PUT /jobs/{id}/skills` → `match_scores.stale = true`; gọi lại → điểm mới | company | 4 |
| `verified_score` đổi (mock profile trả giá trị khác) → gọi lại → điểm mới, không trả bản cũ | company | 4 |
| profile / recruitment / danh mục tắt → 503 | company | — |
| profile `GET /internal/profiles/{id}/verified-skills`: chỉ trả dòng có `verified_score`, bỏ `self_score`; thiếu/sai khoá → 401; đường mã hoá (`/%69nternal/...`) → 401 | profile | 3 |
| recruitment `GET /internal/applications/exists`: có/không đơn, mọi trạng thái; thiếu/sai khoá → 401 | recruitment | — |

Frontend: `type-check`, `lint`, `build`; kiểm tay trên `/staff/match-preview`.

## 10. Tài liệu bàn giao

- `docs/matching.md` (§3, §7).
- `docs/api/DN-06-job-match.md` (theo mẫu DN-05: mục tiêu, DB, API, quyền, cách chạy, dữ liệu mẫu, ca kiểm thử, trạng thái xác minh, checklist) và `docs/api/DN-06.http`.
- springdoc cho 2 endpoint public; `docs/api/company-openapi.yaml` cập nhật; endpoint nội bộ ghi trong tài liệu DN-06 (và OpenAPI tham khảo của service tương ứng nếu có).
- `docs/architecture/service-conventions.md`: thêm hai API nội bộ mới.
- Ảnh màn hình `/staff/match-preview` (3 ví dụ) theo tiêu chí 6.

## 11. Để người làm tự quyết

- Tên lớp DTO, cách chia service/impl, cách so `inputs` (so danh sách đã sắp xếp hay map).
- Cách serialize `detail` (Jackson `ObjectMapper` sẵn có).
- Câu chữ thông báo, chi tiết giao diện trong khuôn §8.
