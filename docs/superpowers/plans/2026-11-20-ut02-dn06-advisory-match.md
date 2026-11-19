# UT-02 DN-06 Advisory Match Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** HR/STAFF xem điểm DN-06 chỉ để tham khảo, nhận biết lỗi lấy điểm và thử lại riêng khối điểm mà không mất nội dung tuyển dụng.

**Architecture:** Một lookup service trong recruitment chuyển kết quả DN-06 thành AVAILABLE/UNAVAILABLE; detail và endpoint match-only dùng chung service sau kiểm quyền đơn. FE dùng MatchScoreCard có sẵn, retry cập nhật riêng hai trường match và matchStatus, loại response cũ theo đơn/phiên. Không migration và không sửa policy tuyển dụng.

**Tech Stack:** Java21/Spring Boot/JPA/JUnit/MockMvc, RestClient, Next.js/React/TypeScript/Playwright hiện có.

**Spec:** `docs/superpowers/specs/2026-11-19-ut02-dn06-advisory-match-design.md` (đã duyệt; bổ sung quyết định timeout khi đối chiếu mã nguồn).

## Global Constraints

- Điểm0 hoặc belowMinimum=true không chặn ứng tuyển, chuyển bước, tạo/đặt lịch hoặc gửi offer. Điều kiện UT-01, UT-02, UT-06, UT-07 giữ nguyên.
- Không migration, không lưu snapshot điểm, không đổi công thức DN-06, không mở rộng quyền DN-06.
- Giữ match và cấu trúc MatchResponse; chỉ thêm matchStatus vào chi tiết và endpoint đọc mới.
- Ownership/role/account guards chạy trước lookup điểm, không bị catch hoặc hạ thành UNAVAILABLE.
- ADMIN không gọi DN-06, detail trả NOT_REQUESTED/null; retry chỉ HR/STAFF, ADMIN403.
- Nút thử lại cập nhật riêng match/matchStatus; không làm mất input chưa lưu. Không gán0 khi lỗi.
- Match client có connect2000ms/read5000ms riêng để nhỏ hơn timeout Axios15000ms; không đổi RecruitmentHttpSupport dùng chung. Không retry tự động.
- Không sửa shared MatchScoreCard theo cách phá DN-06 preview. Câu hiển thị: “Điểm phù hợp chỉ mang tính tham khảo”.
- Nhánh/PR mới `[UT-02]`, base test. Giữ phương thức thực thi tuần tự trong phiên đã được người dùng chọn trước đó; một review độc lập cuối nhánh. Tạo/kiểm tra worktree từ test mới nhất bằng using-git-worktrees khi bắt đầu thực thi.
- Không chạm thay đổi người dùng `mock/data.json`, `mock/test/jd-cv.test.js`; không đưa chúng vào nhánh mới. Mang spec/plan đã duyệt vào worktree mới.
- Trước sửa FE đọc web/AGENTS.md và Next docs cài trong web/node_modules/next/dist/docs/. Không thêm dependency.

## Review Focus

1. Timeout match30s vượt Axios15s: dependency chậm phải trở thành UNAVAILABLE trong request detail thay vì frontend mất cả trang (Task1/2).
2. Upstream401/403 khác lỗi quyền API UT-02: chỉ lỗi trong lookup được cô lập; HR công ty khác404 trước khi gọi điểm (Task2).
3. Hai response retry cùng đơn nhưng khác phiên đăng nhập: response phiên trước không được ghi đè UI/cache phiên mới (Task3).
4. Điểm0 và JD không có kỹ năng: đều là AVAILABLE, không nhầm thành lỗi hoặc không có kết quả (Task1/3).
5. Retry trong khi HR đang nhập ghi chú, form vòng hoặc offer: không remount các form và không ghi đè input chưa lưu (Task3).

## File map và interfaces chung

