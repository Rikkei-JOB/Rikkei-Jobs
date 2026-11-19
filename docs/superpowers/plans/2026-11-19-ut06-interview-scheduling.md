# UT-06 Interview Scheduling Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** HR đề xuất 1–5 khung, ứng viên chọn giờ không trùng lịch, nhận phòng Meet và email/ICS, được nhắc lịch, HR theo dõi lịch tuần của công ty.

**Architecture:** recruitment-service sở hữu slot, schedule và notification outbox trong PostgreSQL. Ghi lịch dùng application/round locks và advisory locks theo ứng viên/người phỏng vấn; gọi Meet có idempotency trước khi commit lịch và email outbox. Next.js dùng API thật, UTC ở API và Asia/Ho_Chi_Minh trên UI.

**Tech Stack:** Java 21, Spring Boot, PostgreSQL 16, Flyway/JPA/JdbcTemplate, JUnit/MockMvc/Testcontainers, Express/node:test, Next.js/React/TypeScript và Playwright hiện có.

**Spec:** `docs/superpowers/specs/2026-11-19-ut06-interview-scheduling-design.md` (đã được người dùng duyệt).

## Global Constraints

- PR vào `test`, tiêu đề bắt đầu `[UT-06]`; không push trực tiếp nhánh tích hợp. Khi bắt đầu thực thi dùng skill using-git-worktrees để kiểm tra và tạo môi trường cô lập từ test, mang spec/plan sang đó và giữ thay đổi có sẵn của người dùng.
- Mỗi lần đề xuất gồm 1–5 khung; mỗi khung dài ít nhất 30 phút, bắt đầu trong tương lai. Danh sách có lỗi bị từ chối toàn bộ với 400.
- Khoảng giờ dùng quy ước [start, end), cho phép hai buổi nối tiếp. OPEN chưa giữ chỗ.
- Chọn hoặc đổi lịch chỉ khi application INTERVIEW, round PENDING. Sai role 403, ngoài ownership 404, tài nguyên không còn hợp lệ/xung đột 409.
- HR gửi khung mới hợp lệ thì huỷ lịch cũ ngay; ứng viên phải chọn lại. Đề xuất mới không hợp lệ giữ nguyên lịch cũ.
- Xác nhận, huỷ lịch và nhắc gửi cho ứng viên cùng interviewerEmail; chuẩn hoá và loại trùng email. Lời mời chọn giờ chỉ gửi ứng viên.
- Job mỗi 15 phút: mốc 24h áp dụng khi 1h < thời gian còn lại <= 24h; mốc 1h khi 0 < thời gian còn lại <= 1h. Đặt khi còn <=1h thì xác nhận thay nhắc 1h. Không gửi nhắc lịch huỷ, DONE hoặc đã bắt đầu.
- HR xem lịch toàn công ty, có lọc người phỏng vấn. Không lấy companyId từ client để quyết định quyền.
- Backend prefix `/recruitment`, gateway `/api/v1/recruitment`; JSON camelCase, lỗi theo `{code,message}` hiện có. Dùng Clock injectable và TIMESTAMPTZ, request thời gian có offset.
- Giữ giới hạn SMTP được nêu trong spec: không cam kết exactly-once qua crash hoặc thứ tự tới hộp thư; không tuyên bố URL Meet đã gửi bị vô hiệu hoá khi huỷ.
- Trước sửa FE đọc `web/AGENTS.md` và tài liệu Next.js liên quan tại `web/node_modules/next/dist/docs/`. Không thêm thư viện lịch hoặc mail provider.

## Review Focus

1. Mốc giờ có offset khác nhau nhưng cùng instant phải được coi là trùng, kể cả người phỏng vấn có email khác chữ hoa/thường (Task 3).
2. SlotId hợp lệ thuộc vòng khác hoặc lịch cũ không được dùng để lấy URL Meet hoặc đặt giờ (Task 3).
3. SMTP nhận xác nhận rồi crash trước SENT: khi huỷ vẫn phải tạo thông báo CANCEL cho người nhận có lần gửi chưa rõ kết quả (Task 5).
4. HR chuyển tuần khi response tuần trước còn đang về: UI không hiển thị nhầm tuần; phải lấy đủ trang (Task 7).
5. Trạng thái PASS/FAIL/PENDING và giờ UT-02 cũ không được tự mở lại lịch DONE hoặc tự sinh phòng (Task 4).

