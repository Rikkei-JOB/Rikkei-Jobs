# Authenticated email offer implementation plan

> **For agentic workers:** Use superpowers:executing-plans. Steps use checkbox syntax.

**Goal:** Offer phản hồi qua link email sau đăng nhập, không có menu/danh sách offer ứng viên.

**Architecture:** Giữ URL chi tiết và API authenticated. Tái sử dụng next-path cho quay lại sau login/SSO/401; chuẩn hoá whitelist và thêm đổi tài khoản tại màn sai quyền. Không thêm token hoặc database migration.

**Tech Stack:** Next.js/React/Base UI/React Query, Spring Mail, Playwright/JUnit.

**Spec:** docs/superpowers/specs/2026-11-25-email-offer-authenticated-design.md, approved 25/11/2026.

## Global constraints

- Giữ thay đổi ứng tuyển/phỏng vấn đang local; cùng branch codex/candidate-application-journey. Không commit/push/merge.
- Giữ trang `/candidate/offers/[id]`, API đọc/accept/decline và confirm-onboard.
- Chỉ POST sau xác nhận; không phát lại mutation sau login. Lý do từ chối tối đa 1.000 ký tự.
- Không thêm token email, resend, HR notification hoặc dependency.

## Review focus

- Phiên hết hạn trong POST: lưu pathname, login xong chỉ GET trạng thái mới.
- Sai tài khoản/role: không lộ offer cache; đổi tài khoản giữ đích an toàn.
- Redirect có protocol-relative/backslash hoặc sai role phải về home hợp lệ.
- Link email/thông báo cũ và URL danh sách cũ vẫn có đích phù hợp.
- Email deadline đúng UTC+7, nghiệp vụ accept/decline/onboard giữ nguyên.

## Task 1: Navigation and authenticated response page

Files: ROLE_MENU, candidate shell (dùng groups hiện có); offers/page.tsx; delete candidate-offer-list.tsx; candidate-offer-detail.tsx; offer-response-dialog.tsx; application-detail-view.tsx; e2e/candidate-journey.spec.ts; e2e/email-offer-auth.spec.ts.

- [x] Viết test không có menu Offer, route danh sách redirect, trang chi tiết quay lại hồ sơ, lý do max1000; chạy đỏ.
- [x] Bỏ item menu, redirect route danh sách, bỏ list component không còn caller; quay lại applicationId/fallback list, bỏ returnTo offer-list, đổi hướng dẫn đơn OFFER.
- [x] Đồng bộ limit reason1000, cập nhật screenshot cases để không còn list; chạy xanh cùng offer journey tests.

## Task 2: Login return and wrong-account handling

Files: helper/next-path.ts; services/api.ts; hooks/use-auth-guard.ts nếu cần; new auth/switch-account-button.tsx; layout/forbidden.tsx; offers/candidate-offer-detail.tsx; e2e/email-offer-auth.spec.ts.

Interfaces: next-path chỉ nhận pathname nội bộ thuộc role; SwitchAccountButton nhớ pathname rồi dùng useLogout hiện có (không sửa logout chung).

- [x] Viết test login/SSO, must-change-password, offer401→login→GET không POST lặp; wrong-account404/role403 đổi tài khoản; saved path độc hại bị từ chối. Chạy đỏ cho hành vi thiếu, ghi nhận hành vi đã có.
- [x] Cứng hoá helper whitelist, 401 nhớ pathname trước clear/redirect; sai quyền có đổi tài khoản trong scope offer; detail404 không hiển thị dữ liệu cũ.
- [x] Chạy auth tests cùng topbar notification và toàn bộ journey.

## Task 3: Email and final validation

Files: SmtpApplicationStatusMailSender.java và test JUnit gần nó; docs/design/candidate-journey/README.md; DESIGN.md/spec/plan.

- [x] Thêm JUnit xác minh link không đổi, instruction đúng tài khoản và deadline giờ Việt Nam; chạy đỏ.
- [x] Formatter giờ Việt Nam và câu hướng dẫn email, không đổi gửi/transaction. Chạy test backend liên quan.
- [x] Run type-check/lint/build và full Playwright; chụp lại viewport1440/834/390, kiểm tra360px; review độc lập và fix findings quan trọng; báo kết quả thật.

## Ledger

- Spec approved. Native execution continues existing authorised local work without another approval gate.
- Baseline: prior journey full suite99 passed7 skipped, type/lint/build pass. Preserve all existing local changes. No new worktree needed: this is a continuation replacing the offer portion on the same dedicated branch.
- RED evidence: missing menu removal/old-list redirect, account-switch action, password-return path, maxlength1000; unsafe backslash/encoded slash/dot-segment saved destinations. Initial email-link login and POST401 already passed (existing behavior); explicit pathname persistence strengthens determinism.
- GREEN: email-offer-auth.spec.ts 13 passed. SMTP status mail test observed failing on missing login instruction/UTC+7, then 2 passed. Maven was absent from PATH; fixed-version Apache Maven3.9.9 downloaded from Maven Central to ignored .runtime/tooling, no repository dependency changes.
- Type-check passed; lint0 errors with existing commitlint anonymous-default warning. Full E2E and final review underway. Offer list images removed; screenshot suite now captures18 images.
- First full suite:112 passed7 skipped. Independent review found cross-tab stale session and delayed API401/403 invalidating a newer account. Added three regressions: all red; after storage/focus/visibility session sync the cross-tab test passed and the two late-response tests remained red; after bearer-session guard all16 email auth tests passed. Root layout places AuthHydrator within QueryProvider to clear account-bound cache; delayed auth/me success is also guarded against replacing a new identity.
- Final verification: type-check/lint/build exit0 (lint retains one preexisting commitlint warning); full Playwright115 passed7 skipped; explicit journey visual/behavior run19 passed and18 screenshots regenerated. SMTP status sender2 tests passed. git diff --check passed. Old screenshot artifacts from existing suites restored; current candidate-journey captures retained. Local uncommitted changes on codex/candidate-application-journey, no push/merge.
