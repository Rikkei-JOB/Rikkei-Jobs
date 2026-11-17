# UT-02 HR Application Pipeline Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** HR xử lý đơn đúng luật và nhiều vòng phỏng vấn, STAFF theo dõi toàn bộ, ứng viên nhận email sau mỗi lần chuyển trạng thái.

**Architecture:** `recruitment-service` sở hữu trạng thái, vòng, audit và outbox; khóa hàng `applications` khi chuyển bước hoặc sửa vòng. API đọc chi tiết kiểm quyền trước rồi tổng hợp identity và điểm hiện tại của DN-06. Next.js dùng response tổng hợp, tái dùng thẻ điểm DN-06 khi component sẵn sàng.

**Tech Stack:** Java 21, Spring Boot 3.3, JPA/Flyway/PostgreSQL, JUnit 5/MockMvc/Testcontainers, Next.js 16/React 19/TypeScript, Playwright.

**Spec:** `docs/superpowers/specs/2026-11-17-ut02-hr-application-pipeline-design.md`

## Global Constraints

- API dưới `/api/v1/recruitment`; JSON camelCase, lỗi `{code,message}`, phân trang `page=1`, `size=20`, `size<=100`, thời gian ISO 8601 UTC.
- HR chỉ truy cập đơn có `companyId` bằng JWT; sai công ty trả 404. STAFF chỉ đọc, bao gồm CV PDF. CANDIDATE giữ quyền UT-01.
- HR transition: `SUBMITTED -> SCREENING|REJECTED`, `SCREENING -> INTERVIEW|REJECTED`, `INTERVIEW -> REJECTED`; lỗi `409 INVALID_TRANSITION`.
- UT-07 độc quyền API gửi offer; core transition kiểm điều kiện offer dưới cùng khóa đơn: có ít nhất một vòng, không còn PENDING, vòng cuối PASS.
- Điểm khớp là giá trị hiện tại từ DN-06, không tính từ PDF CV; DN-06 lỗi thì `match:null`, identity lỗi thì chi tiết trả lỗi phụ thuộc.
- Chỉ tạo và sửa vòng trong INTERVIEW; `scheduledAt` tùy chọn; sửa kết quả phải ghi audit; không xóa vòng.
- Mỗi lần chuyển status sau SUBMITTED tạo một event email ứng viên trong cùng transaction; tạo đơn ban đầu vẫn chỉ báo HR như UT-01.
- Đọc `web/AGENTS.md` và Next.js 16 docs trong `web/node_modules/next/dist/docs/` trước khi sửa FE.

## Review Focus

1. HR đoán ID đơn/CV/vòng của công ty khác: tất cả trả 404, không gọi identity/DN-06 trước khi kiểm quyền (Task 2 và 4).
2. Hai HR cùng tạo vòng hoặc một người sửa vòng khi UT-07 gửi offer: roundNo không trùng; kết quả offer dùng trạng thái vòng đã khóa (Task 1 và 2).
3. Một đơn có nhiều lần chuyển nhưng cùng email nhận: mỗi history sinh một email, retry không sinh thêm event (Task 3).
4. DN-06 không sẵn sàng hoặc trả 403 bất ngờ: lỗi tạm thời hiển thị `match:null`; lỗi quyền/hợp đồng không bị biến thành điểm 0 (Task 4).
5. FE đang hiện nút hợp lệ nhưng status đổi ở request khác: 409 được hiển thị và chi tiết được tải lại (Task 5).

---

## File Map

