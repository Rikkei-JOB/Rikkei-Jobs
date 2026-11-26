# UT-03 Contact Privacy Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** Bảo vệ email/phone ở BE, cho HR xin/xem liên hệ có audit, STAFF duyệt/tra cứu, ứng viên xem công ty đã truy cập.

**Architecture:** Recruitment sở hữu quyền, yêu cầu và log, dùng application theo company/user không lọc status. Chỉ `/contact` trả liên hệ cho HR sau khi log đã commit; các client nội bộ lấy dữ liệu từ identity/profile/company. FE dùng contact panel chung và các màn theo vai trò.

**Tech Stack:** Java 21, Spring Boot, JPA, Flyway, PostgreSQL, JUnit/MockMvc/Testcontainers; Next.js 16, React, TypeScript, Axios, Playwright hiện có.

**Spec:** `docs/superpowers/specs/2026-11-26-ut03-contact-privacy-design.md` — người dùng duyệt ngày 2026-11-26.

## Global Constraints

- PR vào `test`, tiêu đề bắt đầu `[UT-03]`; không merge tự động.
- Không triển khai DN-03: không thêm tìm kiếm/xếp hạng ứng viên, tổng hợp JD, shortlist hoặc invitation.
- Quyền thuộc cặp công ty–ứng viên; WITHDRAWN/REJECTED vẫn đủ quyền; APPROVED không hết hạn.
- companyId và hrId luôn lấy từ phiên HR đã xác thực, không nhận từ body/query.
- API danh sách/chi tiết không tự trả liên hệ; mỗi lần gọi `/contact` thành công tạo một dòng nhật ký.
- Lý do trim, bắt buộc, tối đa 2000 ký tự; size tối đa 100; ngày ISO-8601 có offset, UTC `[from,to)`.
- Không thêm service, hàng đợi, kho cache hoặc thư viện mới chỉ cho UT-03.
- Không tự sửa PDF/CV/cover letter hoặc che nội dung tự do; không thay đổi gửi email tự động hay hồ sơ tự xem của ứng viên.
- Đọc `web/AGENTS.md`, `DESIGN.md` và docs Next.js cục bộ trước khi viết FE.

## Review Focus

1. Đổi userId/công ty khi request liên hệ cũ còn chạy: không hiện response cũ ở hồ sơ mới (Task 6).
2. Hồ sơ thành PRIVATE sau APPROVED hoặc sau rút đơn: quyền hiện có còn nguyên; yêu cầu mới chưa có quyền bị chặn (Task 2–3).
3. HR bị khóa nhưng JWT chưa hết hạn hoặc companyId giả: endpoint mới vẫn đi qua kiểm tra HR hiện hành, không lộ contact (Task 4).
4. Client nội bộ timeout, trả UUID khác, hoặc DB commit thất bại: không trả contact, không báo thành công giả (Task 2–4).
5. Tên công ty bị đổi/xóa và lịch sử trải nhiều trang: vẫn hiện log bằng ID khi thiếu tên; FE không trình bày số liệu toàn lịch sử từ một trang (Task 5–7).

## File Map và quy ước đường dẫn

Các ký hiệu sau là prefix đường dẫn chính xác, không phải thư mục mới:

- `R` = `server/services/recruitment-service/src/main/java/com/rikkei/jobs/recruitment`
- `RT` = `server/services/recruitment-service/src/test/java/com/rikkei/jobs/recruitment`
- `P` = `server/services/profile-service/src/main/java/com/rikkei/jobs/profile`
- `PT` = `server/services/profile-service/src/test/java/com/rikkei/jobs/profile`
- `C` = `server/services/company-service/src/main/java/com/rikkei/jobs/company`
- `CT` = `server/services/company-service/src/test/java/com/rikkei/jobs/company`

