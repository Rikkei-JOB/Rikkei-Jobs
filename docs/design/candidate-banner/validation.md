# Banner minh hoạ công nghệ — 24/11/2026

Đã triển khai phương án người dùng chọn: banner gradient đỏ trầm, tiêu đề “Bước tiếp theo cho sự nghiệp của bạn”, link “Hoàn thiện hồ sơ” và minh hoạ vector CV/kỹ năng/cơ hội. Banner thay phần tiêu đề cũ; thanh tìm kiếm nằm ngay dưới. Lối tắt “Ứng tuyển của tôi” chuyển sang hàng kết quả.

Minh hoạ là SVG nội bộ, không cần tải ảnh ngoài hoặc dependency mới; không có dữ liệu hay số liệu giả. SVG trang trí được ẩn khỏi screen reader. Mobile giữ tiêu đề/link và làm mờ minh hoạ, bỏ mô tả phụ để banner gọn. Link có focus outline trắng tương phản với nền đỏ.

## Ảnh Playwright

Ảnh trang dùng API fixtures và được gắn nhãn. Chiều cao banner desktop/tablet là 220px, mobile 180px; không tràn ngang tại 360px.

| Viewport | Trong trang | Riêng banner |
| --- | --- | --- |
| 1440×1000 | [Desktop](desktop.png) | [1120×220](desktop-banner.png) |
| 834×1112 | [Tablet](tablet.png) | [786×220](tablet-banner.png) |
| 390×844 | [Mobile](mobile.png) | [358×180](mobile-banner.png) |

## Kiểm tra và sửa kèm theo

27 browser checks liên quan đã qua: banner/responsive, tìm việc, modal ứng tuyển, CV và notification top bar. Bài mobile Escape được chạy lặp sau sửa focus, 5/5 qua. Ảnh desktop/tablet/mobile được xem trực tiếp.

Lượt regression phát hiện lỗi focus có sẵn khi đóng modal mobile. Giữ controlled Dialog.Root để hoàn thành chu kỳ đóng, rồi trả focus qua onOpenChangeComplete sau khi thanh ứng tuyển hiện lại. Do form giữ mounted, reset loading/CV selection khi đóng để lần mở kế tiếp tải danh sách CV mới trước khi cho gửi. Regression mới xác nhận CV vừa xoá/đổi không được gửi trong lúc tải lại. Thư giới thiệu và trạng thái đối soát vẫn được giữ trong phiên form hiện tại.

Chạy lại trong `web/`:

```powershell
npx playwright test e2e/candidate-banner.visual.spec.ts e2e/candidate-career.spec.ts e2e/ut01-apply.spec.ts e2e/ut01-tracking.spec.ts e2e/candidate-topbar.spec.ts --workers=2
npm run type-check
npm run lint
$env:PLAYWRIGHT_TEST='1'
npm run build
```

Build kiểm chứng dùng thư mục cách ly đã có để giữ dev server hiện tại. Không sửa API/backend trong phần banner; không commit/push. Các thay đổi top bar và backend thông báo đang có được giữ nguyên.