| Trách nhiệm | File tạo/sửa chính |
| --- | --- |
| Vòng và audit | `server/services/recruitment-service/src/main/resources/db/migration/V2__interview_rounds.sql`, `.../entity/InterviewRound.java`, `.../entity/InterviewRoundRevision.java`, `.../repository/InterviewRoundRepository.java`, `.../repository/InterviewRoundRevisionRepository.java`, `.../service/InterviewRoundService.java`, `.../service/OfferEligibilityPolicy.java` |
| HR transitions/quyền | `.../service/HrTransitionPolicy.java`, `.../service/HrApplicationWorkflowService.java`, `.../controller/HrApplicationController.java`, `.../controller/ApplicationSnapshotController.java`, sửa `ApplicationTransitionServiceImpl.java` |
| Email ứng viên | `.../db/migration/V3__status_notification_outbox.sql`, `.../entity/ApplicationStatusNotification.java`, `.../service/ApplicationStatusOutboxWorker.java`, `.../service/ApplicationStatusMailSender.java`; identity `.../controller/InternalUserController.java` |
| API đọc tổng hợp | `.../client/IdentityCandidateClient.java`, `.../client/JobMatchClient.java`, `.../service/HrApplicationReadService.java`, `.../controller/StaffApplicationController.java`, DTO riêng cho HR/STAFF |
| FE | `web/src/interface/hr-application.ts`, `web/src/services/hr-application.service.ts`, `web/src/app/components/recruitment/*`, `web/src/app/(pages)/(app)/hr/recruitment/*`, `web/src/app/(pages)/(app)/staff/applications/*`, `web/src/config/menu.ts` |
| Hợp đồng/bàn giao | `docs/api/ut02-openapi.yaml`, `docs/api/UT-02-application-pipeline.md`, ảnh trong `docs/api/screenshots/` |

Các đường `...` trong bảng là dưới `server/services/recruitment-service/src/main/java/com/rikkei/jobs/recruitment/`, riêng migration dưới `src/main/resources/`. Mỗi task dưới đây nêu file chính xác.

### Task 1: Vòng phỏng vấn, audit và điều kiện offer

**Files:**
- Create: `server/services/recruitment-service/src/main/resources/db/migration/V2__interview_rounds.sql`
- Create: `server/services/recruitment-service/src/main/java/com/rikkei/jobs/recruitment/entity/InterviewRound.java`, `InterviewRoundRevision.java`
- Create: `server/services/recruitment-service/src/main/java/com/rikkei/jobs/recruitment/repository/InterviewRoundRepository.java`, `InterviewRoundRevisionRepository.java`
- Create: `server/services/recruitment-service/src/main/java/com/rikkei/jobs/recruitment/service/InterviewRoundService.java`, `OfferEligibilityPolicy.java`
- Create: `server/services/recruitment-service/src/main/java/com/rikkei/jobs/recruitment/dto/request/CreateRoundRequest.java`, `UpdateRoundRequest.java`; `.../dto/response/InterviewRoundResponse.java`
- Modify: `server/services/recruitment-service/src/test/java/com/rikkei/jobs/recruitment/ApplicationIntegrationSupport.java`
- Test: `server/services/recruitment-service/src/test/java/com/rikkei/jobs/recruitment/InterviewRoundServiceTest.java`, `OfferEligibilityPolicyTest.java`

**Interfaces:** `InterviewRoundService.create(AuthPrincipal actor, UUID applicationId, CreateRoundRequest request): InterviewRoundResponse`; `update(AuthPrincipal actor, UUID applicationId, UUID roundId, UpdateRoundRequest request): InterviewRoundResponse`; `list(UUID applicationId): List<InterviewRoundResponse>`. `OfferEligibilityPolicy.requireEligible(List<InterviewRound> rounds): void` ném conflict khi rỗng, có PENDING hoặc vòng cuối khác PASS.

- [ ] **Step 1: Viết test fail.** `InterviewRoundServiceTest`: chỉ INTERVIEW được tạo/sửa; `roundNo` là 1,2; `scheduledAt` null hoặc UTC; sửa PASS→FAIL→PASS sinh hai revision; round của đơn khác trả 404; hai request tạo đồng thời không trùng `roundNo`. `OfferEligibilityPolicyTest`: rỗng/PENDING/last FAIL bị chặn; các vòng có kết quả với last PASS được phép, kể cả vòng trước FAIL.
- [ ] **Step 2: Chạy RED.** `mvn -f server/pom.xml -pl services/recruitment-service -am "-Dtest=InterviewRoundServiceTest,OfferEligibilityPolicyTest" -Dsurefire.failIfNoSpecifiedTests=false test`; kỳ vọng thiếu type/migration hoặc assertion fail.
- [ ] **Step 3: Implement.** Migration có unique `(application_id,round_no)`, FK và CHECK result; `create/update` lấy `applications.findForUpdate(id)` trước khi đọc/sửa vòng; score nullable 0–100, PENDING xóa score; revision chỉ ghi khi giá trị thay đổi. Cập nhật cleanup test support theo thứ tự revision→round→application.
- [ ] **Step 4: Chạy GREEN.** Cùng lệnh Step 2; tất cả test PASS, Flyway validate và JPA validate PASS.
- [ ] **Step 5: Commit.** `feat(UT-02): add interview rounds and offer eligibility`.

