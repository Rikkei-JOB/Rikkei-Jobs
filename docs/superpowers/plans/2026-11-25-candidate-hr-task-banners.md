# Candidate and HR Task Banners Implementation Plan

> **For agentic workers:** Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Áp dụng banner hướng A cho bốn trang ứng tuyển/phỏng vấn ứng viên và HR.
**Architecture:** Một TaskBanner tại ui, CSS module và hai SVG minh hoạ. Callers truyền tên trang, mô tả, eyebrow, nội dung phụ và biến thể compact.
**Tech Stack:** Next.js, React, TypeScript, CSS Modules, Playwright; không dependency mới.
**Spec:** `docs/superpowers/specs/2026-11-25-candidate-hr-task-banners-design.md` (đã duyệt).

## Global Constraints

- Gradient `#8F1D2B → #B4232D`, H1 30/36px và mobile 24/30px; nội dung tự tăng chiều cao.
- Chỉ bốn trang danh sách/lịch; giữ nghiệp vụ, API, phân quyền và focus restoration hiện có.
- Tiếp tục nhánh local hiện tại để giữ các thay đổi ứng viên liên quan; không commit/push.

## Review Focus

- CSS scope của journey/HR không làm mất màu trắng và focus trên banner.
- 360px, chữ tiếng Việt và mô tả dài không tràn hoặc bị ảnh che.
- Chuyển Tuần/Danh sách vẫn hoạt động bằng bàn phím ngoài banner.
- H1 phỏng vấn ứng viên vẫn nhận focus khi danh sách cần khôi phục focus.
- Loading/rỗng/lỗi không làm biến mất banner hay tạo số liệu giả.

### Task 1: Shared banners and four integrations

**Files:** tạo `web/src/app/components/ui/task-banner.tsx`, `task-banner.module.css`; sửa applications-list, candidate-interviews, hr-application-list và hr-interview-calendar.
**Interface:** `TaskBanner({ title, description, eyebrow, illustration: "applications" | "interviews", compact?: boolean, children?: ReactNode, focusableTitle?: boolean })`.

- [x] Ghi visual fixture `web/e2e/task-banners.visual.spec.ts`; chụp trước ở ba viewport cho bốn trang. Dùng dữ liệu mock cố định, có kiểm tra H1 và thao tác thật, không test class CSS.
- [x] Tạo component, hai minh hoạ SVG decorative và CSS module; tích hợp bốn callers, chuyển cách xem lịch HR xuống toolbar.
- [x] Chụp sau; xem ảnh 12 màn, kiểm tra 360px, focus và các trạng thái rỗng/lỗi/loading. Chạy kiểm thử journey/HR hiện có.

### Task 2: Verification and handoff

**Files:** cập nhật `DESIGN.md`, đặc tả, plan; tạo `docs/design/task-banners/README.md` cùng ảnh trước/sau.

- [x] Chạy type-check, lint, build; chạy E2E liên quan. Kết quả kỳ vọng exit 0.
- [x] Review độc lập phạm vi banner, sửa vấn đề quan trọng và kiểm chứng nếu có.
- [x] Ghi bằng chứng và bàn giao ảnh; giữ thay đổi local.

## Ledger

- 25/11/2026: Người dùng duyệt đặc tả. Triển khai trực tiếp trong phiên hiện tại; thay đổi thuần trình bày được kiểm chứng bằng ảnh và kiểm thử hành vi sẵn có thay vì test lặp CSS.
- Task 1: Complete. Baseline 6/6 visual tests, 12 PNG trước; sau tích hợp: journey + HR + banner 30 passed / 6 screenshot tests cũ skip có chủ đích, log `.runtime/task-banner-after.log`. Xem đủ 12 PNG sau tại desktop/tablet/mobile. Bản HR compact tự tăng chiều cao khi có mô tả và múi giờ.
- Task 2 verification: Banner visual/state suite 10/10, log `.runtime/task-banner-states.log`; bao gồm 4 test loading→empty→error ở 360px. Type-check, lint, build exit 0 trong `.runtime/task-banner-{types-final,lint-final,build}.log`; lint còn 1 warning cũ của commitlint.config.mjs, không error. `git diff --check` exit 0. Không chạy lại toàn bộ suite không liên quan vì thay đổi chỉ trình bày.
- Final review: `/root/review_task_banners` kiểm tra độc lập phạm vi mới, không phát hiện lỗi chức năng. Góp ý hình HR còn giống ứng viên được xử lý bằng nhóm người kết nối trên hai SVG compact; đây là chi tiết thị giác theo đặc tả, kiểm chứng bằng ảnh thay vì test lặp SVG. Chụp lại 12 ảnh, xem lại đủ 6 ảnh HR; visual/state suite 10/10 trong `.runtime/task-banner-final.log`. Build/type-check/lint sau cùng exit 0 trong `.runtime/task-banner-{build-final,types-final,lint-final}.log`. Không còn góp ý tồn đọng; không commit/push.