`J = server/services/recruitment-service/src/main/java/com/rikkei/jobs/recruitment`, `T = server/services/recruitment-service/src/test/java/com/rikkei/jobs/recruitment`.

| Phần | File |
| --- | --- |
| Availability/domain | `J/dto/response/MatchStatus.java`, `J/dto/response/MatchLookupResponse.java`, `J/service/MatchLookupService.java` |
| HTTP boundary | `J/client/HttpJobMatchClient.java`, `server/services/recruitment-service/src/main/resources/application.yml`, `.env.example`, `compose.yaml` |
| Quyền và API | `J/service/ApplicationReadAccessService.java`, `J/service/HrApplicationReadService.java`, `J/controller/ApplicationMatchController.java`, `J/dto/response/HrApplicationDetailResponse.java` |
| FE | `web/src/interface/hr-application.ts`, `web/src/services/hr-application.service.ts`, `web/src/app/components/recruitment/{match-card,application-workflow-detail}.tsx` |
| Tests | `T/{MatchLookupServiceTest,MatchLookupApiTest,MatchLookupHttpBoundaryTest}.java`, existing read/dependency tests, `web/e2e/ut02-match-advisory.spec.ts` |
| Tài liệu | `docs/api/{ut02-openapi.yaml,UT-02-application-pipeline.md,UT-02-match-advisory.http,UT-02-match-advisory-verification.md}`, ảnh `docs/api/screenshots/ut02-match-*` |

- `enum MatchStatus { AVAILABLE, UNAVAILABLE, NOT_REQUESTED }`.
- `record MatchLookupResponse(MatchStatus matchStatus, MatchResponse match)`; AVAILABLE có DTO kể cả0; hai trạng thái còn lại null.
- `MatchLookupService.lookup(UUID jobId, UUID candidateId, String bearer): MatchLookupResponse` chỉ dùng sau khi kiểm quyền tài nguyên.
- `ApplicationReadAccessService.findReadable(AuthPrincipal actor, UUID applicationId): Application` dùng đúng quyền reader UT-02 HR/STAFF/ADMIN và ownership HR; không gọi matching.
- Detail DTO giữ mọi trường hiện tại và thêm `MatchStatus matchStatus`; JSON tương thích kiểu mở rộng. Frontend `matchStatus?: MatchStatus` để đọc được response/fixture cũ.
- `hrApplicationService.match(applicationId: string, signal?: AbortSignal): Promise<MatchLookupResponse>` gọi `/recruitment/applications/{id}/match`.

## Task 1: Availability lookup và timeout riêng DN-06

**Files:** tạo MatchStatus, MatchLookupResponse, MatchLookupService, `T/MatchLookupServiceTest.java`, `T/MatchLookupHttpBoundaryTest.java`; sửa HttpJobMatchClient và cấu hình trong File map; giữ constructor `HttpJobMatchClient(RestClient)` cho test hiện có.

**Interfaces:** consumes `JobMatchClient.get(UUID,UUID,String): Optional<MatchResponse>`; produces lookup signature và response chung phía trên. Spring constructor adapter nhận thêm `app.recruitment.match-connect-timeout-ms` mặc định2000 và `app.recruitment.match-read-timeout-ms` mặc định5000, dùng builder.clone() và request factory riêng.