| Phần | File chính |
| --- | --- |
| Persistence/quyền | recruitment migration V9; `R/entity/ContactRequest.java`, `ContactAccessLog.java`, `ContactRequestStatus.java`, `ContactAccessVia.java`; hai repository; `R/service/ContactAccessService.java` |
| Metadata nội bộ | `P/controller/InternalCandidateContactController.java`; `C/controller/InternalContactDirectoryController.java`; `R/client/ContactProfileClient.java`, `HttpContactProfileClient.java`, `ContactDirectoryClient.java`, `HttpContactDirectoryClient.java` |
| Nghiệp vụ/API | `R/service/ContactRequestService.java`, `CandidateContactService.java`, `ContactAccessLogService.java`; ba controller; `R/dto/contact/ContactDtos.java`; `R/config/ContactException.java`, `ContactExceptionHandler.java` |
| Chống rò dữ liệu | `R/service/HrApplicationReadService.java`, `R/dto/response/HrApplicationDetailResponse.java`; các DTO HR có contact phát hiện trong kiểm kê Task 4 |
| FE | `web/src/interface/contact.ts`, `web/src/services/contact.service.ts`, `web/src/app/components/contact/*`, bốn route UT-03, chi tiết đơn HR, navigation |
| Bàn giao | `docs/api/UT-03-contact-privacy.md`, `docs/api/ut03-openapi.yaml`, `docs/api/UT-03.http`, fixture/script demo và ảnh nghiệm thu |

## Task 1: Persistence và chính sách quyền dùng chung

**Files:** Create migration `server/services/recruitment-service/src/main/resources/db/migration/V9__contact_privacy.sql`; bốn entity/enum ở File Map; `R/repository/ContactRequestRepository.java`, `ContactAccessLogRepository.java`; `R/service/ContactAccessService.java`. Modify `R/repository/ApplicationRepository.java`, `RT/ApplicationIntegrationSupport.java`. Test `RT/ContactAccessPolicyTest.java`, `RT/ContactPrivacyPersistenceTest.java`.

**Interfaces:** `canSeeContact(UUID companyId, UUID userId): boolean`; `accessVia(UUID companyId, UUID userId): Optional<ContactAccessVia>` ưu tiên APPLICATION. Repository application thêm `existsByCompanyIdAndUserId(UUID companyId, UUID userId): boolean`. Request repository có `findForUpdate(UUID id): Optional<ContactRequest>`.

- [x] **1. Viết test RED.** Đặt tên và assertion: `withdrawnAndRejectedStillGrantAccess` → true; `anotherCompanyDenied` → false; `pendingAndRejectedRequestsDoNotGrant` → false; `applicationTakesPrecedence` → APPLICATION. Persistence: hai PENDING cùng company/user phải vi phạm unique; REJECTED rồi PENDING mới được phép; trạng thái/nguồn không hợp lệ bị CHECK từ chối.
- [x] **2. Chạy RED.** `mvn -f server/pom.xml -pl services/recruitment-service -am "-Dtest=ContactAccessPolicyTest,ContactPrivacyPersistenceTest" -Dsurefire.failIfNoSpecifiedTests=false test`. Phải fail vì type/schema chưa tồn tại.
- [x] **3. Implement migration và policy.** Đúng cột spec, thêm CHECK quan hệ status/reviewed fields/rejection reason, partial unique PENDING và index application(company_id,user_id). UUID/timestamps phía server; cleanup test xóa log/request. Kiểm tra V9 chưa bị task khác chiếm trước khi ghi migration.
- [x] **4. Chạy GREEN.** Lệnh bước 2: test PASS, Flyway và JPA validate PASS trên PostgreSQL, dùng Testcontainers hoặc `UT01_TEST_DB_URL` là DB test riêng như support hiện có.
- [x] **5. Commit.** `feat(UT-03): add contact access persistence and policy`.

## Task 2: Client metadata tối thiểu, xác minh PUBLIC và JD thuộc công ty

