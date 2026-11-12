# UT-01 Candidate Applications Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task; use superpowers:subagent-driven-development only if that execution method is selected. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Triển khai DB, BE, FE để ứng viên nộp CV, xem đơn và lịch sử, rút đơn trước OFFER; HR nhận email và xem đúng PDF đã nộp.

**Architecture:** `recruitment-service` sở hữu đơn, lịch sử, snapshot và outbox email; gọi HS-04 để xuất PDF, DN-04/company-service để kiểm tra JD và lấy công ty/HR. Tạo snapshot trước transaction ngắn ghi đơn + lịch sử + outbox; dùng unique index và khoá hàng để kiểm soát request đồng thời. FE gắn component vào trang JD của DN-04 và thêm trang ứng tuyển riêng.

**Tech Stack:** Java 21, Spring Boot 3.3.4, JPA, PostgreSQL, Flyway, RestClient, Spring Mail, JUnit 5, Testcontainers PostgreSQL; Next.js/React/TypeScript/Tailwind/Axios hiện có; Playwright cho hành vi trình duyệt.

**Spec:** [Yêu cầu UT-01 và phụ thuộc HS-04/DN-04](../specs/2026-11-12-ut01-requirements.md), [quy ước chung](../../conventions.md), [quy ước service](../../architecture/service-conventions.md). Đây là kế hoạch theo yêu cầu của người dùng; các đề xuất ở mục 5 của spec chưa được xác nhận riêng.

## Global Constraints

- Giữ `server/`, `web/`, JWT/common-core; không thay framework hoặc dựng lại các module phụ thuộc.
- API `/api/v1`, JSON camelCase; lỗi `{code, message}`, phân trang từ 1, mặc định size 20, tối đa 100; timestamp ISO 8601 UTC.
- Enum: SUBMITTED, SCREENING, INTERVIEW, OFFER, HIRED, REJECTED, WITHDRAWN.
- Ứng viên chỉ thao tác đơn và CV của mình; HR chỉ đọc snapshot của công ty mình; userId/actorId lấy từ principal.
- Không FK xuyên service; Flyway và JPA dùng schema `recruitment`; giữ `ddl-auto: validate`.
- Không xác nhận nộp thành công nếu PDF chưa được lưu; status/history/outbox commit hoặc rollback cùng nhau.
- Cho nộp lại cùng JD sau WITHDRAWN hoặc REJECTED; người dùng đã xác nhận trường hợp REJECTED. Mỗi lần nộp lại tạo đơn mới/SUBMITTED và PDF mới, giữ nguyên đơn cũ. Sau HIRED vẫn chặn theo đề xuất hiện tại.
- PR base `develop`, tiêu đề `[UT-01] ...` theo yêu cầu trực tiếp. Không tự đổi base thành `test` khi thiếu develop.
- Bước hiện tại chỉ lập tài liệu. Các lệnh build, cài dependency, commit và PR dưới đây dành cho giai đoạn triển khai.

## Review Focus

1. Hai tab cùng nộp: đúng một đơn; request còn lại 409, không để lại history/email/file của đơn thất bại — Task 2, 4.
2. JD hết hạn trong lúc xuất PDF, hoặc HR chuyển OFFER khi ứng viên rút: không vượt qua điều kiện tại lúc ghi đơn/đổi trạng thái — Task 4, 5.
3. Sửa/xoá CV gốc, rút công khai QR hoặc JD không còn hiển thị: PDF đã nộp vẫn đọc được theo quyền, QR vẫn giữ chính sách HS-04 — Task 3, 9.
4. Đoán applicationId, giả cvId, hoặc HR khác công ty: không đọc đơn/PDF hoặc làm lộ email HR — Task 3, 4, 5.
5. SMTP lỗi hoặc service restart sau commit: đơn vẫn thành công và email có thể retry; outbox rollback không gửi — Task 6.

## Lựa chọn kiến trúc phục vụ kế hoạch

| Cách | Đánh đổi | Đề xuất |
| --- | --- | --- |
| Xuất PDF và lưu snapshot ngay trong request | API chờ renderer; chỉ báo thành công khi có bản nộp hoàn chỉnh | Chọn cho UT-01 |
| Nhận đơn rồi xuất PDF bất đồng bộ | Cần trạng thái xử lý mới, có nguy cơ lấy hồ sơ đã sửa sau khi nộp | Chưa dùng |
| Chỉ lưu URL `/cvs/{id}/pdf` | Nội dung URL thay đổi theo hồ sơ, không đạt nghiệm thu | Loại |

Thứ tự triển khai: hợp đồng → DB/luật trạng thái → adapter snapshot → apply → đọc/rút → email → FE ứng tuyển → FE theo dõi → tích hợp/bàn giao. Có thể hoàn thành core bằng fixture, nhưng không công bố hoàn thành UT-01 trước tích hợp thật.

