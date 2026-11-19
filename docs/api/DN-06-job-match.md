# DN-06 · Điểm khớp ứng viên với JD theo kỹ năng

| Mục | Giá trị |
| --- | --- |
| Mã task | DN-06 |
| Service | `company-service` (8085) qua `api-gateway` (8080); gọi nội bộ sang `profile-service` (8082) và `recruitment-service` (8083) |
| Frontend | `/staff/match-preview` (STAFF, không có trong menu, mở bằng URL); component `MatchScoreCard` cho DN-03 / DN-04 |
| Phụ thuộc | HS-01 (`skill_scores`), DN-02 (`jobs`, `job_skills`), UT-01 (`applications`), NT-02 (phân quyền) |
| OpenAPI | [company-openapi.yaml](company-openapi.yaml) (2 path `/jobs/{id}/match/...`) |
| Công thức | [docs/matching.md](../matching.md) (nguồn duy nhất; muốn đổi phải thống nhất cả nhóm và sửa file đó trước) |
| Request mẫu | [DN-06.http](DN-06.http) |
| Thiết kế | [spec DN-06](../superpowers/specs/2026-11-19-dn-06-job-match-design.md) |

## 1. Mục tiêu

Tính **mức khớp 0–100** giữa một ứng viên và một JD dựa trên kỹ năng đã xác minh, kèm giải thích: kỹ năng đạt (`met`), kỹ năng thiếu (`missing`) và cờ `belowMinimum`. Kỹ năng chỉ tự khai không được cộng điểm. Đây là phần lõi dùng chung: DN-03, DN-04, AI-01, DT-01 gọi API này; DN-03/DN-04 gắn thêm component `MatchScoreCard`.

Ngoài phạm vi: gắn thẻ vào trang của DN-03 / DN-04 / UT-01 / UT-02, tính hàng loạt hoặc xếp hạng ứng viên.

## 2. Cơ sở dữ liệu

Migration `company-service` `V4__match_scores.sql`, bảng `match_scores` (đệm kết quả tính, khoá chính `(job_id, user_id)`):

| Cột | Kiểu | Ghi chú |
| --- | --- | --- |
| `job_id` | UUID, khoá ngoại `jobs(id)` `ON DELETE CASCADE` | |
| `user_id` | UUID | `users.id` của identity, không có khoá ngoại chéo schema |
| `score` | INTEGER, `CHECK (score BETWEEN 0 AND 100)` | |
| `detail` | TEXT (JSON) | `belowMinimum`, `met`, `missing` và `inputs` (dữ liệu vào đã dùng: `skillId`, `required`, `minScore`, `verifiedScore`) |
| `stale` | BOOLEAN, mặc định false | Đánh `true` khi JD đổi danh sách kỹ năng |
| `computed_at` | TIMESTAMPTZ | |

**Không trả bản đệm cũ.** Mỗi lần gọi, company-service lấy `verified_score` hiện tại của ứng viên và chỉ dùng lại bản đệm khi `stale = false` **và** `inputs` đã lưu trùng với dữ liệu vào hiện tại (so số bằng `compareTo`, nên `70` và `70.000000` coi là bằng nhau). Ngược lại tính lại bằng `MatchCalculator` và ghi đè. Vì vậy đổi `min_score` hoặc điểm kỹ năng của ứng viên đều cho điểm mới ở lần gọi kế tiếp. `PUT /jobs/{id}` có `skills` và `PUT /jobs/{id}/skills` đánh `stale` trong cùng transaction (lưới an toàn, để DN-03 biết dòng nào cần tính lại). Bản đệm có `detail` hỏng hoặc đọc không được thì tính lại, không lỗi. **Lưu ý:** `stale = false` không có nghĩa là điểm đã lưu còn đúng (khi `verified_score` của ứng viên đổi thì không có gì đánh `stale`). DN-03 phải đọc điểm qua `MatchService` hoặc so `inputs`, không xếp hạng trực tiếp từ các dòng có `stale = false`.

## 3. API

Qua gateway `http://localhost:8080/api/v1` (gateway bỏ `/api/v1`; route `company` đã có `/api/v1/jobs/**`). Thành công trả thẳng DTO, lỗi là `{code, message, errors?}`.

### 3.1 API công khai (company-service)