## File Map và hợp đồng chung

Đặt `R = server/services/recruitment-service`, `J = R/src/main/java/com/rikkei/jobs/recruitment`, `T = R/src/test/java/com/rikkei/jobs/recruitment`; mọi đường dẫn dùng các ký hiệu này được mở rộng đúng thư mục trên.

| Phần | File |
| --- | --- |
| Migration/domain | `R/src/main/resources/db/migration/V6__interview_scheduling.sql`, `J/entity/Interview{Slot,Schedule,NotificationOutbox}.java`, enum trạng thái tương ứng |
| Persistence/locks | `J/repository/Interview{Slot,Schedule,NotificationOutbox}Repository.java`, `J/service/InterviewResourceLock.java` |
| Commands/reads | `J/service/Interview{SchedulingService,QueryService,LifecycleService}.java`, `J/controller/{HrInterviewController,CandidateInterviewController}.java` |
| DTO | `J/dto/request/InterviewSlotRequest.java`; `J/dto/response/Interview{SlotResponse,ScheduleResponse,ProposalResponse,InvitationResponse,CalendarItemResponse}.java` |
| Meet | `J/client/{MeetRoomClient,HttpMeetRoomClient}.java`, `mock/meet-rooms.js`, `mock/server.js` |
| Email | `J/service/{InterviewReminderJob,InterviewNotificationWorker,InterviewMailSender,InterviewCalendarWriter}.java`, `J/service/impl/SmtpInterviewMailSender.java` |
| Integration | `J/service/InterviewRoundService.java`, `J/service/impl/ApplicationTransitionServiceImpl.java`, `R/src/main/resources/application.yml` |
| FE | `web/src/interface/interview.ts`, `web/src/services/interview.service.ts`, `web/src/app/components/interviews/*`, `web/src/app/components/recruitment/interview-rounds.tsx`, candidate/HR interview routes, `web/src/config/menu.ts` |
| Docs/QA | `docs/api/{UT-06-interview-scheduling.md,UT-06.http,ut06-openapi.yaml}`, `docs/api/screenshots/ut06-*`, `web/e2e/ut06-interviews.spec.ts` |

DTO chính (UUID và Instant trên Java; string trên TypeScript):

- `InterviewSlotRequest(OffsetDateTime startAt, OffsetDateTime endAt)`; `PickInterviewRequest(UUID slotId)` tại `J/dto/request/PickInterviewRequest.java`.
- `InterviewSlotResponse(UUID id, UUID roundId, UUID proposalId, Instant startAt, Instant endAt, InterviewSlotStatus status)`.
- `InterviewScheduleResponse(UUID id, UUID roundId, UUID slotId, Instant startAt, Instant endAt, String meetUrl, InterviewScheduleStatus status)`.
- `InterviewProposalResponse(UUID proposalId, UUID roundId, List<InterviewSlotResponse> slots)`.
- `InterviewInvitationResponse(UUID roundId, String roundName, String jobTitle, String companyName, String interviewerEmail, List<InterviewSlotResponse> slots, InterviewScheduleResponse schedule)`.
- `InterviewCalendarItemResponse(InterviewScheduleResponse schedule, UUID candidateId, String candidateName, String jobTitle, String roundName, String interviewerEmail)`.
- PageResponse hiện có; page bắt đầu 1, size 1–100, mặc định 20; sort ổn định theo startAt/id cho lịch và createdAt/id cho lời mời.

## Task 1: Migration, domain và invariants

**Files:** tạo migration/domain/repository trong File Map; sửa `T/ApplicationIntegrationSupport.java`; tạo `T/InterviewSchedulingPersistenceTest.java`.

**Interfaces:** repository tìm slot theo round, lịch active theo round, và overlap theo candidate/email. `InterviewResourceLock.lock(UUID candidateId, String interviewerEmail): void` yêu cầu transaction đang mở. Email được strip/lowercase Locale.ROOT trước lưu/khoá.