## Bản đồ file và ranh giới

Ký hiệu đường dẫn dùng dưới đây, đều tính từ root repo:

- `R = server/services/recruitment-service`
- `J = server/services/recruitment-service/src/main/java/com/rikkei/jobs/recruitment`
- `T = server/services/recruitment-service/src/test/java/com/rikkei/jobs/recruitment`
- `W = web/src`

| Nhóm | File tạo/sửa | Trách nhiệm |
| --- | --- | --- |
| Persistence | `J/entity/{Application,ApplicationStatus,ApplicationStatusHistory,ApplicationNotificationOutbox}.java`; `J/repository/{ApplicationRepository,ApplicationStatusHistoryRepository,ApplicationNotificationOutboxRepository}.java` | Đơn, lịch sử append-only, hàng đợi email |
| Migration/config | `R/pom.xml`; `R/src/main/resources/application.yml`; `R/src/main/resources/db/migration/V1__applications.sql`; `J/config/RecruitmentConfiguration.java` | Flyway schema, dependencies, Clock, storage/SMTP |
| Luật nghiệp vụ | `J/service/{ApplicationStatusPolicy,ApplicationService,ApplicationWriteService,ApplicationTransitionService}.java`; `J/service/impl/{ApplicationServiceImpl,ApplicationWriteServiceImpl,ApplicationTransitionServiceImpl}.java` | Orchestration, transaction ghi đơn, transaction chuyển trạng thái |
| Adapter | `J/client/{JobApplicationClient,ProfileCvClient,HttpJobApplicationClient,HttpProfileCvClient}.java`; `J/service/{CvSnapshotStorage,LocalCvSnapshotStorage}.java`; `J/dto/integration/{JobApplicationContext,CvPdf}.java` | Hợp đồng liên service và lưu PDF |
| API | `J/controller/{CandidateApplicationController,ApplicationSnapshotController}.java`; `J/dto/request/{ApplyRequest,WithdrawRequest}.java`; `J/dto/response/{ApplicationSummaryResponse,ApplicationDetailResponse,ApplicationHistoryResponse}.java`; `J/mapper/ApplicationMapper.java` | DTO, quyền truy cập, phân trang |
| Email | `J/service/{ApplicationMailSender,ApplicationOutboxWorker}.java`; `J/service/impl/SmtpApplicationMailSender.java` | Claim outbox, gửi email, retry |
| FE apply | `W/interface/application.ts`; `W/services/application.service.ts`; `W/app/components/applications/{apply-button,apply-dialog}.tsx` | Hợp đồng FE và hộp ứng tuyển |
| FE tracking | `W/app/(pages)/(app)/candidate/applications/page.tsx`; `W/app/(pages)/(app)/candidate/applications/[id]/page.tsx`; `W/app/components/applications/{application-card,application-status-badge,application-progress,application-history,withdraw-dialog}.tsx`; `W/config/application-status.ts` | Danh sách, chi tiết, lịch sử, rút |
| Integration | `server/api-gateway/src/main/resources/application.yml` chỉ khi thiếu route; `W/app/(pages)/(app)/[role]/page.tsx`; file trang JD thật do DN-04 cung cấp | Đường dẫn gateway, menu, gắn nút |
| Verification | `T/*Test.java`; `web/e2e/ut01-*.spec.ts`; `web/playwright.config.ts`; `docs/api/UT-01-applications.md`; `.github/workflows/ci.yml`; `.env.example` | Test, tài liệu, CI |

File `web/src/services/cv.service.ts`, route CV và trang JD chưa tồn tại trong checkout này: dùng file của HS-04/DN-04 khi tích hợp; không tạo bản production cạnh tranh trong UT-01.

## Task 1: Chốt hợp đồng tích hợp và bộ tiêu chí

**Files:** Create `docs/api/UT-01-applications.md`; chuẩn bị fixture `R/src/test/resources/ut01/{job-context.json,cv-before.pdf,cv-after.pdf}`. PDF fixture chứa dữ liệu giả, không dữ liệu cá nhân thật.

**Interfaces:** Các port dưới đây là hợp đồng đề xuất của UT-01, chưa phải endpoint đã tồn tại:

- `JobApplicationClient.getContext(UUID jobId): JobApplicationContext` gọi endpoint nội bộ có xác thực của company-service, không tăng job_views. Context gồm `UUID jobId`, `UUID companyId`, `String jobTitle`, `String companyName`, `String status`, `Instant deadline`, `boolean companyLocked`, `List<String> hrEmails`. Endpoint và cách cấp service credential phải được owner DN-04 xác nhận; cấm đọc bảng company trực tiếp hoặc cho browser lấy danh sách email HR.
- `ProfileCvClient.exportOwnedPdf(UUID cvId, String candidateBearerToken): CvPdf`. HS-04 xác minh JWT và CV ownership; trả `CvPdf(byte[] bytes)` chỉ sau khi xuất hoàn chỉnh. Dùng đường xuất PDF thực tế do HS-04 công bố, không tự tạo renderer hoặc service token giả danh ứng viên.
- FE dùng `applicationService.apply(jobId, {cvId, coverLetter})`, `list({status?, page?, size?})`, `detail(id)`, `withdraw(id, {reason})`, `downloadSnapshot(id)`; trả DTO tương ứng hoặc Blob cho PDF.

- [ ] Ghi mapping API public: POST `/api/v1/recruitment/jobs/{jobId}/apply` → 201 `ApplicationDetailResponse`; GET `/api/v1/recruitment/me/applications` → 200 `PageResponse<ApplicationSummaryResponse>`; GET `/api/v1/recruitment/me/applications/{id}` → 200 detail; POST cùng đường `/{id}/withdraw` → 200 detail; GET `/api/v1/recruitment/applications/{id}/cv` → 200 PDF có kiểm tra quyền.
- [ ] Định nghĩa summary gồm `id, jobId, jobTitle, companyId, companyName, status, createdAt, updatedAt, canWithdraw`; detail thêm `cvId, cvSnapshotUrl, coverLetter, history`; history gồm `id, fromStatus, toStatus, actorId, note, createdAt`. Sắp đơn `createdAt DESC, id DESC`; lịch sử `createdAt ASC, id ASC`.
- [ ] Ghi lỗi: 400 dữ liệu không hợp lệ; 401 thiếu/sai JWT; 403 sai role/chủ tài nguyên; 404 không có JD/CV/đơn; 409 trùng đơn/JD không nhận hồ sơ/rút sai bước; 502 response upstream không hợp lệ; 503 upstream unavailable hoặc snapshot storage lỗi. Giữ error shape common-core, không đổi lỗi hạ tầng thành 400.
- [ ] Ghi rõ các giả định ở mục 5 spec và quyết định đã xác nhận cho nộp lại sau REJECTED; xác nhận hợp đồng DN-04/HS-04 trước khi triển khai phần bị phụ thuộc. Nếu chưa có dependency thật, chỉ dùng fixture trong test/dev có opt-in, không fallback fixture trong production.
- [ ] Kiểm tra hợp đồng không gọi GET chi tiết DN-04 có side effect; commit riêng tài liệu khi bắt đầu triển khai.

## Task 2: Database và luật trạng thái dùng chung UT-02

**Files:** persistence/config ở bản đồ file; test `T/ApplicationPersistenceTest.java`, `T/ApplicationStatusPolicyTest.java`; `R/src/test/resources/application-test.yml`.

**Interfaces:** `ApplicationStatusPolicy.canWithdraw(ApplicationStatus): boolean`; `requireTransition(ApplicationStatus from, ApplicationStatus to): void`; `requireReapplicationAllowed(ApplicationStatus previous): void`; `ApplicationRepository.findOwnedForUpdate(UUID id, UUID userId): Optional<Application>` dùng pessimistic write lock; enum/policy đặt trong recruitment-service để UT-02 dùng cùng, không nhân bản sang common-core.

- [ ] Viết `ApplicationStatusPolicyTest`: assert chỉ 3 bước SUBMITTED/SCREENING/INTERVIEW được rút; OFFER/HIRED/REJECTED/WITHDRAWN bị chặn; chỉ tiến bước kế tiếp; REJECTED từ 4 bước trước HIRED; không chuyển từ terminal. `requireReapplicationAllowed` cho phép WITHDRAWN/REJECTED, chặn trạng thái khác; nộp mới không phải chuyển trạng thái của đơn terminal.
- [ ] Viết `ApplicationPersistenceTest` trên PostgreSQL thật qua Testcontainers: hai insert cùng cặp user/job bị unique; sau WITHDRAWN hoặc REJECTED insert mới thành công; giữ history cũ; HIRED vẫn chặn; có nhiều đơn REJECTED cũ nhưng đã có SUBMITTED mới thì insert tiếp vẫn bị unique; migration chạy lại không lỗi. Trước implementation chạy lệnh test bên dưới và xác nhận fail do phần UT-01 chưa có.
- [ ] Thêm Flyway, Spring Mail, Testcontainers PostgreSQL/JUnit test scope và Surefire 3.2.5 vào `R/pom.xml`; dùng version dependency được parent quản lý khi có. Cấu hình Flyway/default_schema `recruitment`; giữ các service khác độc lập.
- [ ] Tạo migration với 2 bảng nghiệp vụ, metadata `company_id/job_title/company_name` và bảng kỹ thuật outbox. UUID PK; timestamps `timestamptz`; status CHECK; FK history/outbox chỉ đến applications trong cùng schema; không cascade xoá lịch sử, không FK đến CV/user/job bên service khác.
- [ ] Tạo unique index `(user_id, job_id) WHERE status NOT IN ('WITHDRAWN','REJECTED')`: các đơn bị rút/không đạt không chặn lần nộp mới; SUBMITTED/SCREENING/INTERVIEW/OFFER/HIRED vẫn chặn. Thêm index `(user_id, created_at DESC, id DESC)`, `(user_id, status, created_at DESC, id DESC)`, history `(application_id, created_at, id)`.
- [ ] Outbox lưu `id, application_id, recipient, status, attempts, next_attempt_at, lease_until, created_at, sent_at`; unique `(application_id, recipient)`; trạng thái PENDING/PROCESSING/SENT/FAILED. Một hàng mỗi HR; email lấy từ trusted context và normalize/deduplicate trước insert.
- [ ] Implement entities, repositories, policy và injectable `Clock`; chạy test pass rồi commit `feat(UT-01): add applications persistence and status policy`.