| Endpoint | Vai trò | Thành công | Lỗi chính |
| --- | --- | --- | --- |
| `GET /jobs/{id}/match/me` | `CANDIDATE` | 200 `MatchResponse` | 404 `NOT_FOUND` (JD không tồn tại hoặc chưa `APPROVED`), 503 `EXTERNAL_UNAVAILABLE` |
| `GET /jobs/{id}/match/{userId}` | `HR`, `STAFF` | 200 `MatchResponse` | 404 `NOT_FOUND`, 503 `EXTERNAL_UNAVAILABLE` |

Mọi endpoint còn có 401 `UNAUTHORIZED` / `ACCOUNT_DISABLED`, 403 `FORBIDDEN` (sai vai trò), 403 `MUST_CHANGE_PASSWORD`.

Phản hồi 200 theo đúng hợp đồng UT-02 (`ut02-openapi.yaml` `Match`/`MatchSkill`), **không thêm trường nào khác**:

```json
{
  "score": 78,
  "belowMinimum": false,
  "met":     [{ "skill": "Java", "required": true, "minScore": 80, "candidateScore": 82.058824 }],
  "missing": [{ "skill": "Tiếng Anh", "required": false, "minScore": 60, "candidateScore": 0 }]
}
```

- `skill` là **tên** kỹ năng trong danh mục (không còn trong danh mục thì là chuỗi `skillId`); `candidateScore` luôn là số, không có điểm xác minh thì `0`, giữ phần thập phân như profile trả.
- `score` là số nguyên; thứ tự `met`/`missing`: bắt buộc trước, ưu tiên sau, cùng nhóm theo tên.
- Quy tắc truy cập của `{userId}`: **HR** chỉ xem được JD của công ty mình **và** ứng viên đã có đơn ứng tuyển vào JD đó (mọi trạng thái, kể cả `REJECTED`, `WITHDRAWN`); không thoả thì 404 `NOT_FOUND` "Không tìm thấy JD" giống JD không tồn tại. **STAFF** xem mọi JD, mọi ứng viên, không kiểm tra đơn ứng tuyển (kể cả JD `DRAFT`). `me` chỉ cho JD `APPROVED` để không lộ JD chưa công bố.
- Đường `me` được Spring ưu tiên hơn `{userId}` (đường tĩnh), và `me` không phải UUID nên không khớp `{userId}`.

### 3.2 API nội bộ mới (không qua gateway)

| Service | Endpoint | Trả về |
| --- | --- | --- |
| profile-service (8082) | `GET /internal/profiles/{userId}/verified-skills` | 200 `[{skillId, verifiedScore, updatedAt}]`, chỉ dòng có `verified_score` (bỏ `self_score`); không có thì `[]` |
| recruitment-service (8083) | `GET /internal/applications/exists?jobId=&userId=` | 200 `{applied: boolean}` (mọi trạng thái đơn); thiếu tham số → 400 `BAD_REQUEST` |

Cách bảo vệ giống `/internal/profiles/{id}/phone` đã có: company-service gửi kèm header `X-Internal-Key` **và chuyển tiếp nguyên `Authorization` của người gọi**. Endpoint đặt `@PreAuthorize("isAuthenticated()")` (profile: mọi vai trò vì đường `me` dùng token ứng viên; recruitment: `hasAnyRole('HR','STAFF')`, token ứng viên → 403) và so khoá trong controller bằng `MessageDigest.isEqual`; khoá cấu hình rỗng từ chối tất cả. Thiếu hoặc sai khoá, hoặc thiếu JWT → 401; đường dẫn mã hoá (`/%69nternal/...`) không qua được. Gateway không có route `/internal/**`.

Client trong company-service (`client/`): `HttpProfileSkillClient`, `HttpApplicationClient`, `HttpSkillCatalogClient` (nay trả cả `code` và `name`). `RestClient`, timeout 5 giây, không tự thử lại; lỗi mạng, 5xx hoặc 401 → 503 `EXTERNAL_UNAVAILABLE`. Lời gọi HTTP nằm ngoài transaction DB.

### 3.3 Ma trận quyền

| Endpoint | CANDIDATE | HR | MENTOR | STAFF | ADMIN |
| --- | --- | --- | --- | --- | --- |
| `GET /jobs/{id}/match/me` | JD `APPROVED` | 403 | 403 | 403 | 403 |
| `GET /jobs/{id}/match/{userId}` | 403 | JD công ty mình và ứng viên đã ứng tuyển | 403 | mọi JD, mọi ứng viên | 403 |