- [ ] Viết test persistence: `rejectsShortSlotAndMismatchedRound` assert 29m59s bị constraint reject, 30m hợp lệ; slot thuộc round khác không tạo schedule; `allowsOneActiveSchedulePerRound` assert lịch SCHEDULED thứ hai bị reject nhưng CANCELLED lịch cũ cho phép lịch mới.
- [ ] Chạy `mvn -f server/pom.xml -pl services/recruitment-service -am test -Dtest=InterviewSchedulingPersistenceTest -Dsurefire.failIfNoSpecifiedTests=false`; mong đợi fail do schema/domain chưa có.
- [ ] Tạo V6 (kiểm tra version mới nhất khi thực thi), 3 bảng với cột trong spec. Thêm UUID proposal_id, timestamps; schedule có snapshot ứng viên/người phỏng vấn, suppression flag; outbox có event_key, type, recipient, JSON payload snapshot, lease/retry/sent metadata. Unique `(event_key,type,recipient)`, partial unique SCHEDULED/round và FK ghép slot/round. Enum mapping dạng STRING. Cấm end < start + 30m. Thêm indexes theo truy vấn.
- [ ] Implement repositories và lock bằng `pg_advisory_xact_lock` từ danh sách khoá `candidate:<uuid>` và `interviewer:<email>` đã sort, dùng cùng phép hash ổn định trên PostgreSQL. Collision chỉ làm serialize thừa, không làm mất bảo vệ.
- [ ] Sửa cleanup test: xoá interview outbox, schedules, slots trước rounds; tắt job/worker interview ở integration support.
- [ ] Chạy lại test trên PostgreSQL thật, kỳ vọng PASS; commit `feat(UT-06): add interview scheduling persistence` với danh sách file cụ thể.

## Task 2: Meet có idempotency và HTTP adapter

**Files:** `mock/meet-rooms.js`, `mock/server.js`, `mock/test/meet-rooms.test.js`, `J/client/{MeetRoomClient,HttpMeetRoomClient}.java`, `T/MeetRoomClientTest.java`, application.yml.

**Interfaces:** `MeetRoomClient.create(UUID slotId, String title, Instant startAt, Instant endAt): String` trả URL phòng; gửi `Idempotency-Key: <slotId>`, service key theo mock hiện tại; lỗi dependency thành RecruitmentDependencyException 503 với code ổn định.

- [ ] Viết node:test với server con theo pattern `mock/test/jd-cv.test.js`: cùng key/body hai lần có cùng roomId/roomUrl và một phòng; đổi body cùng key trả 409; `/mock/reset` xoá cả phòng và khoá; không key giữ hợp đồng mock cũ.
- [ ] Chạy `npm --prefix mock test`, mong đợi test idempotency fail trước khi implement.
- [ ] Tách riêng quản lý phòng/idempotency vào meet-rooms.js, nối route/reset hiện có; không thay đổi 10 endpoint khác. Giới hạn key hợp lệ và canonicalize title/start/end để đối chiếu payload.
- [ ] Viết adapter test: map roomUrl thành String, đúng service key/idempotency key; timeout, URL thiếu/sai scheme và response lỗi đều thành 503. Chạy test thấy fail rồi implement HTTP adapter theo RestClient pattern của repo, timeout mặc định connect 2s/read 5s, cấu hình base URL/service key/timeout qua env không chứa secret thật.
- [ ] Chạy `npm --prefix mock test` và Maven với `-Dtest=MeetRoomClientTest -Dsurefire.failIfNoSpecifiedTests=false`; kỳ vọng PASS; commit `feat(UT-06): make Meet room creation retry safe`.

## Task 3: API đề xuất/chọn giờ và chống trùng

**Files:** DTOs trong File Map, `J/service/InterviewSchedulingService.java`, `J/controller/{HrInterviewController,CandidateInterviewController}.java`, `T/{InterviewSchedulingApiTest,InterviewSchedulingConcurrencyTest}.java`.

**Interfaces:** `propose(AuthPrincipal actor, UUID roundId, List<InterviewSlotRequest> slots): InterviewProposalResponse`; `pick(AuthPrincipal actor, UUID roundId, UUID slotId): InterviewScheduleResponse`. Gọi MeetRoomClient Task 2, dùng repositories/locks Task 1. Ghi outbox trực tiếp qua repository Task 1 với event_key `proposal:<id>` cho INVITATION hoặc `schedule:<id>` cho CONFIRMATION, payload snapshot đầy đủ cho Task 5.

