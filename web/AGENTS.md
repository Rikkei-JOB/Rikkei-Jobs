<!-- BEGIN:nextjs-agent-rules -->

## This is NOT the Next.js you know

This version has breaking changes — APIs, conventions, and file structure may all differ from your training data. Read the relevant guide in `node_modules/next/dist/docs/` (resolved from this file's directory; in monorepos the `next` package may not be visible from the repo root) before writing any code. Heed deprecation notices.

This block is written and re-added by `next dev` — verify at `node_modules/next/dist/server/lib/generate-agent-files.js`. Removing it from a diff only re-creates the uncommitted change; committing it with your work keeps the tree clean.

<!-- END:nextjs-agent-rules -->

## Quy chuẩn giao diện chung

- Trước khi tạo hoặc sửa UI, đọc và áp dụng [DESIGN.md](../DESIGN.md) ở gốc repo.
- Dùng chung tokens, typography, spacing, component và quy tắc responsive trong tài liệu; ưu tiên tái sử dụng `src/app/components/ui/` và pattern đã có.
- `DESIGN.md` là chuẩn đích. Theme/component cũ còn khác chuẩn: chuyển đổi trong phạm vi nhiệm vụ, kiểm tra các màn bị ảnh hưởng và tránh tạo một bộ màu riêng cho mỗi vai trò.
- Khi thay đổi nền tảng thiết kế dùng chung, cập nhật `DESIGN.md` trong cùng PR; kiểm tra ảnh desktop/tablet/mobile và các trạng thái liên quan theo hướng dẫn trong file.