Run từ root: `mvn -f server/pom.xml -pl services/recruitment-service -am "-Dtest=ApplicationStatusPolicyTest,ApplicationPersistenceTest" -Dsurefire.failIfNoSpecifiedTests=false test`.

## Task 3: Adapter PDF snapshot riêng tư và dependency clients

**Files:** nhóm Adapter; `J/config/RecruitmentDependencyExceptionHandler.java`; tests `T/CvSnapshotStorageTest.java`, `T/ApplicationDependencyClientsTest.java`.

**Interfaces:** `CvSnapshotStorage.store(UUID applicationId, byte[] pdf): void`; `read(UUID applicationId): byte[]`; `delete(UUID applicationId): void`. Object key được suy ra từ UUID của đơn; URL lưu DB là `/api/v1/recruitment/applications/{applicationId}/cv`, không phải đường filesystem hoặc URL tải PDF động của HS-04.

- [ ] Viết test client: JD context không tăng view; HS-04 nhận đúng bearer, CV người khác trả 403; upstream 404/403 giữ ý nghĩa; timeout/unavailable trả 503; nội dung không phải PDF trả 502. Response nội bộ company không được trả thẳng cho FE.
- [ ] Viết `CvSnapshotStorageTest`: lưu/read bytes A bằng nhau; xuất lại CV thành bytes B không đổi A; không cho overwrite cùng applicationId; delete thất bại phải log để dọn sau; file nằm ngoài static root; restart adapter vẫn đọc được A.
- [ ] Chạy targeted tests để thấy fail; implement Http clients bằng RestClient, cấu hình base URL/timeouts; credential nội bộ DN-04 lấy từ env theo hợp đồng Task 1, không ghi log token và không tin header userId của browser.
- [ ] Implement filesystem adapter dưới `RECRUITMENT_CV_STORAGE_DIR` mặc định `.runtime/recruitment-cvs` (tương đối với working directory đã ghi trong hướng dẫn chạy); ghi file tạm rồi atomic move/create-new. Không tạo URL công khai và không nhận đường file do client gửi.
- [ ] Map lỗi dependency/storage qua handler cục bộ với error shape chuẩn. Bổ sung `.env.example` chỉ tên biến và placeholder; hướng dẫn volume bền vững khi chạy container. Chạy test pass và commit `feat(UT-01): add immutable CV snapshot adapters`.

Run: `mvn -f server/pom.xml -pl services/recruitment-service -am "-Dtest=CvSnapshotStorageTest,ApplicationDependencyClientsTest" -Dsurefire.failIfNoSpecifiedTests=false test`.

## Task 4: API nộp đơn có transaction và chống trùng

**Files:** `J/service/ApplicationService.java`, `J/service/impl/ApplicationServiceImpl.java`, `J/service/ApplicationWriteService.java`, `J/service/impl/ApplicationWriteServiceImpl.java`, apply DTO/controller/mapper; tests `T/ApplyApplicationTest.java`, `T/ApplyApplicationConcurrencyTest.java`.

**Interfaces:** `ApplicationService.apply(AuthPrincipal actor, String bearerToken, UUID jobId, ApplyRequest request): ApplicationDetailResponse`; `ApplicationWriteService.createSubmitted(UUID applicationId, UUID userId, UUID cvId, JobApplicationContext context, String coverLetter): Application` chạy transaction riêng qua Spring bean. `ApplyRequest(UUID cvId, String coverLetter)` có `cvId` bắt buộc, coverLetter tối đa 5.000.

