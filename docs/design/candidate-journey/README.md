# Ảnh giao diện hành trình ứng viên

Ảnh chụp bằng Playwright từ giao diện local, sử dụng API fixtures trong `web/e2e/candidate-journey.spec.ts`. Tên ứng viên, doanh nghiệp, vị trí, lương và lịch là dữ liệu kiểm thử; thời gian ảnh cố định tại 09:00 ngày 03/01/2030 theo giờ Việt Nam.

| Màn hình | Desktop | Tablet | Mobile |
| --- | --- | --- | --- |
| Đơn ứng tuyển | [Ảnh](applications-desktop.png) | [Ảnh](applications-tablet.png) | [Ảnh](applications-mobile.png) |
| Chi tiết hồ sơ | [Ảnh](application-detail-desktop.png) | [Ảnh](application-detail-tablet.png) | [Ảnh](application-detail-mobile.png) |
| Chọn giờ phỏng vấn | [Ảnh](interviews-action-desktop.png) | [Ảnh](interviews-action-tablet.png) | [Ảnh](interviews-action-mobile.png) |
| Lịch đã xác nhận | [Ảnh](interviews-upcoming-desktop.png) | [Ảnh](interviews-upcoming-tablet.png) | [Ảnh](interviews-upcoming-mobile.png) |
| Chi tiết offer | [Ảnh](offer-detail-desktop.png) | [Ảnh](offer-detail-tablet.png) | [Ảnh](offer-detail-mobile.png) |
| Dialog từ chối | [Ảnh](offer-dialog-desktop.png) | [Ảnh](offer-dialog-tablet.png) | [Ảnh](offer-dialog-mobile.png) |

Kích thước: desktop 1440×1000, tablet 834×1112, mobile 390×844. Các trang chụp full page; dialog chụp viewport để phản ánh đúng backdrop, vị trí và phần cuộn. Kiểm thử riêng có viewport 360px, nội dung dài, keyboard/Escape/focus, conflict, deadline và lỗi mạng.

Chạy từ thư mục `web` bằng PowerShell, khi không có phiên Playwright khác đang chạy:

```powershell
$env:JOURNEY_SCREENSHOTS='1'
npx playwright test e2e/candidate-journey.spec.ts --workers=2
```

Bộ ảnh gồm 18 ảnh, cập nhật theo luồng email cần đăng nhập: bỏ danh sách/menu Offer, giữ trang phản hồi và quay lại hồ sơ tương ứng. Bộ ảnh xác minh giao diện với fixture; không xác minh tích hợp backend thật hoặc khả năng truy cập phòng họp bên ngoài.