### Task 2: Map chuyển bước HR, API ghi và quyền CV

**Files:**
- Create: `server/services/recruitment-service/src/main/java/com/rikkei/jobs/recruitment/service/HrTransitionPolicy.java`, `HrApplicationWorkflowService.java`
- Create: `server/services/recruitment-service/src/main/java/com/rikkei/jobs/recruitment/controller/HrApplicationController.java`
- Create: `server/services/recruitment-service/src/main/java/com/rikkei/jobs/recruitment/dto/request/TransitionRequest.java`, `.../dto/response/ApplicationWorkflowResponse.java`
- Create: `server/services/recruitment-service/src/main/java/com/rikkei/jobs/recruitment/config/InvalidTransitionException.java`, `InvalidTransitionExceptionHandler.java`
- Modify: `server/services/recruitment-service/src/main/java/com/rikkei/jobs/recruitment/service/impl/ApplicationTransitionServiceImpl.java`, `controller/ApplicationSnapshotController.java`, `service/impl/ApplicationServiceImpl.java`
- Test: `server/services/recruitment-service/src/test/java/com/rikkei/jobs/recruitment/HrTransitionPolicyTest.java`, `HrApplicationApiTest.java`, `ApplicationTransitionConcurrencyTest.java`

**Interfaces:** `HrTransitionPolicy.allowedFrom(ApplicationStatus from): Set<ApplicationStatus>` và `requireAllowed(from,to): void` dùng map cố định; `HrApplicationWorkflowService.transition(AuthPrincipal actor, UUID id, ApplicationStatus to, String note): ApplicationWorkflowResponse` kiểm quyền và map dưới khóa đơn. `ApplicationWorkflowResponse` gồm `id,status,updatedAt` để FE tải lại detail sau mutation. `ApplicationTransitionService.transition(...)` giữ interface UT-01; khi target OFFER, core gọi `OfferEligibilityPolicy` sau khi khóa đơn. Controller expose `POST /recruitment/applications/{id}/transition`, `POST /rounds`, `PUT /rounds/{roundId}` cho HR.

- [ ] **Step 1: Viết test fail.** Ma trận toàn bộ 7×7 cặp status: chỉ năm cặp HR hợp lệ; các cặp khác `INVALID_TRANSITION`. MockMvc: SUBMITTED→INTERVIEW 409; HR công ty B POST/CV 404; STAFF POST 403 và CV 200; candidate withdraw còn hoạt động; OFFER khi last FAIL/PENDING 409 ngay cả khi gọi core transition trực tiếp. Race test offer với thêm/sửa vòng chỉ có một thứ tự commit hợp lệ.
- [ ] **Step 2: Chạy RED.** `mvn -f server/pom.xml -pl services/recruitment-service -am "-Dtest=HrTransitionPolicyTest,HrApplicationApiTest,ApplicationTransitionConcurrencyTest" -Dsurefire.failIfNoSpecifiedTests=false test`; kỳ vọng fail ở API/policy mới.
- [ ] **Step 3: Implement.** HR controller dùng `@PreAuthorize(Roles.HR)`; workflow kiểm `actor.companyId == application.companyId` dưới khóa, sai nhận `ResourceNotFoundException`; policy lỗi riêng map ra `409 INVALID_TRANSITION` thay vì ConflictException chung. STAFF chỉ đọc CV; giữ `private,no-store`. Mutation response chỉ chứa dữ liệu recruitment đã commit để lỗi identity/DN-06 không biến một mutation thành lỗi mơ hồ.
- [ ] **Step 4: Chạy GREEN.** Cùng lệnh Step 2; ma trận, MockMvc và race test PASS.
- [ ] **Step 5: Commit.** `feat(UT-02): enforce HR transitions and application ownership`.

### Task 3: Email ứng viên theo từng transition và API identity nội bộ