- [ ] Viết MockMvc assertions: 0/6 khung, quá khứ, 29m59s, offset thiếu, null item, chồng khung =>400; 1/5 khung, 30m và các khung nối tiếp =>201; HR sai công ty/candidate khác =>404, role sai =>403. Chỉ tạo lần đầu, đề xuất lại qua slots =>409.
- [ ] Viết assertions pick: có meetUrl và đúng start/end, 1 PICKED/những slot khác CANCELLED, round.scheduledAt được đồng bộ; pick huỷ/hết giờ =>409; slot round khác =>404; pick cùng slot hiện tại trả cùng schedule, một lần gọi Meet và một outbox/người. Meet lỗi =>503, chưa đổi slot/lịch/outbox.
- [ ] Chạy Maven với `-Dtest=InterviewSchedulingApiTest -Dsurefire.failIfNoSpecifiedTests=false`, kỳ vọng fail do API chưa có.
- [ ] Implement validation nguyên danh sách và 2 endpoint dưới `/recruitment/rounds/{roundId}/slots`, `/recruitment/me/interviews/{roundId}/pick`. Ownership trước tiết lộ trạng thái. Transaction lock application → round → resource locks theo Task 1; re-read và kiểm tra thời gian/overlap sau khoá, rồi Meet, lưu schedule, slot, scheduledAt và outbox. Request pick lặp nhận kết quả cũ chỉ sau kiểm tra ownership/current status.
- [ ] Viết concurrency test bằng executor/latches và PostgreSQL: hai pick cùng round chỉ một lịch; hai round chung ứng viên hoặc email người phỏng vấn khác casing chỉ một lịch khi overlap; khác offset cùng instant vẫn trùng; hai buổi nối tiếp đều thành công. Không dùng sleep làm đồng bộ.
- [ ] Chạy `-Dtest=InterviewSchedulingApiTest,InterviewSchedulingConcurrencyTest -Dsurefire.failIfNoSpecifiedTests=false`; kỳ vọng PASS; commit `feat(UT-06): schedule interviews with conflict protection`.

## Task 4: Đổi lịch và tích hợp vòng đời UT-02

**Files:** `J/service/{InterviewSchedulingService,InterviewLifecycleService,InterviewRoundService}.java`, `J/service/impl/ApplicationTransitionServiceImpl.java`, HrInterviewController, `T/InterviewLifecycleTest.java`, InterviewSchedulingConcurrencyTest.

**Interfaces:** `reschedule(AuthPrincipal actor, UUID roundId, List<InterviewSlotRequest> slots): InterviewProposalResponse`; `InterviewLifecycleService.cancelForApplication(UUID applicationId, Instant now): void`; `completeForRound(UUID roundId, Instant now): void`. Lifecycle methods dùng transaction/lock application đã có, không mở REQUIRES_NEW và không phụ thuộc SchedulingService để tránh circular bean.

- [ ] Viết test: reschedule body sai giữ lịch/khung/outbox nguyên; body đúng huỷ SCHEDULED và OPEN cũ, xoá scheduledAt, tạo 1–5 khung OPEN mới, invitation mới, CANCEL cho recipient CONFIRMATION SENT/attempts>0; confirmation/reminder chưa gửi thành CANCELLED. Pick slot PICKED của lịch huỷ =>409.
- [ ] Thêm test ứng viên rút đơn/HR chuyển trạng thái khỏi INTERVIEW huỷ lịch cùng transaction; PASS/FAIL => DONE, trả PENDING không mở lại; scheduledAt cũ không tạo schedule/phòng. Test reject scheduledAt khác null trên createRound mới với thông báo dùng UT-06, vẫn đọc giá trị legacy.
- [ ] Chạy `-Dtest=InterviewLifecycleTest -Dsurefire.failIfNoSpecifiedTests=false`, kỳ vọng fail rồi implement POST `/recruitment/rounds/{roundId}/reschedule` và hooks lifecycle trong đường ghi trạng thái/kết quả. Tuân cùng thứ tự application/round locks; huỷ/DONE chỉ làm giảm lịch active nên không lấy resource lock sau khoá khác theo thứ tự ngược.
- [ ] Thêm race pick/reschedule và pick/application transition: không có lịch active tham chiếu đề xuất cũ sau khi reschedule commit hoặc sau khi đơn rời INTERVIEW. Kiểm tra UT-07 offer transition vẫn hợp lệ.
- [ ] Chạy `-Dtest=InterviewLifecycleTest,InterviewSchedulingConcurrencyTest,InterviewRoundServiceTest,HrApplicationApiTest,ApplicationTransitionConcurrencyTest -Dsurefire.failIfNoSpecifiedTests=false`; kỳ vọng PASS; commit `feat(UT-06): cancel and replace interview schedules atomically`.