**Files:** Modify `P/controller/InternalCandidateContactController.java`; create `C/controller/InternalContactDirectoryController.java`; bốn client mới ở File Map; `R/dto/contact/ContactDtos.java`. Test `PT/InternalContactProfileApiTest.java`, `CT/InternalContactDirectoryApiTest.java`, `RT/ContactDependencyClientsTest.java`.

**Interfaces:** `ContactProfileClient.get(UUID userId, String bearer): ContactProfile` gọi GET `/internal/profiles/{userId}/contact-metadata`, trả `{userId,fullName,visibility}`, không contact. `ContactDirectoryClient.lookup(Set<UUID> companyIds, Set<UUID> hrIds): ContactDirectory` gọi POST `/internal/contact-directory/lookup`, trả hai map ID→tên từ CompanyRepository/CompanyHrRepository. Tiếp tục dùng `JobApplicationClient.getContext(UUID jobId): JobApplicationContext` để kiểm companyId, không áp điều kiện JD còn nhận ứng tuyển cho yêu cầu contact. Dùng `IdentityCandidateClient`/`CandidateContactClient` hiện có chỉ khi lấy contact hoặc nghiệp vụ nội bộ vốn cần chúng.

- [x] **1. Viết RED.** Metadata chỉ có userId/fullName/visibility; thiếu/sai internal key → 401; không tồn tại → 404; gateway không expose `/internal/**`. Client từ chối UUID không khớp/response rỗng, timeout → lỗi dependency. Directory chỉ trả tên, không email/phone; tối đa 100 ID mỗi tập; tên thiếu có thể fallback về ID.
- [x] **2. Chạy RED.** `mvn -f server/pom.xml -pl services/profile-service,services/company-service,services/recruitment-service -am "-Dtest=InternalContactProfileApiTest,InternalContactDirectoryApiTest,ContactDependencyClientsTest" -Dsurefire.failIfNoSpecifiedTests=false test`; kỳ vọng endpoint/client chưa có.
- [x] **3. Implement.** Theo RestClient/internal-key pattern hiện tại; profile endpoint kiểm JWT HR/STAFF/ADMIN và key như endpoint phone. Directory theo InternalKeyFilter của company, không đổi API identity. Dùng timeout/client config sẵn có; thông báo lỗi không chứa thông tin liên hệ. `ContactDtos` khai báo record ContactProfile, ContactDirectory và response dùng các task sau.
- [x] **4. Chạy GREEN.** Lệnh bước 2 PASS; kiểm tra JD công ty khác bị nhận diện theo `context.companyId`, metadata PRIVATE không bị hiểu thành PUBLIC.
- [x] **5. Commit.** `feat(UT-03): add internal contact metadata contracts`.

## Task 3: Tạo và duyệt yêu cầu mở liên hệ

**Files:** Create `R/service/ContactRequestService.java`, `R/controller/CandidateContactController.java`, `StaffContactRequestController.java`, `R/config/ContactException.java`, `ContactExceptionHandler.java`. Modify `R/dto/contact/ContactDtos.java`. Test `RT/ContactRequestApiTest.java`, `ContactRequestConcurrencyTest.java`.

**Interfaces:** `ContactRequestService.create(AuthPrincipal actor, UUID userId, UUID jobId, String reason, String bearer): ContactRequestView`; `status(AuthPrincipal actor, UUID userId, String bearer): ContactAccessView`; `list(ContactRequestStatus status, int page, int size): PageResponse<ContactRequestView>`; `review(AuthPrincipal staff, UUID id, boolean approved, String reason): ContactRequestView`.

`ContactAccessView` = `{userId,fullName,canSeeContact,via,requestStatus,latestRequest}`. `latestRequest` nullable và chỉ thuộc công ty actor. Với quyền cũ + PRIVATE vẫn cho status/contact; không có quyền + PRIVATE trả 404. `ContactRequestView` = `{id,companyId,companyName,userId,candidateName,jobId,reason,status,rejectionReason,createdBy,createdAt,reviewedBy,reviewedAt}`. Dùng directory/identity batch cho danh sách; không trả contact.

