# Candidate application journey implementation plan

> **For agentic workers:** Use superpowers:executing-plans to implement this plan task by task. Steps use checkbox syntax for tracking.

**Goal:** Đồng nhất UI đơn ứng tuyển, phỏng vấn và offer theo hướng A đã duyệt.

**Architecture:** Giữ routes và service contracts. Dùng theme chung trong một stylesheet cho vùng tính năng; tái sử dụng component UI và React Query hiện có. Mutation có busy guard, conflict refresh và đối soát lỗi mạng.

**Tech Stack:** Next.js, React, TypeScript, Tailwind, Base UI, React Query, Playwright.

**Spec:** docs/superpowers/specs/2026-11-25-candidate-application-journey-design.md (đã duyệt 25/11/2026).

## Global constraints

- Không thêm API, dependency, thay đổi quyền hay trạng thái nghiệp vụ.
- Khung 1200px, desktop từ 1200px, aside 300px, mobile một cột; giờ Asia/Ho_Chi_Minh.
- Dùng tokens DESIGN.md, một main landmark do CandidateShell cung cấp.
- Giữ thông báo, dialog nộp ứng tuyển, CV snapshot và điều kiện rút hồ sơ.
- Thực hiện tại branch riêng trong workspace hiện có; giữ thay đổi local để review, không push/merge.

## Review focus

- Deadline thay đổi trong dialog hoặc tab nền: khoá gửi, server có quyền quyết định cuối.
- Lỗi mạng sau mutation: kiểm tra detail trước khi gửi lại, giữ lý do từ chối.
- URL không hợp lệ, Back và đổi filter: dữ liệu/returnTo an toàn và đúng ngữ cảnh.
- Lịch đã qua giờ không phải DONE, lịch huỷ không mở link cũ.
- Nội dung dài, mobile 360px và focus sau dialog khi trigger biến mất.

## Task 1: Đơn ứng tuyển và nền tảng trình bày

Files: applications/{applications-list,application-card,application-detail-view,application-progress}.tsx; new applications/candidate-journey.css and journey-navigation.ts; e2e/candidate-journey.spec.ts.

Interfaces: `candidateListReturnUrl(value: string | null, path: string): string` chỉ giữ query danh sách hợp lệ; `CandidateJourney` dùng CSS scope class; ApplicationCard nhận returnTo optional.

- [x] Viết test lọc/Back, safe returnTo, trạng thái kết thúc và mobile; chạy test để xác nhận thiếu hành vi mới.
- [x] Chuẩn hoá bố cục, loading/error không lộ kết quả cũ, key theo query/phiên/id, aside tài liệu, tiến trình responsive, error tải CV riêng.
- [x] Chạy tests mới cùng ut01 applications; xác minh không đổi điều kiện rút và nộp hồ sơ.

## Task 2: Phỏng vấn

Files: interviews/candidate-interviews.tsx; new interviews/interview-invitation.tsx; e2e/candidate-journey.spec.ts.

Interfaces: `InterviewInvitationCard({item, refresh, now})` dùng InterviewInvitation; phân nhóm từ lịch và thời gian, view URL action/upcoming/history.

- [x] Viết tests phân nhóm, chọn giờ, 409, lịch huỷ, link thiếu/không hợp lệ, giờ đã qua; chạy đỏ.
- [x] Triển khai tabs, radio cards, thời lượng, safe link; query theo viewer/token, lấy đủ trang, cập nhật clock và refetch sau chọn giờ.
- [x] Chạy tests cùng ut06; kiểm tra focus và xác nhận không tự đổi tab sau mutation.

## Task 3: Offer

Files: offers/candidate-offer-{list,detail}.tsx; new offers/offer-response-dialog.tsx; e2e/candidate-journey.spec.ts; update ut07-offer-flow.spec.ts theo dialog mới.

Interfaces: `OfferResponseDialog({offer,action,onClose,onUpdated})` dùng Offer và action accept/decline; detail có identity key theo id/phiên; list page ở URL.

- [x] Viết tests dialog accept/decline, draft giữ khi lỗi, expire khi dialog mở, conflict, network reconciliation, page/Back; chạy đỏ.
- [x] Triển khai card, detail/aside, dữ liệu application bổ sung độc lập, timer phút + timeout deadline; dialog accessible, guard bằng ref, unknown outcome khoá gửi.
- [x] Chạy tests mới cùng ut07; kiểm tra trạng thái kết quả và focus.

## Task 4: Kiểm chứng và review

- [x] Chạy type-check/lint/build và toàn bộ Playwright suite; sửa regression.
- [x] Chụp Playwright fixture tại 1440×1000, 834×1112, 390×844; kiểm tra 360px, nhìn ảnh và sửa nếu cần. Lưu docs/design/candidate-journey/ kèm README.
- [x] Review độc lập toàn bộ diff theo spec và review focus; xử lý findings quan trọng, kiểm tra lại.
- [x] Cập nhật trạng thái kế hoạch, báo kết quả thực tế và trạng thái git. Giữ local không commit/push trừ khi cần theo yêu cầu mới.

## Ledger

- 25/11/2026: user duyệt đặc tả. Native execution để tiếp tục công việc đã được cho phép, không thêm vòng duyệt kế hoạch.
- Ruling: tạo branch riêng tại workspace hiện có để tái sử dụng node_modules, không sửa branch HR; không cần tạo thêm checkout. Chỉ có đặc tả chưa tracked lúc bắt đầu.
- Tasks 1–3: complete. New behavior tests observed failing before implementation; new journey suite 11 behavior tests + 3 screenshot cases passed; related suite 25 passed, 3 explicit screenshot skips. Withdrawal conflict regression added separately: red dialog disappeared, green preserved dialog/draft after refresh. Type-check exit 0; lint 0 errors with preexisting commitlint warning.
- Ruling: preserve application detail during background refresh so an open withdrawal dialog survives 409; successful withdrawal updates query data directly and invalidates list. Error refresh still hides unusable detail.
- Visual run initially overlapped another Playwright invocation and lost shared dev server; rerun serially. No test assertion weakened. Dialog screenshots use viewport to avoid full-page fixed-position capture artifacts.
- Independent source review found four P2 issues: paused offline reconciliation, lost interview focus, late initial GET crossing deadline, and stale expiry GET overwriting authoritative mutation. All reproduced by new regressions and fixed; focused review regression run: 5 passed. Reconciliation uses bounded service read; mutations cancel outstanding detail GET before publishing server response; clock wakes on late deadline/visibility; removed interview cards restore focus without changing tabs.
- First full suite overlapped the separate review regression run: 92 passed, 3 artifact ENOENT failures, 7 skipped; test trace directories are shared. Final suite is being rerun without overlapping Playwright invocations.
- Final verification: `npm run type-check`, `npm run lint`, `npm run build` exit 0. Lint has one preexisting anonymous default export warning in commitlint.config.mjs. `npx playwright test --workers=2`: 99 passed, 7 skipped (one live-stack test and six explicitly gated screenshot cases). `JOURNEY_SCREENSHOTS=1 npx playwright test e2e/candidate-journey.spec.ts --workers=2`: 19 passed, including 21 screenshots. Inspected desktop/tablet/mobile renders; 360px overflow and keyboard focus covered by behavior tests.
- Restored old screenshot artifacts regenerated by existing E2E; retained only new candidate-journey image set. No dependency/API contract changes. Branch `codex/candidate-application-journey`, local uncommitted changes, no push/merge.