## Task 5: Mail, ICS, reminder job và worker

**Files:** các file Email trong File Map; `T/{InterviewReminderJobTest,InterviewNotificationWorkerTest,InterviewCalendarWriterTest,SmtpInterviewMailSenderTest}.java`, application.yml.

**Interfaces:** `InterviewReminderJob.enqueueDue(): int`; `InterviewNotificationWorker.dispatchBatch(): int`; `InterviewCalendarWriter.write(InterviewNotificationOutbox notification): byte[]`; `InterviewMailSender.send(InterviewNotificationOutbox notification): void`. Dùng Clock, outbox Task 1/payload Task 3–4. Cấu hình enabled riêng cho worker/job để test không chạy ngầm.

- [ ] Viết Clock test tại start-now = 24h+1s,24h,1h+1s,1h,1s,0: đúng mốc, không gửi hai mốc cùng lượt; đặt <=1h suppressed; CANCELLED/DONE bỏ qua. Chạy job hai lần/nhiều transaction => một row/người/mốc. Khi recipient email trùng chỉ một row. Chạy test thấy fail.
- [ ] Implement cron `0 */15 * * * *`, enqueue outbox với INSERT ON CONFLICT DO NOTHING. Worker scan batch 20, lease 60s, tối đa 5 attempts, retry 60/300/900/3600s như repo. Không retry reminder qua giờ bắt đầu hoặc sau khi mốc đổi; không tự ghi SENT khi chưa gửi. Cập nhật reminded_* khi tất cả recipient của mốc SENT; suppression dùng flag riêng.
- [ ] Viết test worker: một recipient SENT, người còn lại lỗi rồi retry chỉ gửi người lỗi; huỷ giữa claim và send chặn gửi; attempts>0 dù chưa SENT vẫn nhận CANCEL khi huỷ; failed lease phục hồi; FAILED sau 5 lần. Serialize lượt gửi theo schedule bằng PostgreSQL session advisory lock trên dedicated connection, dùng namespace riêng resource locks; giữ qua send và ghi kết quả, giải phóng trong finally. Worker re-read trạng thái trước gửi; huỷ không đợi worker trong DB transaction, email CANCEL phải đợi cùng send lock để đứng sau lượt gửi đã bắt đầu.
- [ ] Implement worker theo claim/lease pattern hiện có, xác định recipient từng hàng, payload immutable. Không dùng cùng dedicated connection đang giữ send lock cho query có thể chờ application lock; chỉ đọc state bằng MVCC rồi send để tránh deadlock. Ghi rõ race sau lần kiểm tra cuối có thể đã bắt đầu gửi trước huỷ và sẽ được theo bằng CANCEL.
- [ ] Viết ICS/mail assertions: REQUEST/CANCEL cùng UID, CANCEL SEQUENCE lớn hơn, schedule mới UID khác; DTSTART/DTEND UTC, ORGANIZER/ATTENDEE, URL, CRLF/escape/folding Unicode, MIME attachment `.ics` đúng method/content-type. Subject/body tiếng Việt chứa giờ Asia/Ho_Chi_Minh và URL khi xác nhận. Message-ID cố định theo outboxId.
- [ ] Implement CalendarWriter và SMTP sender; INVITATION không đính ICS vì chưa có giờ, REMINDER không tạo sự kiện khác. Thêm config từ env và log id/attempt không log secret. Chạy Maven với `-Dtest=InterviewReminderJobTest,InterviewNotificationWorkerTest,InterviewCalendarWriterTest,SmtpInterviewMailSenderTest -Dsurefire.failIfNoSpecifiedTests=false`; kỳ vọng PASS; commit `feat(UT-06): deliver calendar invitations and interview reminders`.

## Task 6: API đọc lời mời và lịch HR

**Files:** `J/service/InterviewQueryService.java`, 2 controllers Task 3, response DTOs, `T/InterviewQueryApiTest.java`.

