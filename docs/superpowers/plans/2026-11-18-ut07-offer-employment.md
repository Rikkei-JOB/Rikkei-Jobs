# UT-07 Offer and Employment Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** HR gửi offer hợp lệ, ứng viên nhận hoặc từ chối trước hạn, và công việc được ghi ngay vào hồ sơ khi nhận.

**Architecture:** `recruitment-service` sở hữu offer, đơn và employment record trong cùng PostgreSQL schema. Các thay đổi trạng thái dùng khóa hàng đơn và outbox UT-02; FE đọc thêm kinh nghiệm từ recruitment trên trang hồ sơ cá nhân.

**Tech Stack:** Java 21, Spring Boot 3.3, PostgreSQL/Flyway/JPA, JUnit 5/MockMvc, Next.js 16/React 19/TypeScript, Playwright.

**Spec:** `docs/superpowers/specs/2026-11-18-ut07-offer-employment-design.md`

## Global Constraints

- Mọi endpoint mới dưới gateway `/api/v1/recruitment`, JSON camelCase, lỗi `{code,message}`.
- Offer chỉ gửi từ đơn `INTERVIEW`, có vòng, không có vòng `PENDING`, vòng `round_no` lớn nhất `PASS`; sai trả `409`.
- Lương là số nguyên VND/tháng **gross**, `BIGINT > 0`, không hiển thị trên kinh nghiệm; HR công ty đúng và ứng viên sở hữu mới xem nội dung offer.
- Ứng viên khác và HR sai công ty nhận `404`; ID người dùng và công ty lấy từ JWT.
- Nhận/từ chối chỉ khi `SENT` và giờ server **nhỏ hơn** `response_deadline`; job hàng giờ đánh dấu offer đến hạn `EXPIRED`.
- Nhận tạo record ngay; `confirmed_at` là thời điểm HR xác nhận, không sửa `start_date` dự kiến.
- Các đơn khác giữ nguyên; ở `OFFER` dùng từ chối có lý do, các bước trước dùng rút đơn UT-01.
- PR `[UT-07] ...` vào `test`, kèm cách chạy, dữ liệu mẫu, ảnh, OpenAPI. Không commit trực tiếp lên `test`; tạo feature worktree từ `test` khi bắt đầu thực hiện và đưa spec/plan đã duyệt vào worktree đó. Giữ nguyên thay đổi `.gitignore` đã có trong workspace.
- Trước khi sửa FE, đọc `web/AGENTS.md` và tài liệu liên quan trong `web/node_modules/next/dist/docs/`.

## Review Focus

1. Deadline có offset hoặc đúng ranh giới: server so sánh instant, `now == deadline` từ chối dù scheduler chưa chạy (Task 3).
2. ID offer của ứng viên khác: cả GET, accept, decline đều `404`, không phân biệt tồn tại hay không (Task 3).
3. Hai request nhận/từ chối đồng thời và job quá hạn: chỉ một trạng thái cuối cùng, nhiều nhất một employment record (Task 3 và 4).
4. STAFF/ADMIN vẫn đọc được đơn UT-02 nhưng không thấy lương, ghi chú hay lý do offer (Task 5).
5. Lương có dấu phân cách hoặc số thập phân từ form: chỉ gửi số nguyên VND, email và UI ghi gross/tháng; dữ liệu sai bị chặn (Task 2 và 6).

---

## File Map

| Phần | File chính |
| --- | --- |
| Data/domain | `server/services/recruitment-service/src/main/resources/db/migration/V4__offers_employment_records.sql`; `src/main/java/com/rikkei/jobs/recruitment/entity/{Offer,OfferStatus,EmploymentRecord}.java`; `repository/{OfferRepository,EmploymentRecordRepository}.java` |
| Command API | `.../service/OfferCommandService.java`, `.../controller/{HrOfferController,CandidateOfferController}.java`, DTO request/response |
| Job/đọc | `.../service/{OfferExpiryJob,OfferQueryService}.java`; `.../controller/CandidateEmploymentController.java`; `HrApplicationReadService.java` |
| Email | `.../service/impl/SmtpApplicationStatusMailSender.java`; test email hiện có |
| FE | `web/src/interface/offer.ts`, `web/src/services/{offer,hr-application}.service.ts`, `web/src/app/components/{recruitment,candidate-offers,profile}/*`, các route candidate/HR, `web/src/config/menu.ts` |
| Hợp đồng/bàn giao | `docs/api/ut07-openapi.yaml`, `docs/api/UT-07-offer-employment.md`, `docs/api/screenshots/*` |