- [ ] Viết `zeroAndEmptySkillListsAreAvailable`: score0, belowMinimum=false, met/missing rỗng => AVAILABLE cùng DTO, không null. Viết `missingOrDependencyFailureIsUnavailable`: Optional.empty hoặc RecruitmentDependencyException502 MATCH_CONTRACT_ERROR => UNAVAILABLE/null. Không catch lỗi bất kỳ ngoài ranh giới dependency.
- [ ] Chạy `mvn -f server/pom.xml -pl services/recruitment-service -am test -Dtest=MatchLookupServiceTest -Dsurefire.failIfNoSpecifiedTests=false`; Expected: FAIL do lookup/types chưa có.
- [ ] Implement enum/record/service. Chỉ catch RecruitmentDependencyException từ JobMatchClient và Optional.empty; log code/jobId/candidateId khi contract/auth upstream lỗi, không log token/body. Giữ HttpJobMatchClient contract cho caller khác.
- [ ] Viết boundary tests dùng HTTP server cục bộ: URL/bearer chính xác; score78 và candidateScore82.058824 giữ giá trị;404/503 => empty;401/403/payload sai => adapter exception và lookup UNAVAILABLE. Server giữ response bằng latch; read timeout cấu hình100ms trong test, lookup trả UNAVAILABLE trước khi mở latch. Không dùng sleep đồng bộ. Chạy RED trước thay timeout factory.
- [ ] Implement match-only request factory, connect/read timeout qua env `MATCH_CONNECT_TIMEOUT_MS`/`MATCH_READ_TIMEOUT_MS` và Compose forwarding. Không thay timeout chung của identity/profile/job clients.
- [ ] Chạy `-Dtest=MatchLookupServiceTest,MatchLookupHttpBoundaryTest,ApplicationDependencyClientsTest,RecruitmentConfigurationSmokeTest -Dsurefire.failIfNoSpecifiedTests=false`; Expected: PASS. Commit `feat(UT-02): isolate advisory match availability` với file Task1.

## Task 2: Detail metadata, retry endpoint và bảo toàn quyền

**Files:** tạo ApplicationReadAccessService, ApplicationMatchController và MatchLookupApiTest; sửa HrApplicationReadService, HrApplicationDetailResponse, `T/HrApplicationReadApiTest.java` và caller constructors liên quan.

**Interfaces:** consumes lookup Task1 và access helper; produces GET `/recruitment/applications/{id}/match` trả200 MatchLookupResponse cho HR/STAFF. Controller method/service dùng cùng helper như detail; route tĩnh suffix match không đổi các route khác.

- [ ] Viết API tests: detail match lỗi502 vẫn200 có candidate/CV/round/history/allowedTransitions và UNAVAILABLE/null; score0 => AVAILABLE/0; ADMIN detail => NOT_REQUESTED/null, không gọi matching. Retry endpoint HR/STAFF =>200; ADMIN/CANDIDATE/MENTOR403; không JWT401; HR khác công ty404 và không gọi dependency. Đối chiếu lỗi account-disabled/must-change-password theo guards hiện có, không thay guard.
- [ ] Chạy `-Dtest=MatchLookupApiTest,HrApplicationReadApiTest -Dsurefire.failIfNoSpecifiedTests=false`; Expected: FAIL do thiếu endpoint/metadata hoặc lỗi dependency vẫn làm hỏng detail.
- [ ] Extract quyền reader/ownership thành ApplicationReadAccessService, dùng trong detail và match-only. Detail tránh gọi lookup cho ADMIN, còn HR/STAFF gọi service Task1; append matchStatus vào record/caller, giữ match và tất cả trường cũ.
- [ ] Implement controller chỉ HR/STAFF, dùng authorized application lấy jobId/userId từ DB, chuyển tiếp nguyên bearer. Không nhận jobId/userId do FE gửi; endpoint không đọc identity/contact/match-score table trực tiếp.
- [ ] Bổ sung assertions: identity lỗi503 vẫn làm detail503, không chuyển UNAVAILABLE; retry không gọi identity/contact; 404/503/timeout/payload/upstream401/403 của matching cho detail và retry200 UNAVAILABLE; không đổi trạng thái đơn trong mọi lookup.
- [ ] Chạy `-Dtest=MatchLookupApiTest,HrApplicationReadApiTest,ApplicationRoleAlignmentTest,HrAccountLockIntegrationTest,HrApplicationApiTest -Dsurefire.failIfNoSpecifiedTests=false`; Expected: PASS. Commit `feat(UT-02): expose advisory match status and retry endpoint`.

## Task 3: Component chung, retry độc lập và chống response cũ