## 4. Giao diện

- Component `MatchScoreCard` (`web/src/app/components/match/match-score-card.tsx`): vòng tròn % bằng SVG (< 50 đỏ, 50–79 vàng, ≥ 80 xanh), banner "Thiếu kỹ năng bắt buộc tối thiểu" khi `belowMinimum`, danh sách "Kỹ năng đạt" và "Kỹ năng thiếu" (kèm `candidateScore / minScore` và "thiếu X điểm"), trạng thái tải, lỗi, và "JD chưa khai báo kỹ năng".
- Hook `useJobMatch(jobId, userId)` (HR/STAFF) và `useMyJobMatch(jobId)` (ứng viên) ở `web/src/hooks/use-job-match.ts`; service `web/src/services/match.service.ts`.
- Trang `/staff/match-preview` (STAFF): nhập JD id và ứng viên id hoặc bấm điền nhanh JD6 / JD7, bấm "Xem". Không có trong menu; dùng để chụp ảnh và làm mẫu cho DN-03 / DN-04.
- DN-06 **không sửa** thẻ riêng của UT-02 (`web/src/app/components/recruitment/match-card.tsx`) và không sửa code UT-02.

Dùng lại ở DN-03 / DN-04: `MatchScoreCard` nhận kết quả API làm props; lấy dữ liệu bằng hook ở trên.

## 5. Cách chạy