Các đường dẫn rút gọn `.../` trong bảng là dưới `server/services/recruitment-service/src/main/java/com/rikkei/jobs/recruitment/`.

### Task 1: Migration và domain

**Files:**
- Create: `server/services/recruitment-service/src/main/resources/db/migration/V4__offers_employment_records.sql`
- Create: `server/services/recruitment-service/src/main/java/com/rikkei/jobs/recruitment/entity/Offer.java`, `OfferStatus.java`, `EmploymentRecord.java`
- Create: `server/services/recruitment-service/src/main/java/com/rikkei/jobs/recruitment/repository/OfferRepository.java`, `EmploymentRecordRepository.java`
- Modify: `server/services/recruitment-service/src/test/java/com/rikkei/jobs/recruitment/ApplicationIntegrationSupport.java` (cleanup đúng thứ tự FK)
- Test: `server/services/recruitment-service/src/test/java/com/rikkei/jobs/recruitment/OfferPersistenceTest.java`

**Interfaces:** `OfferRepository.findByApplicationId(UUID): Optional<Offer>`, `findByApplicationIdForUpdate(UUID): Optional<Offer>`; `EmploymentRecordRepository.findByApplicationId(UUID): Optional<EmploymentRecord>`, `findByUserIdOrderByStartDateDesc(UUID): List<EmploymentRecord>`. `offers.application_id` và `employment_records.application_id` UNIQUE; `Offer.salary: Long`, `Offer.responseDeadline: Instant`, `EmploymentRecord.startDate: LocalDate`.

- [ ] **Step 1: Viết test fail.** `OfferPersistenceTest` lưu offer và employment record, assert FK/UNIQUE theo `application_id`, salary 0 bị DB chặn, các enum status map đúng; cleanup test không lỗi FK.
- [ ] **Step 2: Chạy RED.** `mvn -f server/pom.xml -pl services/recruitment-service -am "-Dtest=OfferPersistenceTest" -Dsurefire.failIfNoSpecifiedTests=false test`; kỳ vọng thiếu migration/type hoặc assertion fail.
- [ ] **Step 3: Tạo migration, entity và repository.** Dùng schema `recruitment`, CHECK cho status và salary; index `(status,response_deadline)` và `user_id`. `companyName` của record đọc qua application snapshot, không nhân bản.
- [ ] **Step 4: Chạy GREEN.** Lệnh Step 2 PASS, Flyway và JPA validation PASS.
- [ ] **Step 5: Commit trên feature branch.** `feat(UT-07): add offer and employment tables`.

### Task 2: HR gửi offer

**Files:**
- Create: `server/services/recruitment-service/src/main/java/com/rikkei/jobs/recruitment/service/OfferCommandService.java`
- Create: `server/services/recruitment-service/src/main/java/com/rikkei/jobs/recruitment/controller/HrOfferController.java`
- Create: `server/services/recruitment-service/src/main/java/com/rikkei/jobs/recruitment/dto/request/CreateOfferRequest.java`
- Create: `server/services/recruitment-service/src/main/java/com/rikkei/jobs/recruitment/dto/response/OfferResponse.java`
- Test: `server/services/recruitment-service/src/test/java/com/rikkei/jobs/recruitment/HrOfferApiTest.java`

**Interfaces:** `OfferCommandService.send(AuthPrincipal actor, UUID applicationId, CreateOfferRequest request): OfferResponse`; request `(String position, Long salary, LocalDate startDate, String workLocation, OffsetDateTime responseDeadline, String note)`; `POST /recruitment/applications/{id}/offer` trả `201` và `OfferResponse` gồm các trường offer, trạng thái `SENT`, `respondedAt`, `declineReason`.