**Interfaces:** `listMine(AuthPrincipal actor, int page, int size): PageResponse<InterviewInvitationResponse>`; `listCompany(AuthPrincipal actor, Instant from, Instant to, String interviewerEmail, InterviewScheduleStatus status, int page, int size): PageResponse<InterviewCalendarItemResponse>`.

- [ ] Viết MockMvc assertions: candidate chỉ thấy đơn mình, list gồm lời mời và lịch hiện tại; HR thấy toàn công ty nhưng không thấy công ty khác; filter email ignore-case; page 1,size100 hợp lệ, page0/size101 =>400; missing/invalid offset/from>=to =>400.
- [ ] Viết query assertions: buổi đi qua from/to có giao khoảng được trả; end==from/start==to bị loại; sort ổn định khi cùng giờ; lịch mặc định SCHEDULED/DONE, status filter cho CANCELLED. Khung OPEN đã hết giờ không có trong lựa chọn. Chạy test thấy fail.
- [ ] Implement GET `/recruitment/me/interviews` và `/recruitment/companies/me/interviews`; DTO tránh lộ email ứng viên ngoài công ty, tránh N+1 cho danh sách. API HR thêm status optional theo spec, mặc định hai trạng thái SCHEDULED/DONE. Query theo authenticated companyId, không tin query companyId.
- [ ] Chạy Maven với `-Dtest=InterviewQueryApiTest -Dsurefire.failIfNoSpecifiedTests=false`; kỳ vọng PASS; commit `feat(UT-06): expose candidate invitations and company calendar`.

## Task 7: FE đề xuất giờ, chọn giờ và lịch tuần

**Files:** interface/interview.ts, services/interview.service.ts; tạo `web/src/app/components/interviews/{slot-proposal-form,candidate-interviews,hr-interview-calendar}.tsx`; sửa recruitment/interview-rounds.tsx; tạo `web/src/app/(pages)/(app)/{candidate,hr}/interviews/page.tsx`; sửa config/menu.ts; tạo `web/e2e/ut06-interviews.spec.ts`.

**Interfaces:** TypeScript DTO mirror Task 1–6. `interviewService.propose(roundId, slots)`, `.reschedule(roundId, slots)`, `.pick(roundId, slotId)`, `.listMine({page,size})`, `.listCompany({from,to,interviewerEmail,status,page,size})`; trả DTO/PageResponse đã định nghĩa. `SlotProposalForm({roundId,onSaved})`, `CandidateInterviews()`, `HrInterviewCalendar()`.

- [ ] Đọc AGENTS/Next docs, viết Playwright theo pattern e2e hiện có: HR thêm/xoá tối đa5 dòng, chặn <30m/quá khứ/chồng giờ; gửi payload có offset, confirm đổi lịch nói rõ huỷ ngay; candidate pick rồi hiện nút Meet; 409 hiển thị thông báo và reload, pending chặn double click. Route fixtures kiểm chứng UI/API contract, không coi fixture là bằng chứng tích hợp thật.
- [ ] Chạy `npm --prefix web run test:e2e -- ut06-interviews.spec.ts`, kỳ vọng fail vì UI chưa có.
- [ ] Implement types/client và form tích hợp vòng UT-02; bỏ ô nhập scheduledAt trực tiếp. Đọc trạng thái proposal qua lời mời không phù hợp quyền HR thì mở rộng InterviewRoundResponse hiện có với `slots`/`schedule` từ query service, cập nhật mapper/detail UT-02 và TypeScript để HR luôn có dữ liệu round hiện tại mà không gọi endpoint candidate. Bổ sung test API detail HR cho mapping này.
- [ ] Implement candidate route/menu và thẻ, nút Meet chỉ khi SCHEDULED, target blank/rel noopener noreferrer. Hiển thị rõ timezone, không dùng timezone máy để diễn giải datetime-local: chuyển từ Asia/Ho_Chi_Minh bằng offset +07:00 trước gửi.
- [ ] Implement HR tuần Monday 00:00 +07 đến Monday kế tiếp, đổi tuần/hôm nay, filter email/status. Dùng TanStack Query key gồm tuần/filter, fetch tất cả page bằng size100, không append response cũ vào tuần mới; giao diện bảy ngày và card theo giờ, loading/empty/error/retry. Buổi giao ranh giới tuần vẫn hiện phần giao với tuần.
- [ ] Thêm Playwright assertions response tuần trước trả trễ không ghi đè tuần hiện tại, >100 buổi không mất page2, lịch ngoài công ty không được client tự truy vấn, thời gian hiển thị +07 đúng cả trình duyệt timezone khác. Mở UI kiểm tra desktop/mobile theo công cụ browser có sẵn.
- [ ] Chạy `npm --prefix web run type-check`, `npm --prefix web run lint`, `npm --prefix web run test:e2e -- ut06-interviews.spec.ts`; kỳ vọng PASS; commit `feat(UT-06): add interview booking and weekly calendar UI`.