- [ ] Viết test fail cho 201/SUBMITTED, ownership CV, JD PENDING/hết hạn/đúng deadline/công ty khoá, gửi trùng 409; sau WITHDRAWN và sau REJECTED đều nộp lại tạo id/PDF mới, được chọn CV khác; assert đơn/PDF/history cũ giữ nguyên, history đơn mới bắt đầu `null → SUBMITTED`, actor lấy từ JWT, outbox đúng công ty. Có REJECTED cũ nhưng đã có đơn SUBMITTED mới thì request tiếp theo vẫn 409; JD hết hạn sau REJECTED vẫn bị chặn.
- [ ] Viết concurrency test dùng 2 transaction/thread thật và barrier cho cả nộp lần đầu và nộp lại sau REJECTED: cùng ứng viên/JD cho đúng một success, một 409; chỉ một đơn mới với history/outbox hợp lệ; đơn REJECTED cũ giữ nguyên và file của request thua được dọn. Test chạy PostgreSQL, không thay bằng H2 hoặc chỉ mock repository.
- [ ] Implement orchestration: kiểm role → lấy/kiểm JD → xuất PDF owned qua HS-04 → lưu file theo applicationId mới → lấy/kiểm lại JD, deadline và company lock → gọi write service transaction ngắn. Trong transaction kiểm deadline bằng Clock ngay trước insert, ghi application + history + email outbox và flush unique constraint.
- [ ] Catch xung đột unique ở ngoài transaction đã rollback để trả 409; không bắt lỗi rồi tiếp tục transaction đã rollback-only. Mọi lỗi sau khi lưu file phải cleanup snapshot của request thất bại; nếu cleanup lỗi thì ghi nhận để đối soát, không xoá file của đơn đã commit.
- [ ] Test xuất/lưu PDF lỗi không tạo đơn/history/outbox; DB lỗi sau lưu PDF có cleanup; hết hạn trong lúc render bị chặn. Quyết định JD dựa trên context mới nhất trước write, không khẳng định có transaction nguyên tử xuyên company/profile/recruitment; deadline kiểm tại insert, thay đổi trạng thái JD sau lần xác minh là giới hạn nhất quán cần ghi tài liệu.
- [ ] Chạy targeted tests pass; bổ sung controller `@PreAuthorize(Roles.CANDIDATE)`, HTTP 201; commit `feat(UT-01): submit applications with frozen CVs`.

Run: `mvn -f server/pom.xml -pl services/recruitment-service -am "-Dtest=ApplyApplicationTest,ApplyApplicationConcurrencyTest" -Dsurefire.failIfNoSpecifiedTests=false test`.

## Task 5: Đọc đơn, lịch sử, PDF và rút đơn

**Files:** các DTO response, controller/mapper, transition service; tests `T/CandidateApplicationApiTest.java`, `T/ApplicationTransitionConcurrencyTest.java`.

**Interfaces:** `ApplicationService.listOwn(UUID userId, ApplicationStatus status, int page, int size): PageResponse<ApplicationSummaryResponse>`; `getOwn(UUID userId, UUID id): ApplicationDetailResponse`; `withdraw(AuthPrincipal actor, UUID id, WithdrawRequest request): ApplicationDetailResponse`; `ApplicationTransitionService.transition(UUID id, ApplicationStatus target, UUID actorId, String note): Application` là hàm nội bộ cho caller đã được kiểm quyền, không tự expose thành API HR; `WithdrawRequest(String reason)` giới hạn 1–1.000 sau trim.

- [ ] Viết test fail: list chỉ của mình, status filter/invalid enum, phân trang 1/20/100, trang vượt trả items rỗng; detail đủ history theo thứ tự; metadata vẫn có khi JD không còn công khai; user khác 403, id không tồn tại 404.
- [ ] Viết test snapshot: candidate chủ đơn và HR có companyId trùng metadata đơn đọc được PDF; candidate khác, HR khác công ty, HR thiếu companyId bị 403; chưa login 401. Đáp ứng PDF có `Cache-Control: private, no-store`, `Content-Disposition: attachment`; không trả credential hoặc đường file thật.
- [ ] Implement read methods, candidate ownership checks, `canWithdraw` theo policy và snapshot controller; chỉ trả note khi `toStatus = WITHDRAWN` và `actorId = application.userId`, các transition khác note null; giữ đủ from/to/time. Test history có ghi chú HR nhưng response ứng viên không làm lộ ghi chú đó.
- [ ] Viết race test: HR chuyển INTERVIEW→OFFER và candidate rút chạy đồng thời qua cùng transition service. Lock cùng hàng; nếu OFFER thắng thì withdraw 409, nếu WITHDRAWN thắng thì chuyển OFFER bị chặn; đúng một transition/history mới. Ghi chú HR/API HR chưa thuộc UT-01.
- [ ] Implement withdraw với row lock + kiểm chủ đơn + kiểm trạng thái + đổi status/history trong cùng transaction; duplicate withdraw trả 409, không tạo thêm history. UT-02 phải gọi cùng transition/policy và khoá hàng, không update status trực tiếp.
- [ ] Chạy targeted tests, giữ ping/security test có datasource test hoặc mock bean phù hợp khi thêm repository; commit `feat(UT-01): track and withdraw own applications`.