- [ ] **Step 1: Viết test fail.** `HrOfferApiTest`: HR đúng công ty, đơn INTERVIEW với last PASS và không PENDING tạo đúng một offer, đơn `OFFER`, một history/outbox; chưa PASS, PENDING, sai status, deadline quá khứ, salary 0/số thập phân, gửi trùng trả `400` hoặc `409` theo loại lỗi; HR sai công ty `404`, STAFF `403`.
- [ ] **Step 2: Chạy RED.** `mvn -f server/pom.xml -pl services/recruitment-service -am "-Dtest=HrOfferApiTest" -Dsurefire.failIfNoSpecifiedTests=false test`; kỳ vọng endpoint/type chưa có.
- [ ] **Step 3: Implement.** Khóa `applications.findForUpdate`, kiểm tra công ty và `OfferEligibilityPolicy`, lưu offer rồi dùng `ApplicationTransitionService.transition` trong cùng transaction. Validation tại DTO/service; không tạo email trực tiếp.
- [ ] **Step 4: Chạy GREEN.** Lệnh Step 2 PASS, chỉ một event status cho offer.
- [ ] **Step 5: Commit.** `feat(UT-07): let HR send eligible offers`.

### Task 3: Ứng viên xem và phản hồi, ghi kinh nghiệm

**Files:**
- Modify: `server/services/recruitment-service/src/main/java/com/rikkei/jobs/recruitment/service/OfferCommandService.java`
- Create: `server/services/recruitment-service/src/main/java/com/rikkei/jobs/recruitment/service/OfferQueryService.java`
- Create: `server/services/recruitment-service/src/main/java/com/rikkei/jobs/recruitment/controller/CandidateOfferController.java`
- Create: `server/services/recruitment-service/src/main/java/com/rikkei/jobs/recruitment/dto/request/DeclineOfferRequest.java`
- Test: `server/services/recruitment-service/src/test/java/com/rikkei/jobs/recruitment/CandidateOfferApiTest.java`, `OfferResponseConcurrencyTest.java`

**Interfaces:** `OfferCommandService.accept(AuthPrincipal actor, UUID offerId): OfferResponse`, `decline(AuthPrincipal actor, UUID offerId, String reason): OfferResponse`; `OfferQueryService.listOwn(UUID userId, int page, int size): PageResponse<OfferResponse>`, `getOwn(UUID userId, UUID offerId): OfferResponse`. Routes `GET /recruitment/me/offers`, `GET /recruitment/me/offers/{id}`, `POST /recruitment/me/offers/{id}/accept`, `POST /recruitment/me/offers/{id}/decline`.

- [ ] **Step 1: Viết test fail.** Chủ offer đọc/trả lời được; người khác GET/accept/decline `404`; accept tạo `HIRED`, history/outbox và một record; decline thiếu/trắng lý do `400`, hợp lệ tạo `DECLINED`/`REJECTED` và HR đọc được lý do; `now == deadline`/sau deadline `409`; hai request đồng thời chỉ một thắng; đơn khác của ứng viên giữ nguyên.
- [ ] **Step 2: Chạy RED.** `mvn -f server/pom.xml -pl services/recruitment-service -am "-Dtest=CandidateOfferApiTest,OfferResponseConcurrencyTest" -Dsurefire.failIfNoSpecifiedTests=false test`; kỳ vọng fail endpoint/logic.
- [ ] **Step 3: Implement.** Tra offer theo chủ trước khi phản hồi; khóa application rồi offer theo thứ tự chung, kiểm `SENT` và giờ server; trong một transaction cập nhật offer, chuyển đơn qua `ApplicationTransitionService`, tạo record khi nhận. Lý do trim 1–1.000 ký tự. UNIQUE `application_id` chặn lặp.
- [ ] **Step 4: Chạy GREEN.** Lệnh Step 2 PASS, race test chỉ có một trạng thái kết thúc.
- [ ] **Step 5: Commit.** `feat(UT-07): let candidates respond to offers`.

### Task 4: Hết hạn và xác nhận đi làm

**Files:**
- Create: `server/services/recruitment-service/src/main/java/com/rikkei/jobs/recruitment/service/OfferExpiryJob.java`
- Create: `server/services/recruitment-service/src/main/java/com/rikkei/jobs/recruitment/dto/response/EmploymentRecordResponse.java`
- Modify: `server/services/recruitment-service/src/main/java/com/rikkei/jobs/recruitment/service/OfferCommandService.java`, `controller/HrOfferController.java`
- Test: `server/services/recruitment-service/src/test/java/com/rikkei/jobs/recruitment/OfferExpiryJobTest.java`, `OnboardConfirmationApiTest.java`