- [x] **1. Viết RED API.** HR POST yêu cầu → 201 PENDING; reason rỗng/2001 ký tự → 400; company khác sở hữu JD → 404; PRIVATE chưa có quyền → 404. Đã đủ quyền → 409 CONTACT_ALREADY_ACCESSIBLE; trùng pending → 409 CONTACT_REQUEST_PENDING; sau reject gửi lại được. STAFF approve/reject ghi đúng reviewedBy/time và rejectionReason; HR/CANDIDATE/ADMIN gọi duyệt → 403. Endpoint trạng thái không tạo log, không chứa contact.
- [x] **2. Chạy RED.** `mvn -f server/pom.xml -pl services/recruitment-service -am "-Dtest=ContactRequestApiTest,ContactRequestConcurrencyTest" -Dsurefire.failIfNoSpecifiedTests=false test`.
- [x] **3. Implement API.** Endpoint đúng spec, @PreAuthorize role tương ứng, trim trước validation; controller không tin companyId/body. Review trong transaction lấy khóa request; unique PENDING xử lý sau rollback transaction thành 409 đúng mã. Kiểm lại application/quyền trước insert; không thu hồi quyền khi PRIVATE. Phân trang 1-based theo PageResponse repo, size mặc định 20, tối đa 100.
- [x] **4. Chạy GREEN và race test.** Hai HR gửi đồng thời chỉ một PENDING; hai STAFF approve/reject chỉ một thành công, bên kia 409 CONTACT_REQUEST_ALREADY_REVIEWED; lần duyệt sau không ghi đè người/lý do duyệt trước. Lệnh bước 2 PASS.
- [x] **5. Commit.** `feat(UT-03): add contact requests and staff review`.

## Task 4: Chỉ trả contact qua endpoint có audit, đóng mọi đường rò có cấu trúc

**Files:** Create `R/service/CandidateContactService.java`; modify CandidateContactController, ContactDtos, HrApplicationReadService, HrApplicationDetailResponse, test HrApplicationReadApiTest; gateway `server/api-gateway/src/main/resources/application.yml`. Test `RT/CandidateContactApiTest.java`, `ContactResponsePrivacyTest.java`, `ContactCommitFailureTest.java`, `server/api-gateway/src/test/java/com/rikkei/jobs/gateway/ContactRouteIntegrationTest.java`.

**Interfaces:** `CandidateContactService.reveal(AuthPrincipal actor, UUID userId, String bearer): ContactView` trả `{email,phone}`. Method public `@Transactional` trên bean riêng được controller gọi qua Spring proxy: kiểm quyền, gọi dependency rồi lưu log; proxy commit trước khi controller nhận ContactView. Không self-invocation, không async logging, không bắt lỗi DB để tiếp tục trả contact. CandidateContactController expose GET `/candidates/{userId}/contact` và các path Task 3.