Run: `mvn -f server/pom.xml -pl services/recruitment-service -am "-Dtest=CandidateApplicationApiTest,ApplicationTransitionConcurrencyTest,RecruitmentPingControllerTest" -Dsurefire.failIfNoSpecifiedTests=false test`.

## Task 6: Email HR sau commit, retry được

**Files:** nhóm Email; tests `T/ApplicationOutboxTest.java`; config SMTP trong recruitment application.yml; tài liệu cấu hình.

**Interfaces:** `ApplicationMailSender.sendNewApplication(UUID notificationId, String recipient, Application application): void`; `ApplicationOutboxWorker.dispatchBatch(): int`. Worker đọc jobTitle/companyName/applicationId từ đơn đã commit; không gửi kèm PDF hoặc CV URL public.

- [ ] Viết test fail: chỉ committed outbox được gửi; transaction apply rollback không gửi; SMTP lỗi không xoá đơn; retry sau restart; trùng địa chỉ HR chỉ một outbox; không có HR tạo cảnh báo vận hành có applicationId, không giả email nhận.
- [ ] Implement worker định kỳ: claim hàng PENDING đến hạn bằng `FOR UPDATE SKIP LOCKED`, đặt PROCESSING và lease 60 giây trong transaction ngắn; gửi SMTP ngoài transaction; mark SENT hoặc retry. Thu hồi PROCESSING hết lease; tối đa 5 lần, chờ 1/5/15/60 phút rồi FAILED; có cách requeue FAILED được ghi tài liệu vận hành.
- [ ] Test hai worker không claim cùng hàng; process chết sau claim thì lease cho retry. Dùng notificationId làm Message-ID ổn định; ghi rõ SMTP là at-least-once, có thể nhận trùng nếu crash sau gửi trước mark SENT, không hứa exactly-once.
- [ ] Cấu hình SMTP dev qua MailHog `localhost:1025`, từ địa chỉ cấu hình; worker có cờ tắt trong tests. Nội dung tiếng Việt, link tới khu HR có xác thực theo đường do UT-02 thống nhất; trước khi có trang HR, email nêu mã đơn/JD, không gắn link chết.
- [ ] Chạy test pass và xác minh MailHog nhận email khi tích hợp; commit `feat(UT-01): notify company HR through email outbox`.

Run: `mvn -f server/pom.xml -pl services/recruitment-service -am "-Dtest=ApplicationOutboxTest" -Dsurefire.failIfNoSpecifiedTests=false test`.

## Task 7: FE hộp ứng tuyển, tích hợp nút DN-04

**Files:** nhóm FE apply; file trang chi tiết JD thật của DN-04; `web/package.json`, `web/package-lock.json`, `web/playwright.config.ts`, `web/e2e/ut01-apply.spec.ts`.

**Interfaces:** `ApplyButton({jobId, acceptingApplications}: {jobId: string; acceptingApplications: boolean})`; `ApplyDialog({jobId, open, onOpenChange, onApplied}: {jobId: string; open: boolean; onOpenChange(open: boolean): void; onApplied(application: ApplicationDetail): void})`. CV client của HS-04 phải trả tối thiểu `id, name, targetPosition`; tái sử dụng service thật của HS-04 khi có.

- [ ] Đọc `web/AGENTS.md` và guide liên quan trong `web/node_modules/next/dist/docs/` trước code. Thêm Playwright dev dependency/script `test:e2e` và config chạy localhost, không thay framework; kiểm hành vi bằng fixture network và một suite tích hợp thật ở Task 9.
- [ ] Viết browser test fail: chọn một trong hai CV vị trí khác nhau → request gửi đúng cvId/coverLetter; chưa có CV hiển thị đường tới màn HS-04 thật; chưa login yêu cầu đăng nhập; role khác candidate không được nộp.
- [ ] Implement types, Axios service dùng `W/services/api.ts`, button/dialog chọn CV và thư giới thiệu. Giữ giới hạn nhập đồng bộ BE; thư chỉ hiển thị như văn bản, không render HTML không tin cậy; không hỏi upload PDF mới trong UT-01.
- [ ] Disable nút khi đang gửi, giữ nội dung nếu lỗi; 409 trùng hướng tới trang đơn, 409 JD đóng nhận hiển thị thông báo phù hợp; 503 cho thử lại; success đóng hộp và link tới chi tiết đơn. Browser timeout không khẳng định chưa có đơn: làm mới danh sách để đối soát trước gửi lại.
- [ ] Gắn component vào trang chi tiết DN-04, không tạo trang `/jobs/[id]` giả khi dependency chưa sẵn. Test keyboard focus/Escape/label của dialog, lỗi backend hiển thị rõ; chạy type-check/lint/browser test pass rồi commit `feat(UT-01): add candidate apply dialog`.