**Files:**
- Create: `server/services/recruitment-service/src/main/resources/db/migration/V3__status_notification_outbox.sql`
- Create: `server/services/recruitment-service/src/main/java/com/rikkei/jobs/recruitment/entity/ApplicationStatusNotification.java`
- Create: `server/services/recruitment-service/src/main/java/com/rikkei/jobs/recruitment/repository/ApplicationStatusNotificationRepository.java`
- Create: `server/services/recruitment-service/src/main/java/com/rikkei/jobs/recruitment/service/ApplicationStatusOutboxWorker.java`, `ApplicationStatusMailSender.java`
- Create: `server/services/recruitment-service/src/main/java/com/rikkei/jobs/recruitment/service/impl/SmtpApplicationStatusMailSender.java`
- Create: `server/services/recruitment-service/src/main/java/com/rikkei/jobs/recruitment/client/IdentityCandidateClient.java`, `HttpIdentityCandidateClient.java`, `CandidateIdentity.java`
- Modify: `server/services/recruitment-service/src/main/java/com/rikkei/jobs/recruitment/service/impl/ApplicationTransitionServiceImpl.java`, `server/services/identity-service/src/main/java/com/rikkei/jobs/identity/controller/InternalUserController.java`, `server/services/identity-service/src/main/java/com/rikkei/jobs/identity/service/InternalUserService.java`
- Modify: `server/services/recruitment-service/src/main/resources/application.yml`, `.env.example`
- Test: `server/services/recruitment-service/src/test/java/com/rikkei/jobs/recruitment/ApplicationStatusNotificationTest.java`; identity `server/services/identity-service/src/test/java/com/rikkei/jobs/identity/InternalUserApiTest.java` (extend/create existing test if named differently)

**Interfaces:** identity `GET /internal/users/{id}` with `X-Internal-Key` returns existing `InternalUserResponse(userId,email,fullName,role,companyId)`. Recruitment `IdentityCandidateClient.get(UUID userId): CandidateIdentity` resolves current recipient; `CandidateIdentity` is a record `(UUID userId, String fullName, String email)`. Worker obtains it only when dispatching. Outbox unique `status_history_id`; sender `send(UUID notificationId, String email, ApplicationStatusHistory history, Application application): void`.

- [ ] **Step 1: Viết test fail.** Mỗi transition thành công tạo đúng một status outbox row gắn history; tạo SUBMITTED không tạo row mới; rollback không để row; hai transition cùng recipient sinh hai row; worker gửi đúng status, retry lỗi SMTP/identity và không tái tạo event; identity GET có key đúng 200, key sai 401, user không tồn tại 404.
- [ ] **Step 2: Chạy RED.** `mvn -f server/pom.xml -pl services/recruitment-service,services/identity-service -am "-Dtest=ApplicationStatusNotificationTest,InternalUserApiTest" -Dsurefire.failIfNoSpecifiedTests=false test`; kỳ vọng fail.
- [ ] **Step 3: Implement.** Outbox riêng giữ nguyên unique của email HR UT-01; event được ghi cùng transaction ở `ApplicationTransitionServiceImpl.change`. Worker theo retry/lease của `ApplicationOutboxWorker`; lấy email hiện hành từ identity qua internal key, giữ idempotent Message-ID theo notification ID; nội dung không để lộ ghi chú HR. Mở rộng test support cleanup outbox mới.
- [ ] **Step 4: Chạy GREEN.** Cùng lệnh Step 2; tests PASS, SMTP MIME test kiểm subject/body và recipient.
- [ ] **Step 5: Commit.** `feat(UT-02): notify candidates for each status transition`.

### Task 4: API đọc tổng hợp và danh sách HR/STAFF

**Files:**
- Create: `server/services/recruitment-service/src/main/java/com/rikkei/jobs/recruitment/client/JobMatchClient.java`, `HttpJobMatchClient.java`
- Create: `server/services/recruitment-service/src/main/java/com/rikkei/jobs/recruitment/service/HrApplicationReadService.java`
- Create: `server/services/recruitment-service/src/main/java/com/rikkei/jobs/recruitment/controller/StaffApplicationController.java`, `ApplicationDetailController.java`
- Create: `server/services/recruitment-service/src/main/java/com/rikkei/jobs/recruitment/dto/response/HrApplicationDetailResponse.java`, `HrApplicationSummaryResponse.java`, `MatchResponse.java`, `InterviewRoundRevisionResponse.java`
- Modify: `server/services/recruitment-service/src/main/java/com/rikkei/jobs/recruitment/repository/ApplicationRepository.java`, `server/services/recruitment-service/src/main/resources/application.yml`, `.env.example`
- Modify: `server/services/identity-service/src/main/java/com/rikkei/jobs/identity/controller/InternalUserController.java`, `service/InternalUserService.java`, `service/impl/InternalUserServiceImpl.java` for a service-key-protected batch lookup of the page's user IDs
- Test: `server/services/recruitment-service/src/test/java/com/rikkei/jobs/recruitment/HrApplicationReadApiTest.java`, `ApplicationDependencyClientsTest.java`