- [x] **1. Viết RED.** Không quyền → 403 CONTACT_LOCKED, logs=0 và không gọi contact dependency; cùng công ty sau WITHDRAWN/REJECTED → 200. Hai GET thành công → logs=2 đúng HR/company/user; cả hai căn cứ → APPLICATION. Phone thật null vẫn 200; dependency lỗi/UUID sai/commit lỗi → 5xx và không có contact trong response. HR khóa hoặc companyId không hợp lệ không được xem dù có request APPROVED.
- [x] **2. Chạy RED.** `mvn -f server/pom.xml -pl services/recruitment-service,api-gateway -am "-Dtest=CandidateContactApiTest,ContactResponsePrivacyTest,ContactCommitFailureTest,ContactRouteIntegrationTest" -Dsurefire.failIfNoSpecifiedTests=false test`.
- [x] **3. Implement reveal và route.** Thêm Cache-Control:no-store; log dùng clock hiện có, đúng một dòng mỗi request thành công. Route chính xác các path spec, không expose nội bộ, không chiếm route `/jobs/**` hoặc `/staff/profiles/**`. Đảm bảo HrLockedFilter áp dụng mọi path mới.
- [x] **4. Kiểm kê và che response.** Tìm mọi email/phone ứng viên trong DTO/mapper controller HR, kể cả lịch/offer/JSON lồng nhau. Với HR, map email/phone về null trong danh sách/chi tiết, thêm metadata `contactAccess` nếu cần hoặc dùng status endpoint riêng. STAFF/ADMIN và gửi mail nội bộ giữ nghiệp vụ hiện có. Test ContactResponsePrivacyTest dùng canary email/phone, assert không có canary trong JSON API HR ngoài `/contact`; không che interviewerEmail chỉ vì cùng tên trường email.
- [x] **5. Chạy GREEN.** Lệnh bước 2 và `HrApplicationReadApiTest,HrAccountLockIntegrationTest,InterviewQueryApiTest,HrOfferApiTest` PASS; route test chứng minh StripPrefix đúng và route cũ còn đúng. Commit failure test phải gây lỗi tại commit thật (deferred constraint trong DB test), không chỉ mock save ném lỗi.
- [x] **6. Commit.** `feat(UT-03): audit contact reads and redact HR responses`.

## Task 5: API nhật ký STAFF và ứng viên

**Files:** Create `R/service/ContactAccessLogService.java`, `R/controller/ContactAccessLogController.java`; modify log repository/ContactDtos. Test `RT/ContactAccessLogApiTest.java`.

**Interfaces:** `staffLogs(UUID companyId, UUID userId, Instant from, Instant to, int page, int size): PageResponse<ContactAccessLogView>`; `myLogs(AuthPrincipal candidate, int page, int size): PageResponse<ContactAccessLogView>`. View = `{id,companyId,companyName,hrId,hrName,userId,candidateName,via,createdAt}`; myLogs không bắt buộc hrName/candidateName, không trả email/phone. Endpoints đúng spec, Cache-Control:no-store.

- [x] **1. Viết RED.** STAFF lọc riêng và kết hợp company/user/from/to; thời điểm from được gồm, to không gồm. from>=to/size>100/page<1 → 400; offset thiếu hoặc sai → 400. Candidate chỉ thấy userId JWT, thêm query userId không mở rộng dữ liệu; HR/ADMIN không đọc API STAFF. Cùng timestamp phân trang ổn định theo id; công ty thiếu tên vẫn hiện log bằng ID, không bỏ mất log.
- [x] **2. Chạy RED.** `mvn -f server/pom.xml -pl services/recruitment-service -am -Dtest=ContactAccessLogApiTest -Dsurefire.failIfNoSpecifiedTests=false test`.
- [x] **3. Implement.** Query có điều kiện, sort createdAt DESC/id DESC; metadata lookup theo batch tối đa 100; không join DB xuyên service. Không cung cấp mutation/delete log. Không phát sinh log khi xem lịch sử hoặc trạng thái quyền.
- [x] **4. Chạy GREEN.** Lệnh bước 2 PASS; kiểm count log không đổi khi phân trang/lọc, dữ liệu cá nhân có no-store và không có email/phone.
- [x] **5. Commit.** `feat(UT-03): expose staff and candidate contact logs`.

## Task 6: HR contact panel và trang liên hệ độc lập

**Files:** Create `web/src/interface/contact.ts`, `web/src/services/contact.service.ts`, `web/src/app/components/contact/candidate-contact-panel.tsx`, `request-contact-dialog.tsx`, `hr-contact-page.tsx`, `web/src/app/(pages)/(app)/hr/candidates/[userId]/contact/page.tsx`; modify `web/src/app/components/recruitment/hr-application-detail-view.tsx`, `web/src/interface/hr-application.ts`. Test `web/e2e/ut03-hr-contact.spec.ts`.

