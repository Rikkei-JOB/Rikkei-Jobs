# Nghiệm thu giao diện ứng viên — 23/11/2026

Đã triển khai danh sách việc làm, chi tiết và hộp ứng tuyển theo [đặc tả đã duyệt](../../superpowers/specs/2026-11-23-candidate-job-ui-design.md). Tất cả ảnh trong thư mục này dùng **API fixture**, ghi nhãn ngay trên ảnh. Đây là kiểm chứng giao diện và client flow; chưa chứng minh tích hợp producer JD/CV hoặc backend thật.

## Giao diện

- Danh sách tìm theo vị trí/công ty, không phân biệt hoa thường và dấu tiếng Việt; lọc nhận hồ sơ; URL khôi phục đúng sau F5/Back/Forward; xoá bộ lọc giữ query không liên quan.
- Chi tiết có phần tóm tắt và hướng dẫn chuẩn bị ứng tuyển, sidebar thao tác trên desktop và CTA đáy trên mobile. Vị trí đóng giữ nút disabled và thông báo rõ.
- Modal có ngữ cảnh vị trí/công ty, CV bắt buộc, thư optional tối đa 5000 ký tự và giải thích bản chụp PDF. Giữ submit/reconcile hiện có; focus trở về trigger sau đóng cả desktop và mobile.
- Không thêm dữ liệu tuyển dụng không có trong CandidateJob; không thay API/phân quyền/AppShell hay thêm dependency.

## Ảnh để xem

Mỗi trạng thái có ảnh viewport và bản `-full.png` toàn trang.

| Kích thước | Danh sách | Chi tiết | Ứng tuyển |
| --- | --- | --- | --- |
| Desktop 1440×1000 | [Danh sách](desktop-01-list.png) | [Chi tiết](desktop-02-detail.png) | [Modal](desktop-03-apply.png) |
| Tablet 834×1112 | [Danh sách](tablet-01-list.png) | [Chi tiết](tablet-02-detail.png) | [Modal](tablet-03-apply.png) |
| Mobile 390×844 | [Danh sách](mobile-01-list.png) | [Chi tiết](mobile-02-detail.png) | [Modal](mobile-03-apply.png) |

Các ảnh `04-empty`, `05-closed`, `06-no-cv`, `07-error` ghi trạng thái bất thường ở cả ba viewport. Tổng cộng 42 PNG local, tách biệt với ảnh PTIT trong `../ptit-reference/`.

## Kiểm chứng

Chạy trong `web/`:

```powershell
npm run test:e2e -- --workers=2
npm run type-check
npm run lint
npm run build
```

Lượt toàn bộ E2E: **59 passed, 1 skipped**, 34.1 giây. Bài bỏ qua là `hs05-publication-live.spec.ts`, cần `HS05_LIVE=1` và stack/database nghiệm thu riêng. Type-check đã qua; lint không có lỗi, còn một cảnh báo có sẵn trong `commitlint.config.mjs` về anonymous default export. Production build đã qua với cả cấu hình kiểm thử và cấu hình local bình thường; bundle giao cho người dùng không chứa cấu hình fixture do lượt kiểm thử đặt vào process.

Các kiểm tra mới gồm loading, tìm không dấu, refresh/history/reset, lỗi tải và retry, luồng gửi CV, CV retry giữ thư, đối soát mất phản hồi, đóng nhận hồ sơ, modal tại 360×640 và trả focus. Ảnh được xem trực tiếp ở desktop/tablet/mobile; không tràn ngang trong các ảnh và ở bài kiểm tra 360px.

Kiểm tra tương phản các cặp dùng trong UI: primary #B4232D trên trắng 6.52:1, muted #64748B trên trắng 4.76:1, body #334155 trên trắng 10.35:1, success #166534 trên #F0FDF4 6.81:1. Metadata không đặt trên nền accent/pink; viền control dùng #64748B.

Rà soát độc lập không có phát hiện mới mức Critical/Important/Minor trong phạm vi UI. Hành vi có sẵn được giữ: trạng thái chưa rõ kết quả gửi nằm trong lần mở modal; đóng rồi mở lại không lưu trạng thái này. Backend vẫn là nơi chặn đơn trùng; không mở rộng nghiệp vụ đối soát trong thay đổi giao diện.

Next.js có cảnh báo nhiều lockfile từ các root package files có sẵn; không sửa cấu hình project hoặc các file đó chỉ để xoá cảnh báo. Các screenshot UT-02/UT-06/UT-07 bị test cũ ghi lại đã được khôi phục về trạng thái trước lượt chạy, giữ diff trong phạm vi nhiệm vụ.

## Chạy lại ảnh local

```powershell
npx playwright test e2e/candidate-career.visual.spec.ts --workers=1
```

`web/playwright.config.ts` đặt mặc định API kiểm thử khi chưa có biến process tương ứng, tránh phụ thuộc mock server trong `.env.local`. Không thêm endpoint hoặc fixture fallback trong production. Khi xem ứng dụng bằng `npm run dev`, vẫn cần cấu hình JD/CV producer phù hợp và đăng nhập ứng viên; thiếu JOB_API_PATH giữ trạng thái chưa tích hợp như trước.

Thay đổi được để trong workspace, chưa commit/push.