**Interfaces:** `HrApplicationReadService.detail(AuthPrincipal actor, UUID id, String bearer): HrApplicationDetailResponse`; `listHr(AuthPrincipal actor, UUID jobId, ApplicationStatus status, int page, int size): PageResponse<HrApplicationSummaryResponse>`; `listStaff(UUID companyId, UUID jobId, ApplicationStatus status, int page, int size): PageResponse<HrApplicationSummaryResponse>`. `IdentityCandidateClient.getBatch(List<UUID> ids): Map<UUID,CandidateIdentity>` uses identity `POST /internal/users/candidates/lookup` (max 100 IDs) for paged names; DN-05 owns the separate `POST /internal/users/lookup` account-status contract. `JobMatchClient.get(UUID jobId, UUID userId, String bearer): Optional<MatchResponse>`. Detail response giữ history note HR cho HR/STAFF, kèm rounds/revisions, candidate `{userId,fullName,email}`, `match` nullable, `allowedTransitions` từ Task 2 (STAFF nhận mảng rỗng); summary có tên ứng viên.

- [ ] **Step 1: Viết test fail.** HR chỉ thấy đơn `companyId` của JWT; STAFF lọc company/job/status, page 1-based; danh sách 100 đơn chỉ gọi identity một lượt batch; HR sai company 404 và không gọi identity/DN-06; STAFF đọc chi tiết; DN-06 200 có match, 404/503/timeout `match:null`, 401/403 lỗi dependency; identity lỗi hoặc ID không thuộc CANDIDATE không trả danh tính giả. `roundId` của đơn khác không xuất hiện.
- [ ] **Step 2: Chạy RED.** `mvn -f server/pom.xml -pl services/recruitment-service -am "-Dtest=HrApplicationReadApiTest,ApplicationDependencyClientsTest" -Dsurefire.failIfNoSpecifiedTests=false test`; kỳ vọng fail.
- [ ] **Step 3: Implement.** Query filter bằng JPQL/specification với companyId từ principal; detail kiểm quyền trước HTTP calls; identity gọi trực tiếp service với internal key, DN-06 gọi URL cấu hình bằng Bearer HR/STAFF. Không dùng public JD detail vì có thể tăng job view. Không sửa candidate response UT-01 để lộ note HR.
- [ ] **Step 4: Chạy GREEN.** Cùng lệnh Step 2; test PASS. Chạy `mvn -f server/pom.xml -pl services/recruitment-service,services/identity-service -am test` để bắt regression UT-01.
- [ ] **Step 5: Commit.** `feat(UT-02): add HR and staff application views`.

### Task 5: Giao diện HR và STAFF

**Files:**
- Create: `web/src/interface/hr-application.ts`, `web/src/services/hr-application.service.ts`
- Create: `web/src/app/components/recruitment/application-table.tsx`, `application-workflow-detail.tsx`, `interview-rounds.tsx`, `match-card.tsx`
- Create: `web/src/app/(pages)/(app)/hr/recruitment/page.tsx`, `web/src/app/(pages)/(app)/hr/recruitment/[id]/page.tsx`, `web/src/app/(pages)/(app)/staff/applications/page.tsx`, `web/src/app/(pages)/(app)/staff/applications/[id]/page.tsx`
- Modify: `web/src/config/menu.ts`
- Test: `web/e2e/ut02-hr-workflow.spec.ts`, `web/e2e/ut02-staff-applications.spec.ts`

**Interfaces:** FE `hrApplicationService.listHr/listStaff/detail/transition/createRound/updateRound/downloadCv` theo Task 4; các view nhận `readOnly` từ role, nút HR đọc `allowedTransitions` từ detail, không tự tính score/match. `match-card` nhận nullable `MatchResponse`; có thể thay bằng export của DN-06 khi sẵn sàng.