**Interfaces:** `CandidateContactPanel({userId,jobId?}: {userId:string;jobId?:string})`; contactService có `status(userId,signal?)`, `request(userId,{jobId,reason})`, `reveal(userId)` và methods list/review/logs theo backend. Types TS khớp ContactDtos và PageResponse. API `/contact` chỉ gọi trong click handler, không React Query/prefetch/retry. Dropdown JD tái sử dụng API GET `/companies/me/jobs` và service hiện có.

- [x] **1. Viết RED E2E.** Fixture đầy đủ LOCKED/PENDING/REJECTED/ALLOWED; initial render/status/reload → contact requests=0. Xin mở bắt lý do, gửi đúng JD, pending khóa gửi trùng, reject cho gửi lại. Allowed có “Xem liên hệ”, click → một call rồi hiển thị; double click đang loading vẫn một call; “Xem lại” thêm một call. Lỗi 403 xóa contact và reload status, lỗi 5xx không hiện contact. Đổi ứng viên hoặc đăng xuất trước response cũ → không hiện dữ liệu cũ; email/phone null không làm UI crash.
- [x] **2. Chạy RED.** `npm --prefix web run test:e2e -- e2e/ut03-hr-contact.spec.ts`; kỳ vọng thiếu component/route.
- [x] **3. Implement UI.** Nhãn đúng “Liên hệ: đã ẩn”, “Xin mở liên hệ”, “Xem liên hệ”; dialog validation trim/2000; state contact chỉ sống trong component theo user/company/session key. Chặn request trùng bằng in-flight guard, bỏ response cũ sau unmount/session đổi, không lưu browser storage. Profile không tồn tại/PRIVATE chưa có quyền hiển thị 404 thân thiện. Gắn vào chi tiết đơn HR, giữ STAFF/ADMIN view theo role.
- [x] **4. Chạy GREEN.** Lệnh bước 2, `npm --prefix web run type-check`, eslint các file thay đổi PASS. Test bàn phím: mở/đóng dialog, focus quay về trigger, lỗi gắn đúng field.
- [x] **5. Commit.** `feat(UT-03): add HR contact request and reveal controls`.

## Task 7: Màn STAFF, lịch sử ứng viên và nghiệm thu xuyên luồng

**Files:** Create `web/src/app/components/contact/staff-contact-requests.tsx`, `staff-contact-access-logs.tsx`, `candidate-contact-access-logs.tsx`; route `web/src/app/(pages)/(app)/staff/contact-requests/page.tsx`, `staff/contact-access-logs/page.tsx`, `candidate/contact-access-logs/page.tsx`; modify `web/src/config/menu.ts` và navigation ứng viên thực sự đang dùng. Create `web/e2e/ut03-contact-workflow.spec.ts`, `ut03-contact.visual.spec.ts`; tài liệu API ở File Map, `scripts/ut03-contact-demo.ps1`, fixture `docs/api/fixtures/ut03-contact-demo.sql`.

**Interfaces:** FE dùng contactService của Task 6; STAFF list/status/approve/reject và staffLogs; ứng viên myLogs. Bộ lọc ngày đổi đầu ngày Việt Nam sang offset +07:00, to là đầu ngày sau ngày kết thúc. Candidate nhóm công ty trong trang hiện tại, không ghi “tổng lượt”/“lần xem cuối” toàn lịch sử từ dữ liệu trang hiện tại.