**Interfaces:** `OfferExpiryJob.expireDue(): int`, cron mỗi giờ; `OfferCommandService.confirmOnboard(AuthPrincipal actor, UUID applicationId): EmploymentRecordResponse`; `POST /recruitment/applications/{id}/offer/confirm-onboard` trả thời điểm `confirmedAt`.

- [ ] **Step 1: Viết test fail.** Offer SENT đến hạn chuyển EXPIRED/REJECTED và một history/outbox; job chạy lại không đổi, ứng viên đã nhận/từ chối không bị expire; race job với accept giữ một trạng thái cuối. Confirm trước ACCEPTED `409`, sai công ty `404`, sau ACCEPTED ghi timestamp server; gọi lặp giữ nguyên timestamp.
- [ ] **Step 2: Chạy RED.** `mvn -f server/pom.xml -pl services/recruitment-service -am "-Dtest=OfferExpiryJobTest,OnboardConfirmationApiTest" -Dsurefire.failIfNoSpecifiedTests=false test`; kỳ vọng fail.
- [ ] **Step 3: Implement.** Job chọn các ID quá hạn theo lô, mỗi ID khóa application trước offer và kiểm lại điều kiện; dùng actor hệ thống cố định cho history, không giả HR. Confirm khóa application, kiểm quyền và trạng thái, chỉ điền `confirmed_at` null.
- [ ] **Step 4: Chạy GREEN.** Lệnh Step 2 PASS; chạy lại job không sinh thêm event.
- [ ] **Step 5: Commit.** `feat(UT-07): expire offers and confirm onboarding`.

### Task 5: API đọc HR, kinh nghiệm và email offer

**Files:**
- Modify: `server/services/recruitment-service/src/main/java/com/rikkei/jobs/recruitment/service/HrApplicationReadService.java`, `dto/response/HrApplicationDetailResponse.java`
- Create: `server/services/recruitment-service/src/main/java/com/rikkei/jobs/recruitment/controller/CandidateEmploymentController.java`
- Modify: `server/services/recruitment-service/src/main/java/com/rikkei/jobs/recruitment/service/impl/SmtpApplicationStatusMailSender.java`
- Test: `server/services/recruitment-service/src/test/java/com/rikkei/jobs/recruitment/OfferReadApiTest.java`, `SmtpApplicationStatusMailSenderTest.java`

**Interfaces:** `GET /recruitment/me/employment-records: List<EmploymentRecordResponse>` với `companyName` từ application snapshot; `HrApplicationDetailResponse.offer: OfferResponse | null` chỉ non-null cho HR đúng công ty. Mail sender dùng event `toStatus=OFFER` và offer cùng application, không tạo outbox mới.

- [ ] **Step 1: Viết test fail.** HR thấy offer, hạn, trạng thái và lý do từ chối; STAFF/ADMIN đọc đơn cũ nhưng `offer:null`; HR sai công ty `404`; ứng viên chỉ thấy record của mình, có vị trí/công ty/ngày, không có salary; email offer có lương gross VND/tháng, hạn và link, đúng một event; retry không nhân đôi event.
- [ ] **Step 2: Chạy RED.** `mvn -f server/pom.xml -pl services/recruitment-service -am "-Dtest=OfferReadApiTest,SmtpApplicationStatusMailSenderTest" -Dsurefire.failIfNoSpecifiedTests=false test`; kỳ vọng fail.
- [ ] **Step 3: Implement.** Bổ sung `offer` có kiểm role vào HR detail; API candidate dùng userId JWT và join application snapshot; sender nhận hoặc truy vấn offer theo application ID khi history sang OFFER và dựng một email đầy đủ. Giữ Message-ID theo outbox ID.
- [ ] **Step 4: Chạy GREEN.** Lệnh Step 2 PASS; chạy `mvn -f server/pom.xml -pl services/recruitment-service -am test` để kiểm regression UT-01/02.
- [ ] **Step 5: Commit.** `feat(UT-07): show offer and employment details`.

### Task 6: FE HR, ứng viên và hồ sơ