## Task 8: Kiểm chứng tích hợp, tài liệu và PR

**Files:** `docs/api/{UT-06-interview-scheduling.md,UT-06.http,ut06-openapi.yaml}`, `docs/api/screenshots/ut06-*`, `README.md`, `mock/README.md`, `docs/mock-contract.md`.

**Interfaces:** toàn bộ API qua gateway `/api/v1/recruitment`, mock Meet và SMTP/Mailpit theo cấu hình hiện tại. Không thay provider hoặc giả lập thành công email chưa gửi.

- [ ] Chạy toàn bộ `mvn -f server/pom.xml -pl services/recruitment-service -am test` trên Testcontainers PostgreSQL hoặc DB test riêng bằng UT01_TEST_DB_URL/USER/PASSWORD; không trỏ cleanup test vào DB dùng chung. Chạy `npm --prefix mock test` và FE type-check/lint/e2e cho UT-06 cùng UT-02/UT-07 liên quan. Lưu kết quả thật và phân biệt test bị skip/môi trường không chạy được.
- [ ] Dùng PostgreSQL + mock Meet + SMTP capture, gọi HTTP thật để kiểm chứng: pick có URL/phòng duy nhất và 2 mail/ICS; pick cancelled409; reschedule huỷ lịch cũ; lịch còn1h enqueue/dispatch rồi chạy lại không thêm mail; calendar tuần đúng các buổi. MockMvc/Playwright fixture không thay cho bước này. Khi cần API testing tool áp dụng skill tương ứng lúc thực thi, không tự thêm workspace Postman.
- [ ] Kiểm chứng tạo phòng xong rollback DB rồi retry cùng slot dùng cùng room; concurrency cùng người phỏng vấn qua hai đơn khác nhau; email gửi một bên lỗi không gửi lại bên đã SENT. Ghi riêng giới hạn crash SMTP và URL Meet không được revoke.
- [ ] Cập nhật OpenAPI/body array/offset/errors/status filters/response pagination, .http có case400/409, cách chạy job một lượt trong test, env/timeout, demo accounts có sẵn và dữ liệu tương lai reproducible. Chụp ảnh HR form/tuần và candidate chọn/vào phòng. Cập nhật mock idempotency/reset contract.
- [ ] Rà soát diff: không secret, không đổi nhánh đích, migration không sửa file đã chạy; `git diff --check` và kiểm tra tất cả file mới. Commit `docs(UT-06): document interview scheduling and verification` cùng spec/plan đã duyệt.
- [ ] Áp dụng verification-before-completion và requesting-code-review, xử lý findings và chỉ chạy lại checks bị ảnh hưởng bởi sửa đổi. Sau review dùng finishing-a-development-branch, push feature branch và mở draft PR vào test (không merge); tiêu đề `[UT-06] Đặt lịch và nhắc phỏng vấn`, mô tả kết quả, migration/env, checks và giới hạn SMTP/Meet, trả link PR. Nếu quyền mạng/git bị chặn, yêu cầu escalation cho đúng lệnh cần thiết thay vì báo đã hoàn tất.

## Self-review của kế hoạch

- Migration/ownership/đồng thời: Task1,3,4; mock Meet và rollback: Task2,3,8; email/ICS/reminder: Task5; HR/calendar/read: Task6; FE/timezone/pagination: Task7; docs/PR: Task8.
- Năm Review Focus đã gắn assertions vào Task3,4,5,7. Hợp đồng DTO/service thống nhất giữa các task; helper payload/outbox được sở hữu bởi Task1, dùng ở Task3–5.
- Không viết mã sản phẩm trong giai đoạn kế hoạch. Chờ người dùng xem kế hoạch và chọn cách thực thi theo skill writing-plans.