- [x] **1. Viết RED.** STAFF lọc PENDING, duyệt, từ chối bắt lý do, nhận 409 thì refresh. Nhật ký lọc companyId/userId/ngày, đổi bộ lọc về trang 1, không hiển thị response cũ khi đổi lọc nhanh. Candidate chỉ thấy công ty của mình, log lặp hiện đủ, empty/error/pagination rõ. E2E đổi vai trò theo fixture: HR xin → STAFF duyệt → HR xem hai lần → ứng viên thấy hai log.
- [x] **2. Chạy RED.** `npm --prefix web run test:e2e -- e2e/ut03-contact-workflow.spec.ts e2e/ut03-contact.visual.spec.ts`; kỳ vọng thiếu màn/nav.
- [x] **3. Implement các màn và navigation.** Thêm STAFF vào ROLE_MENU và ROLE_NAV_GROUPS, ứng viên vào menu/profile account theo pattern thực có. Không dựng DN-03; seed URL HR theo userId cho demo. Filters xác định công ty/ứng viên bằng ID được gắn label rõ; không thêm tìm kiếm ứng viên trá hình. Render text an toàn, lý do dài wrap, phân trang metadata rõ.
- [x] **4. Kiểm tra hình ảnh.** Playwright chụp 1440×900, 768×1024, 390×844 cho panel/dialog, duyệt STAFF, nhật ký STAFF và ứng viên; mở ảnh để kiểm tra overflow, focus, lỗi/empty/loading, lý do 2000 ký tự và tên dài. Sửa rồi chụp lại phần bị ảnh hưởng.
- [x] **5. Kiểm chứng backend/FE.** `mvn -f server/pom.xml -pl services/recruitment-service,services/profile-service,services/company-service,api-gateway -am test`; `npm --prefix web run type-check`; `npm --prefix web run lint`; `npm --prefix web run build`; `npm --prefix web run test:e2e -- e2e/ut03-hr-contact.spec.ts e2e/ut03-contact-workflow.spec.ts e2e/ut03-contact.visual.spec.ts`. Nếu lỗi nền đã có, phân biệt bằng bằng chứng và sửa lỗi thuộc phạm vi, không bỏ qua thất bại UT-03.
- [x] **6. Demo API thật.** Seed chỉ trên DB demo được chỉ định; script gọi gateway với hai công ty và HR tương ứng, ứng viên PUBLIC chưa/có đơn, yêu cầu ba trạng thái. Chứng minh 403 khóa, cross-company, quyền sau rút/từ chối, STAFF approve, hai contact calls→hai DB logs, ứng viên đọc log mình. UI fixture test không thay thế kiểm chứng API thật.
- [x] **7. Viết bàn giao.** OpenAPI/HTTP mẫu, migration/seed/cách chạy, account/URL demo, kết quả và ảnh kiểm chứng; giải thích contact JSON bảo vệ còn nội dung CV tự do không sửa. Đánh dấu design đã duyệt; cập nhật checklist kế hoạch theo thực tế.
- [x] **8. Commit và PR.** `feat(UT-03): add contact review and access history screens`; review toàn diff, `git diff --check`, xác minh không có file ngoài UT-03. Push feature branch, mở PR `[UT-03] Bảo vệ liên hệ ứng viên và ghi nhật ký truy cập` vào `test`, mô tả hành vi, migration và kiểm chứng thực chạy. Trả link PR; không merge.

## Thứ tự và handoff

Thực hiện Task 1 → 2 → 3 → 4 → 5 → 6 → 7. Khi bắt đầu execution, dùng skill worktree để có checkout riêng từ nhánh `feature/UT-03-contact-privacy`, giữ nguyên file chưa được theo dõi của task khác. Không chạy seed trên dữ liệu người dùng. Không cài dependency nếu công cụ hiện có đã đủ.

Kế hoạch đã tự đối chiếu spec: quyền/log/API/FE/phân trang/lỗi đồng thời/gateway/demo/PR đều có task; các trường hợp Review Focus đã có test ở task sở hữu. Chưa chạy test sản phẩm vì hiện chỉ tạo tài liệu kế hoạch, chưa có implementation.

## Kết quả thực thi 2026-11-26

DB/BE/FE và tự review đã hoàn tất, không dùng subagent theo yêu cầu người dùng. Kết quả kiểm chứng và các điều chỉnh triển khai: `docs/api/UT-03-verification.md`. PR đã mở vào test: https://github.com/Rikkei-JOB/Rikkei-Jobs/pull/41; chưa merge.