**Files:**
- Create: `web/src/interface/offer.ts`, `web/src/services/offer.service.ts`
- Modify: `web/src/interface/hr-application.ts`, `web/src/services/hr-application.service.ts`, `web/src/app/components/recruitment/application-workflow-detail.tsx`, `web/src/app/(pages)/(app)/candidate/profile/page.tsx`, `web/src/config/menu.ts`
- Create: `web/src/app/components/recruitment/offer-form.tsx`, `web/src/app/components/candidate-offers/offer-detail.tsx`, `web/src/app/components/profile/employment-records.tsx`
- Create: `web/src/app/(pages)/(app)/candidate/offers/page.tsx`, `web/src/app/(pages)/(app)/candidate/offers/[id]/page.tsx`
- Test: `web/e2e/ut07-offer-flow.spec.ts`

**Interfaces:** `offerService.list/detail/accept/decline/listEmployment` gọi các route Task 3/5; `hrApplicationService.sendOffer/confirmOnboard` gọi Task 2/4. UI dùng `OfferResponse` chung cho HR và ứng viên, `salary` là number nguyên.

- [ ] **Step 1: Viết test fail.** Playwright: HR thấy form khi last PASS, nhập salary 12.000.000 hiển thị/gửi `12000000`, gửi rồi thấy SENT; candidate mở danh sách/chi tiết, đếm ngược, nhận rồi profile hiện “<vị trí> tại <công ty>”; decline bắt buộc lý do và HR thấy; sau hạn nút khóa, API `409` hiển thị và tải lại.
- [ ] **Step 2: Chạy RED.** `npm --prefix web run test:e2e -- ut07-offer-flow.spec.ts`; kỳ vọng thiếu route/component.
- [ ] **Step 3: Implement.** Đọc `web/AGENTS.md` và Next docs trước; thêm form HR và trạng thái/confirm, trang offer và menu ứng viên, mục kinh nghiệm; parse/format tiền bằng số nguyên, hiển thị “VND/tháng (gross)”; đếm ngược từ deadline ISO, xử lý `409` và refresh. Không mở rộng trang hồ sơ công khai.
- [ ] **Step 4: Chạy GREEN.** Lệnh Step 2 PASS; `npm --prefix web run type-check`, `npm --prefix web run lint`, `npm --prefix web run build` PASS.
- [ ] **Step 5: Commit.** `feat(UT-07): add HR and candidate offer screens`.

### Task 7: OpenAPI, mẫu, ảnh và PR

**Files:**
- Create: `docs/api/ut07-openapi.yaml`, `docs/api/UT-07-offer-employment.md`
- Create: ảnh thực tế trong `docs/api/screenshots/` với tên `ut07-hr-offer.png`, `ut07-candidate-offer.png`, `ut07-profile-employment.png`
- Modify: `README.md` (liên kết tài liệu UT-07)
- Add: seed/fixture dưới `server/services/recruitment-service/src/test/` hoặc script demo trong `scripts/` theo cơ chế seed hiện có, không hardcode vào production startup.

**Interfaces:** OpenAPI mô tả đúng các endpoint Task 2–5, payload salary integer gross VND/month, `responseDeadline` ISO timestamp, 400/404/409 và Bearer auth. Tài liệu chứa cách chạy, dữ liệu mẫu bốn trạng thái, lệnh kiểm chứng và ảnh.

- [ ] **Step 1: Viết checklist đối chiếu hợp đồng.** Liệt kê 7 route mới và phần mở rộng `GET /applications/{id}`, trường request/response, quyền và 400/404/409; xác định fixture cho SENT/ACCEPTED/DECLINED/EXPIRED.
- [ ] **Step 2: Tạo OpenAPI, dữ liệu demo, tài liệu và chụp màn hình.** Dùng ứng dụng chạy thật hoặc Playwright để tạo ảnh HR, offer ứng viên và kinh nghiệm; ghi cách chạy lại.
- [ ] **Step 3: Kiểm chứng.** Đối chiếu OpenAPI với controller/DTO; chạy `mvn -f server/pom.xml -pl services/recruitment-service -am test`, `npm --prefix web run type-check`, `npm --prefix web run lint`, `npm --prefix web run build`, Playwright UT-07. Mọi lệnh PASS hoặc ghi rõ blocker môi trường.
- [ ] **Step 4: Commit.** `docs(UT-07): document offer flow and evidence`.
- [ ] **Step 5: Mở PR.** Push `feature/ut-07-offer-employment`, mở PR base `test`, tiêu đề bắt đầu `[UT-07]`; body có cách chạy, dữ liệu mẫu, ảnh và link OpenAPI.
