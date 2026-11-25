# Ảnh kiểm tra giao diện HR

Các ảnh trong thư mục được Playwright chụp từ ứng dụng local với **API fixtures** trong `web/e2e/hr-workspace.spec.ts`. Tên ứng viên, công ty, tin tuyển dụng và lịch là dữ liệu kiểm thử; không phải dữ liệu hệ thống đang vận hành.

Mỗi màn có ba kích thước: desktop 1440×1000, tablet 834×1112, mobile 390×844. Ảnh full-page có thể cao hơn viewport. Suite còn kiểm tra tràn ngang ở 360px và lịch trên mobile. Đồng hồ lịch sử dụng giờ Việt Nam; tuần đang xem theo ngày chạy kiểm thử.

| Màn | Tiền tố tên ảnh |
| --- | --- |
| Hồ sơ ứng tuyển | applications |
| Chi tiết hồ sơ | detail |
| Tin tuyển dụng | jobs |
| Hồ sơ công ty | company |
| Lịch phỏng vấn | calendar |

Chạy trong `web`, PowerShell:

```powershell
$env:HR_SCREENSHOTS = '1'
npx playwright test e2e/hr-workspace.spec.ts --workers=2
Remove-Item Env:HR_SCREENSHOTS
```

Suite mặc định bỏ qua ba bài chụp ảnh khi không đặt HR_SCREENSHOTS; các kiểm thử hành vi HR vẫn chạy. Ẩn Next.js dev indicator chỉ trong lúc chụp, không thay UI của ứng dụng.

HR hiện dùng top bar và menu tài khoản; chuông HR chờ đặc tả/API riêng. Điểm khớp tiếp tục dùng dữ liệu hiện có, không hiển thị thời điểm tính khi response chưa có trường này. Không thay nghiệp vụ offer, chuyển bước và phỏng vấn.