- [ ] **Step 1: Viết Playwright tests fail.** HR thấy đơn của mình và đích chuyển hợp lệ; thêm vòng chỉ ở INTERVIEW; sửa kết quả lưu và hiển thị revision; 409 chuyển bước làm mới detail; match null báo “Chưa có điểm khớp”; STAFF lọc và tải CV được nhưng không thấy nút ghi; viewport mobile không tràn bảng.
- [ ] **Step 2: Chạy RED.** `npm --prefix web run test:e2e -- --grep UT-02`; kỳ vọng route/component chưa tồn tại.
- [ ] **Step 3: Implement.** Đọc hướng dẫn Next.js 16 theo `web/AGENTS.md`. Thêm types/client, trang và component theo route hiện có; `application-table` giữ lọc trên URL, page 1-based; lấy CV blob qua API có JWT; `match-card` hiển thị kết quả DN-06 nhưng không nhân bản công thức.
- [ ] **Step 4: Chạy GREEN.** Chạy riêng `npm --prefix web run type-check`, `npm --prefix web run lint`, `npm --prefix web run build`, `npm --prefix web run test:e2e -- --grep UT-02`; mỗi lệnh exit 0.
- [ ] **Step 5: Commit.** `feat(UT-02): add HR and staff application screens`.

### Task 6: Hợp đồng API, dữ liệu demo và nghiệm thu

**Files:**
- Create: `docs/api/ut02-openapi.yaml`, `docs/api/UT-02-application-pipeline.md`
- Create: ảnh HR/STAFF trong `docs/api/screenshots/` bằng môi trường demo đã chạy
- Modify: `README.md`, `docs/database/erd.md` nếu sơ đồ đang liệt kê các bảng recruitment
- Test: OpenAPI/contract smoke test phù hợp tool sẵn có; toàn bộ suite backend/FE.

**Interfaces:** OpenAPI mô tả chính xác endpoint Task 2/4, DTO, phân quyền, 400/401/403/404/409, response `match:null`, `INVALID_TRANSITION`, paginated list; tài liệu chạy có fixture công ty A/B, HR1/HR2, STAFF, đơn nhiều trạng thái và vòng.

- [ ] **Step 1: Viết checklist nghiệm thu từ spec.** Ghi lệnh/request cho từng case: SUBMITTED→INTERVIEW 409; last FAIL/PENDING chặn offer; HR B xem đơn A 404; STAFF xem/tải CV; mỗi transition có email; ma trận status; DN-06 tạm lỗi.
- [ ] **Step 2: Chạy smoke trước khi cập nhật tài liệu.** So sánh endpoint thực và OpenAPI; ghi chỗ lệch cần sửa, không đoán từ spec.
- [ ] **Step 3: Hoàn thiện tài liệu và fixture.** Ghi cách chạy identity, recruitment, MailHog, DN-06/mock contract; ảnh chụp HR detail/rounds và STAFF list/detail; cập nhật README/ERD.
- [ ] **Step 4: Xác minh cuối.** `mvn -f server/pom.xml clean test`; chạy riêng `npm --prefix web run type-check`, `npm --prefix web run lint`, `npm --prefix web run build`, `npm --prefix web run test:e2e`. Kiểm tra MailHog và luồng live nếu dependencies có sẵn; nếu chưa, ghi rõ các hợp đồng còn chờ owner.
- [ ] **Step 5: Commit và PR.** `docs(UT-02): document application workflow and verification`; PR tiêu đề `[UT-02] ...` vào nhánh tích hợp đã xác nhận với trưởng nhóm, có cách chạy, fixture, ảnh, OpenAPI; yêu cầu một thành viên review trước mentor.

## Self-review

- Spec coverage: migration, map/ma trận, rounds/audit, offer gate, HR/STAFF rights, CV, identity/DN-06 aggregate, email, FE, OpenAPI, demo và PR đều có task.
- Type consistency: Task 1 tạo round/eligibility; Task 2 dùng và thêm transition; Task 3 thêm outbox/identity; Task 4 dùng identity và cung cấp response; Task 5 dùng response; Task 6 kiểm chứng.
- Review Focus: mỗi điều kiện trong năm dòng đầu có test ở Task 1–5.
- Scope: không triển khai công thức DN-06 hay API offer UT-07; hai owner cần ký hợp đồng trước integration live.