**Files:** FE File map, tạo `web/e2e/ut02-match-advisory.spec.ts`; dùng nguyên `web/src/app/components/match/match-score-card.tsx` nếu props hiện tại đã đủ. Cập nhật fixture UT-02 liên quan để phản ánh metadata mới, không thay fixture của task khác nếu fallback đã hỗ trợ.

**Interfaces:** mirror MatchStatus/MatchLookupResponse trong hr-application.ts; thêm service.match signature chung. `MatchCard` wrapper nhận applicationId, match, matchStatus?, `onMatchChanged(value: MatchLookupResponse): void`; viewerId/token lấy từ auth store. Parent chỉ hiển thị cho HR/STAFF, key theo viewerId/applicationId; onMatchChanged chỉ merge hai trường khi current.id vẫn là request applicationId.

- [ ] Viết Playwright RED: initial AVAILABLE score0 hiển thị0% và dòng tham khảo; met/missing rỗng có thông báo JD không kỹ năng; UNAVAILABLE vẫn có CV/vòng/Chuyển bước. Có nút Thử lại; ADMIN không có khối điểm và không gọi endpoint.
- [ ] Chạy `npm --prefix web run test:e2e -- ut02-match-advisory.spec.ts`; Expected: FAIL vì UI chưa có trạng thái/retry/component chung.
- [ ] Implement types và axios match service có AbortSignal. Đổi MatchCard thành wrapper dùng MatchScoreCard; fallback metadata cũ: match tồn tại =>AVAILABLE, null =>UNAVAILABLE; NOT_REQUESTED không hiện card. Thêm thông báo UNAVAILABLE đúng spec và nút Thử lại, loading trong card.
- [ ] Implement retry với in-flight ref, AbortController và cleanup khi unmount; kiểm tra viewerId/token hiện tại còn khớp trước khi áp dụng kết quả. Không giữ token trong DOM, URL hoặc log. Khi kết quả UNAVAILABLE hoặc retry lỗi, bỏ điểm cũ khỏi phần hiển thị hiện hành; giữ xử lý lỗi401 chuẩn của axios, không retry tự động.
- [ ] Viết assertions độc lập: giữ response retry A, chuyển sang đơn B rồi trả A =>B không đổi; đổi phiên/token khi request đang chờ =>không dùng response cũ. Pending hai click chỉ một request. Giữ ghi chú chuyển bước, tên vòng mới và input offer hợp lệ chưa lưu trước/sau retry. Detail không phát thêm request match ở initial load.
- [ ] Viết test advisory: AVAILABLE0/belowMinimum và UNAVAILABLE không ẩn/chặn nút hợp lệ theo trạng thái đơn. Retry UNAVAILABLE→AVAILABLE dùng đúng response mới, không tự tính điểm; lỗi403 của endpoint hiển thị lỗi quyền, không giả thành điểm0. Kiểm tra mobile/desktop qua ảnh.
- [ ] Chạy `npm --prefix web run type-check`, `npm --prefix web run lint`, `npm --prefix web run test:e2e -- ut02-match-advisory.spec.ts ut02-hr-workflow.spec.ts ut02-staff-applications.spec.ts ut02-role-alignment.spec.ts ut06-interviews.spec.ts ut07-offer-flow.spec.ts`; Expected: PASS (ghi nhận warning có sẵn nếu có). Commit `feat(UT-02): add shared advisory score card and safe retry`.

## Task 4: Kiểm chứng tích hợp, tài liệu và PR

**Files:** tài liệu File map; `.http` mới có detail và match-only cho AVAILABLE/UNAVAILABLE theo môi trường, không thêm endpoint giả đổi điểm. README chỉ thêm link tài liệu nếu cần, giữ các entry task hiện có.

**Interfaces:** API qua gateway `/api/v1/recruitment`, DN-06 owner company-service và profile verified-skills hiện hành. Không sửa mock/data.json/test của người dùng để tạo demo.