Run trong `web/`: `npm.cmd run type-check`; `npm.cmd run lint`; `npm.cmd run test:e2e -- e2e/ut01-apply.spec.ts`.

## Task 8: FE ứng tuyển của tôi và rút đơn

**Files:** nhóm FE tracking; thêm link ở `W/app/(pages)/(app)/[role]/page.tsx` cho candidate; `web/e2e/ut01-tracking.spec.ts`.

**Interfaces:** routes `/candidate/applications?status=&page=` và `/candidate/applications/{id}`; auth guard roles CANDIDATE. Dùng ApplicationSummary/ApplicationDetail từ Task 7; không tạo schema status riêng ngoài enum union và bảng config nhãn/màu FE.

- [ ] Viết browser test fail: filter giữ sau F5; hiển thị JD/công ty/ngày/trạng thái, link chi tiết; empty/loading/error; lịch sử đủ lần đổi trạng thái; tải PDF qua Axios có JWT rồi tạo Blob URL và revoke khi xong.
- [ ] Implement danh sách, phân trang, chi tiết; nhãn SUBMITTED “Đã gửi”, SCREENING “Sàng lọc”, INTERVIEW “Phỏng vấn”, OFFER “Offer”, HIRED “Nhận việc”, REJECTED “Không đạt”, WITHDRAWN “Đã rút”. Dùng nhãn chữ cùng màu, không chỉ dựa màu.
- [ ] Implement tiến trình 5 bước chính; REJECTED/WITHDRAWN hiển thị kết thúc tại bước trước đó suy ra từ history, không đánh dấu HIRED. Hiển thị thời gian theo Asia/Ho_Chi_Minh; metadata dùng dữ liệu của đơn, không phụ thuộc JD còn public.
- [ ] Implement hộp xác nhận rút với lý do bắt buộc; chỉ hiện hành động theo `canWithdraw`. Sau rút cập nhật detail/list/history. Với đơn WITHDRAWN hoặc REJECTED, cung cấp link trở lại JD thật của DN-04 để ứng tuyển lại; BE vẫn kiểm JD và đơn khác đang chặn. Nếu BE trả 409 do HR vừa chuyển OFFER, tải lại detail và ẩn nút Rút.
- [ ] Test OFFER không có nút rút, history tăng đúng một lần sau thao tác; sau REJECTED có đường ứng tuyển lại và lần gửi thành công dẫn tới đơn mới, vẫn xem được đơn cũ; responsive ở màn nhỏ, lỗi không mất danh sách cũ. Chạy FE checks pass rồi commit `feat(UT-01): add application tracking and withdrawal pages`.

Run trong `web/`: `npm.cmd run type-check`; `npm.cmd run lint`; `npm.cmd run test:e2e -- e2e/ut01-tracking.spec.ts`.

## Task 9: Tích hợp thật, CI và bàn giao PR

**Files:** `docs/api/UT-01-applications.md`; `.github/workflows/ci.yml`; `web/e2e/ut01-integration.spec.ts`; `server/api-gateway/src/test/java/com/rikkei/jobs/gateway/RecruitmentRouteIntegrationTest.java`; deployment/env docs liên quan.

**Interfaces:** Dùng production contracts Task 1 đã xác nhận, services HS-04/DN-04 thật và PostgreSQL/storage volume/SMTP dev; backend test fixture không được dùng để kết luận dependency integration đã xong.