Cần Docker (Postgres, MailHog), Java 21 + Maven, Node 24. Mỗi lệnh chạy từ gốc repo, mỗi tiến trình dài một terminal. Các bước dựng chung giống [DN-05 mục 5](DN-05-company-hr-ui.md#5-cách-chạy).

```bash
cp .env.example .env                                                  # nếu chưa có
docker compose --env-file .env -f infra/compose.yaml up -d postgres mailhog
(cd mock && npm ci && npm start)                                      # mock LMS :9000
mvn -f server/pom.xml -pl services/identity-service,services/profile-service,services/company-service,services/recruitment-service,api-gateway -am install -DskipTests
```

Đặt biến môi trường **trong mỗi terminal chạy Java** (PowerShell: `$env:INTERNAL_SERVICE_KEY="dev-internal-key"`):

```bash
export INTERNAL_SERVICE_KEY=dev-internal-key          # company, profile VÀ recruitment (cùng giá trị)
mvn -f server/services/identity-service/pom.xml spring-boot:run "-Dspring-boot.run.profiles=dev"        # :8081, tài khoản ứng viên mẫu (chạy trước)
PROFILE_DEMO_DATA_ENABLED=true mvn -f server/services/profile-service/pom.xml spring-boot:run "-Dspring-boot.run.profiles=dev"   # :8082, điểm kỹ năng mẫu
mvn -f server/services/recruitment-service/pom.xml spring-boot:run                                       # :8083
mvn -f server/services/company-service/pom.xml spring-boot:run "-Dspring-boot.run.profiles=dev"         # :8085, seed JD6, JD7
mvn -f server/api-gateway/pom.xml spring-boot:run                                                       # :8080
(cd web && cp .env.example .env.local && npm ci && npm run dev -- --port 3000)                          # http://localhost:3000/staff/match-preview
```

Lưu ý:

- **`INTERNAL_SERVICE_KEY=dev-internal-key` phải đặt cho cả company, profile và recruitment.** Profile và recruitment mặc định khoá rỗng, mà khoá rỗng từ chối mọi lời gọi nội bộ, khi đó API điểm khớp trả 503 `EXTERNAL_UNAVAILABLE`. Địa chỉ company-service gọi tới: `PROFILE_SERVICE_URL` (mặc định `http://localhost:8082`) và `RECRUITMENT_SERVICE_URL` (mặc định `http://localhost:8083`). Khoá của recruitment đọc `RECRUITMENT_INTERNAL_SERVICE_KEY`, nếu trống thì dùng `INTERNAL_SERVICE_KEY`.
- profile-service phải chạy profile `dev` **và** `PROFILE_DEMO_DATA_ENABLED=true`, nếu không không có điểm kỹ năng mẫu. Identity chạy profile `dev` trước để có tài khoản ứng viên.
- **UUID ứng viên khác nhau theo từng database** (sinh ngẫu nhiên khi seed). Lấy bằng:

  ```sql
  select id, lms_user_id from public.users where role='CANDIDATE';
  ```

  `lms-sv-001` = C1, `lms-sv-002` = C2, `lms-sv-003` = C3. Điền vào `@c1`, `@c2`, `@c3` ở đầu `DN-06.http` (và vào ô ứng viên của `/staff/match-preview`).
- Trên PowerShell đặt `"-Dspring-boot.run.profiles=dev"` trong dấu nháy. Không dùng `-am spring-boot:run`.
- Tài khoản mẫu: `staff1` (SSO `lms-st-001`), `hr1@rikkeijobs.test` (Công ty A), mật khẩu `Test@123`; xem [DN-01 mục 6](DN-01-company-hr.md#6-dữ-liệu-mẫu).
- Đổi `min_score` để xem điểm đổi: dùng JD `DRAFT` hoặc `REJECTED` của Công ty A (mẫu: `…0003-000000000004` DRAFT). **Không sửa JD6/JD7 bằng HR**: sửa JD `APPROVED` sẽ đưa nó về `PENDING`, JD6/JD7 không còn là mẫu và `me` trả 404.
- Test backend: `mvn -f server/pom.xml clean test`. Frontend: `npm run type-check`, `npm run lint`, `npm run build` trong `web/`.

### Chạy test recruitment-service khi Testcontainers không tới được Docker

Máy dùng Docker Engine 29 với Testcontainers 1.19.8 có thể báo `Could not find a valid Docker environment`. Cách làm: dùng một DB riêng và trỏ test vào đó. Lệnh dưới là **PowerShell**; trên bash dùng `export UT01_TEST_DB_URL=...` tương ứng.

```sql
CREATE DATABASE rikkei_jobs_test;
```

```powershell
$env:UT01_TEST_DB_URL="jdbc:postgresql://localhost:5432/rikkei_jobs_test"
$env:UT01_TEST_DB_USER="postgres"; $env:UT01_TEST_DB_PASSWORD="postgres"
mvn -f server/pom.xml clean test "-Dspring.datasource.hikari.maximum-pool-size=2"   # tham số cuối cần khi Postgres báo "too many clients"
```

**Không bao giờ trỏ vào `rikkei_jobs`**: các test xoá dữ liệu.

## 6. Dữ liệu mẫu

JD mẫu (Công ty A, `APPROVED`, seed bởi `DevDataSeeder` của company-service khi chạy profile `dev`) và điểm kỹ năng (tính tay từ `profile-service/src/main/resources/hs01/candidates.json`; công thức và từng bước ở [docs/matching.md](../matching.md)):

| JD | Kỹ năng |
| --- | --- |
| **JD6** `00000000-0000-0000-0003-000000000006` | bắt buộc Java 80, Spring Boot 70, SQL 90; ưu tiên Docker 50, Tiếng Anh 60, Git 60 |
| **JD7** `00000000-0000-0000-0003-000000000007` | bắt buộc Java 60, SQL 60, Git 60 |

| Ứng viên | Java | Spring Boot | SQL | Git | Docker | Tiếng Anh |
| --- | --- | --- | --- | --- | --- | --- |
| C1 `lms-sv-001` | 82,058824 | 80,454545 | 81,153846 | 90 | — | chỉ tự khai 65 |
| C2 `lms-sv-002` | 65 | — | 65 | 80 | — | — |
| C3 `lms-sv-003` | 49,117647 | — | 58,846154 | — | — | — |

Ba ví dụ (request tương ứng trong `DN-06.http`):

| # | Cặp | Điểm mong đợi | `belowMinimum` | `met` | `missing` |
| --- | --- | --- | --- | --- | --- |
| 1 | JD6 × C1 | **78** (không tính tự khai 65; nếu tính sẽ là 88) | false | Java, Spring Boot, Git | SQL, Docker, Tiếng Anh |
| 2 | JD7 × C3 | **60** | true (Git 0 < 30) | rỗng | Git, Java, SQL |
| 3 | JD6 × ứng viên chưa có kỹ năng | **0** | true | rỗng | đủ 6 kỹ năng |

> Điểm của C1–C3 ở trên tính tay từ file seed. Nếu một ứng viên đã mở hồ sơ của mình, đồng bộ NT-05 có thể đã đổi điểm. Kiểm tra bằng `select * from profile.skill_scores where user_id = '<id>';` và cập nhật ví dụ nếu khác.

## 7. Ca kiểm thử và tiêu chí nghiệm thu

| # | Ca (spec mục 9) | Tiêu chí | Test tự động |
| --- | --- | --- | --- |
| 1 | Công thức: 3 ví dụ tính tay | 1 | `MatchCalculatorTest.example1BackendJdPartialMatch`, `example2RequiredOnlyBelowMinimum`, `example3NoSkills` |
| 2 | Ứng viên không có kỹ năng nào → 0, `missing` đủ | 2 | `MatchCalculatorTest.example3NoSkills`; `JobMatchApiTest.jobSkillsClearedGivesZero` (JD không kỹ năng → 0) |
| 3 | Kỹ năng chỉ tự khai không được cộng điểm | 3 | `MatchCalculatorTest.example1BackendJdPartialMatch` (Tiếng Anh tự khai 65 → 0, tổng 78 chứ không phải 88; công thức không có đường vào cho `self_score`); `InternalVerifiedSkillsApiTest.returnsOnlyVerifiedScores` (profile bỏ dòng chỉ có `self_score`) |
| 4 | Unit test công thức (JD chỉ bắt buộc / chỉ ưu tiên / rỗng, chặn ở 1, biên `belowMinimum`, làm tròn 0,5) | 5 | `MatchCalculatorTest`: `requiredOnlyUses100R`, `preferredOnlyUses100P`, `emptyJobIsZero`, `scoreAboveMinIsCappedAtOne`, `scoreEqualToMinIsMet`, `belowMinimumBoundary`, `roundsHalfUp`, `ignoresScoresOfSkillsNotInJob` |
| 5 | Candidate `match/me`: JD `APPROVED` → 200 đúng điểm; `DRAFT`/`PENDING` → 404 | — | `JobMatchApiTest.candidateMeOnApprovedJob`, `candidateMeOnUnpublishedJobIs404` |
| 6 | HR công ty sở hữu + đã ứng tuyển → 200; chưa ứng tuyển → 404; JD công ty khác → 404; STAFF → 200 không gọi recruitment (bằng chứng duy nhất của đường HR vì chưa có seed đơn ứng tuyển) | — | `hrOwnJobAppliedCandidate`, `hrNotAppliedIs404`, `hrOtherCompanyJobIs404`, `staffSeesAnyJobWithoutRecruitment`; thêm `wrongRolesAre403`, `unknownJobIs404` |
| 7 | Hợp đồng UT-02: đúng 4 trường, phần tử đúng 4 trường, `candidateScore` không null | — | `JobMatchApiTest.responseMatchesUt02Contract` |
| 8 | Đổi `min_score` qua `PUT /jobs/{id}/skills` → `stale = true`; gọi lại → điểm mới | 4 | `JobMatchApiTest.minScoreChangeGivesNewScore`, `putWithoutSkillsKeepsCacheFresh` |
| 9 | `verified_score` đổi → gọi lại → điểm mới, không trả bản cũ; bản đệm còn đúng thì dùng lại | 4 | `JobMatchApiTest.verifiedScoreChangeGivesNewScore`, `cachedResultReusedWhenInputsUnchanged`, `cacheHitIgnoresDecimalScale`, `unreadableDetailIsRecomputed` |
| 10 | profile / recruitment / danh mục tắt → 503; kỹ năng không còn trong danh mục → dùng `skillId` | — | `JobMatchApiTest.dependencyDownIs503`, `skillMissingFromCatalogFallsBackToId`, `blankCatalogNameFallsBackToId`; `HttpProfileSkillClientTest`, `HttpApplicationClientTest`, `HttpSkillCatalogClientTest` |
| 11 | Hai lần tính đầu cùng lúc đụng khoá chính vẫn trả kết quả | — | `MatchServiceImplTest.insertConflictStillReturnsResult` |
| 12 | profile `verified-skills`: chỉ dòng có `verified_score`; thiếu/sai khoá → 401; đường mã hoá không qua được | 3 | `InternalVerifiedSkillsApiTest`: `returnsOnlyVerifiedScores`, `noScoresGivesEmptyArray`, `wrongOrMissingKeyIs401`, `missingJwtIs401`, `encodedPathWithoutKeyIsRejected` |
| 13 | recruitment `exists`: có/không đơn, mọi trạng thái; thiếu/sai khoá → 401; token ứng viên → 403; thiếu tham số → 400 | — | `InternalApplicationExistsApiTest`: `appliedIsTrueForAnyStatus`, `notAppliedIsFalse`, `wrongOrMissingKeyIs401`, `candidateTokenIs403`, `missingParamIs400` |
| 14 | Seed JD6/JD7 chạy lại không trùng | — | `DevDataSeederTest` |

## 8. Trạng thái xác minh

- **Test backend tự động** (`mvn -f server/pom.xml clean test`, 2026-11-19): common-core 14, api-gateway 20, identity-service 266, profile-service 143 (1 bỏ qua, gồm `InternalVerifiedSkillsApiTest`), company-service 206 (gồm `MatchCalculatorTest`, `JobMatchApiTest`), internship / assessment / learning mỗi service 5 đều đạt. **recruitment-service 98/98 đạt** (gồm `InternalApplicationExistsApiTest`) ở commit `e91dbd0`, chạy với DB riêng `rikkei_jobs_test` qua `UT01_TEST_DB_*` và `-Dspring.datasource.hikari.maximum-pool-size=2` (cách ở mục 5). Toàn bộ backend xanh ở `e91dbd0`.
- **Chạy thật toàn luồng** (Postgres, mock, identity + profile + recruitment + company-service profile `dev`, gateway, web) và **ảnh màn hình `/staff/match-preview` cho 3 ví dụ**: **chờ chạy thật**. Người làm chạy theo mục 5 và `DN-06.http`, so điểm với mục 6, rồi chụp ảnh vào `docs/api/screenshots/DN-06/` (đường dẫn dự kiến):
  - `match-example-1-jd6-c1.png` (78), `match-example-2-jd7-c3.png` (60), `match-example-3-jd6-no-skills.png` (0).
- `web/`: chạy `type-check`, `lint`, `build` trước khi mở PR (CI chỉ chạy `type-check`).

## 9. Ghi chú cho UT-02, DN-03, DN-04

- **UT-02** (`recruitment-service` `HttpJobMatchClient`) đã gọi sẵn `GET /jobs/{id}/match/{userId}` với đúng hợp đồng ở mục 3.1; DN-06 **không sửa** UT-02. Test `responseMatchesUt02Contract` canh để DN-06 không làm vỡ hợp đồng đó. Không thêm trường vào phản hồi mà không sửa UT-02.
- **DN-03 / DN-04**: gọi cùng API, hoặc dùng `MatchCalculator` nếu cần tính hàng loạt trong company-service; gắn `MatchScoreCard` và hook `useJobMatch`. Đổi công thức phải sửa [docs/matching.md](../matching.md) trước.
- **Gọi vòng giữa recruitment và company**: trang chi tiết đơn của HR (UT-02, recruitment-service) gọi company `GET /jobs/{id}/match/{userId}`, rồi company gọi ngược recruitment `GET /internal/applications/exists`. Khi tải đồng thời cao, lời gọi ngược có thể quá timeout 5 giây, company trả 503, UT-02 hiển thị `match: null`. Chỉ suy giảm hiển thị, không có deadlock; không cần sửa code UT-02.

## 10. Checklist nộp PR

- [ ] Nhánh `feature/DN-06-job-match`, PR **vào `test`** (không vào `main`), tiêu đề `[DN-06] Điểm khớp ứng viên với JD theo kỹ năng`
- [x] Có: cách chạy, dữ liệu mẫu, OpenAPI (`company-openapi.yaml`), request mẫu `DN-06.http`, công thức và 3 ví dụ ở `docs/matching.md`, migration `V4__match_scores.sql`
- [x] 3 ví dụ tính tay khớp kết quả API (test); 0 điểm khi không có kỹ năng; tự khai không cộng điểm; đổi `min_score` ra điểm mới; unit test công thức
- [ ] Chạy thật toàn luồng và chụp ảnh `/staff/match-preview` cho 3 ví dụ vào `docs/api/screenshots/DN-06/` (chờ chạy thật)
- [ ] Tự chạy `npm run lint` và `npm run build` ở `web/` (CI chỉ chạy `type-check`)
- [ ] Một bạn trong nhóm review trước, mentor duyệt PR