- [ ] Cập nhật ut02-openapi.yaml: enum MatchStatus, schema MatchLookupResponse, trường matchStatus trong detail, GET /applications/{id}/match có auth/error/status chuẩn và khả năng200 UNAVAILABLE. Giữ schema Match cho DN-06 và client cũ. Parse YAML, resolve refs, kiểm request .http và link tài liệu.
- [ ] Ghi hướng dẫn UI: dùng đơn có applicant thực, HR/STAFF xem, dừng riêng DN-06 để kiểm UI lỗi và retry sau khôi phục, kiểm ADMIN không có thẻ; không hướng dẫn sửa DB skill trực tiếp hoặc thêm ngưỡng chặn. Nêu rõ điểm0, JD không kỹ năng, UNAVAILABLE khác nhau.
- [ ] Chạy toàn bộ `mvn -f server/pom.xml -pl services/recruitment-service -am test` với PostgreSQL test riêng hoặc Testcontainers; kỳ vọng0 failure/error. Node/mock dependencies cần có cho InterviewLiveFlowTest hiện tại. Chạy FE type-check/lint/build và browser suite Task3; chỉ chạy lại phạm vi bị ảnh hưởng khi có thay đổi mới.
- [ ] Khi môi trường đủ, chạy smoke với identity/company/profile/recruitment thật: ứng viên đã nộp JD6, HR xem điểm DN-06 và UT-02 khớp nhau; thay đổi input kỹ năng bằng API owner đúng quyền rồi retry thấy điểm mới. Không đụng dữ liệu dùng chung nếu chưa có seed test riêng; nếu chưa chạy được ghi rõ chỉ có HTTP-boundary mock, không tuyên bố xuyên service đã pass.
- [ ] Chứng minh policy tuyển dụng không gọi matching: regression UT-01/02/06/07 hiện có và test score0/belowMinimum/UNAVAILABLE vẫn thao tác đúng policy vòng/trạng thái. Không sửa policy để làm test pass.
- [ ] Chụp ảnh các trạng thái AVAILABLE/UNAVAILABLE/retry, tổng hợp command/count/kết quả thật trong UT-02-match-advisory-verification.md. `git diff --check`, xem danh sách file, bảo đảm không có thay đổi người dùng/secret/migration ngoài phạm vi. Commit `docs(UT-02): document DN-06 advisory match integration` cùng spec/plan.
- [ ] Dùng requesting-code-review cho một review độc lập toàn nhánh, xử lý finding theo receiving-code-review/TDD và kiểm chứng lại checks bị ảnh hưởng. Dùng verification-before-completion và finishing-a-development-branch; push feature branch, mở draft PR mới `[UT-02] Tích hợp điểm DN-06 tham khảo và thử lại` vào test, không merge. Xác nhận base/head/URL, trả link và giới hạn kiểm chứng.

## Self-review

- Spec coverage: availability/failure isolation và timeout Task1/2; ownership/API additive Task2; shared UI/legacy fallback/inputs/stale replies Task3; policy regression/smoke/docs/PR Task4. Không có yêu cầu migration.
- Interfaces: matchStatus là tên nhất quán trên enum/record/detail/TS/OpenAPI; service.match trả cùng shape MatchLookupResponse; wrapper cập nhật hai trường, không thay object round/form.
- Review Focus: cả5 tình huống đều có assertions ở task sở hữu; tách lỗi identity với matching và lỗi quyền ứng dụng với upstream.
- Ruling khi lập kế hoạch: timeout riêng DN-065s thay read30s chung là cần thiết để đáp ứng lỗi không làm mất trang; cost là dependency chậm bị coi tạm không khả dụng. Chưa có mã sản phẩm thay đổi.
- Sau khi người dùng xem kế hoạch, thực thi tuần tự trong phiên theo phương thức đã chọn; không yêu cầu chọn lại cách thực thi.