- [ ] Viết và chạy gateway test bảo đảm `/api/v1/recruitment/...` giữ Authorization và map đúng controller; giữ route chung hiện có nếu đã đủ, không tạo alias gây tranh chấp với DN-04.
- [ ] Chạy tích hợp candidate thật: tạo hai CV chọn section khác nhau qua HS-04; mở JD thật qua DN-04; nộp CV A → sửa hồ sơ/tải CV A mới → hash PDF snapshot cũ không đổi. Xoá CV gốc vẫn đọc snapshot; QR phản ánh rút PUBLIC theo HS-04. Không tự ghi SQL để thay thế luồng chức năng đã có.
- [ ] Chạy ma trận nghiệm thu dưới đây với 2 candidate, HR cùng/khác công ty; kiểm job_views chỉ tăng theo GET chi tiết JD, không theo apply/list/detail đơn. Nếu DN-04 chưa sẵn, ghi rõ phần tích hợp còn chờ, không đánh dấu hoàn tất task này.
- [ ] Cập nhật CI bắt PR vào develop (giữ main/test nếu nhóm còn dùng), Node 24 theo repo, `npm ci` không fallback đổi lockfile, type-check/lint/build fail phải fail job. Thêm UT-01 Playwright checks và backend PostgreSQL Testcontainers; không để `|| true` che lỗi kiểm tra frontend.
- [ ] Chạy backend `mvn -f server/pom.xml -pl services/recruitment-service,api-gateway -am test`; trong web chạy `npm.cmd run type-check`, `npm.cmd run lint`, `npm.cmd run build`, `npm.cmd run test:e2e -- e2e/ut01-apply.spec.ts e2e/ut01-tracking.spec.ts e2e/ut01-integration.spec.ts`; đối chiếu output thực tế, không báo pass cho test chưa chạy.
- [ ] Self-review spec coverage, diff và secret/file CV thật; cập nhật cách chạy, migrate, config storage, email retry và các giới hạn nhất quán. Review với thành viên nhóm theo quy ước; xử lý lỗi trước PR.
- [ ] Ở giai đoạn triển khai, tạo feature branch đúng phạm vi bằng worktree nếu cần; commit các phần đã kiểm tra. Sau khi xác minh remote có develop và checks thật sự chạy, push và tạo PR base develop, title `[UT-01] Ứng tuyển bằng CV và theo dõi trạng thái`; nộp link PR. Không merge tự động.

### Ma trận nghiệm thu

| Tình huống | Kết quả cần thấy | Task chứng minh |
| --- | --- | --- |
| Nộp lần đầu hợp lệ | 201, SUBMITTED, PDF riêng, history khởi tạo, outbox HR | 4, 6, 9 |
| Nộp lại cùng JD khi có đơn | 409, kể cả hai request đồng thời | 2, 4 |
| Rút rồi nộp lại | Đơn mới/id mới/PDF mới; đơn cũ giữ WITHDRAWN | 4, 5, 8 |
| REJECTED rồi nộp lại cùng JD | 201 khi JD còn nhận hồ sơ; đơn mới/SUBMITTED/PDF mới, giữ nguyên đơn và lịch sử REJECTED cũ | 2, 4, 8, 9 |
| Hai request nộp lại sau REJECTED | Một 201 và một 409; chỉ một đơn mới; không thay đổi đơn REJECTED cũ | 2, 4 |
| JD hết hạn/PENDING/công ty khoá | Không tạo đơn; kể cả hết hạn trong khi render | 4, 9 |
| CV thuộc người khác | 403, không lưu snapshot/đơn | 3, 4 |
| Sửa/xoá CV gốc | HR vẫn đọc đúng PDF đã nộp | 3, 5, 9 |
| Rút công khai QR | Trang QR tuân thủ HS-04; bytes PDF snapshot giữ nguyên | 9 |
| Rút đơn OFFER | 409, không đổi status/history | 5, 8 |
| Rút đua với OFFER | Một transition hợp lệ; history không mất/trùng | 5 |
| List/detail/withdraw của người khác | Không truy cập/thay đổi được | 5 |
| HR khác công ty đọc PDF | 403 | 5, 9 |
| SMTP unavailable/restart | Đơn vẫn có, outbox retry được | 6 |
| Mỗi lần đổi trạng thái | Một history tương ứng cùng transaction | 4, 5 |
| Lọc, tải lại, lỗi mạng, chưa có CV | UI có trạng thái và hành động phù hợp | 7, 8 |

## Điều kiện bắt đầu và tự rà soát kế hoạch

- Chính sách REJECTED đã được người dùng xác nhận: được nộp lại cùng JD; Task 2 migration, Task 4 apply và Task 8 UI đều áp dụng WITHDRAWN/REJECTED cho lần nộp mới. HIRED vẫn chặn theo đề xuất hiện tại.
- Xác nhận endpoint/auth/fields từ HS-04 và DN-04 trước triển khai Http adapter và gắn FE; chưa có thì core vẫn phát triển bằng tests, tích hợp thật còn chờ.
- Tuân thủ auth, schema ownership, history và lock contract với UT-02 trước khi phía HR thêm transition API.
- Spec coverage: mỗi yêu cầu UT-01 và các ảnh hưởng HS-04/DN-04 đã có task/ma trận; tên port và DTO nhất quán giữa các task; Review Focus đã gắn test; không đưa tìm việc, renderer CV hay HR workflow vào UT-01.
- Lập kế hoạch không cần chạy build/test sản phẩm. Khi triển khai, ghi riêng kết quả test thực tế, không đánh dấu các checkbox dựa trên việc chỉ có tài liệu.
