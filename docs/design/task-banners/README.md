# Banner ứng tuyển và phỏng vấn

Ảnh Playwright chụp local ngày 25/11/2026, theo đặc tả hướng A. Có 12 ảnh trước và 12 ảnh sau, dùng cùng fixture ở ba viewport. Đồng hồ fixture: 09:00 ngày 03/01/2030, giờ Việt Nam; tên người, lịch và dữ liệu đều là dữ liệu mock. Ảnh chứng minh giao diện render và responsive, không chứng minh API/backend production.

| Trang | Desktop 1440×1000 | Tablet 834×1112 | Mobile 390×844 |
| --- | --- | --- | --- |
| Ứng viên · Ứng tuyển | [Trước](before/candidate-applications-desktop.png) · [Sau](after/candidate-applications-desktop.png) | [Trước](before/candidate-applications-tablet.png) · [Sau](after/candidate-applications-tablet.png) | [Trước](before/candidate-applications-mobile.png) · [Sau](after/candidate-applications-mobile.png) |
| Ứng viên · Phỏng vấn | [Trước](before/candidate-interviews-desktop.png) · [Sau](after/candidate-interviews-desktop.png) | [Trước](before/candidate-interviews-tablet.png) · [Sau](after/candidate-interviews-tablet.png) | [Trước](before/candidate-interviews-mobile.png) · [Sau](after/candidate-interviews-mobile.png) |
| HR · Ứng tuyển | [Trước](before/hr-applications-desktop.png) · [Sau](after/hr-applications-desktop.png) | [Trước](before/hr-applications-tablet.png) · [Sau](after/hr-applications-tablet.png) | [Trước](before/hr-applications-mobile.png) · [Sau](after/hr-applications-mobile.png) |
| HR · Phỏng vấn | [Trước](before/hr-interviews-desktop.png) · [Sau](after/hr-interviews-desktop.png) | [Trước](before/hr-interviews-tablet.png) · [Sau](after/hr-interviews-tablet.png) | [Trước](before/hr-interviews-mobile.png) · [Sau](after/hr-interviews-mobile.png) |

Ảnh full-page nên có thể cao hơn viewport. HR tablet giữ vùng lịch cuộn ngang có nhãn; mobile chuyển lịch sang agenda theo pattern hiện có. Minh hoạ banner ẩn dưới 768px, nội dung tự tăng chiều cao.

Tạo lại ảnh sau và kiểm tra trạng thái bằng PowerShell tại `web`:

```powershell
$env:TASK_BANNER_SCREENSHOTS='1'
npx playwright test e2e/task-banners.visual.spec.ts --workers=2
```

Chỉ dùng `TASK_BANNER_PHASE=before` để ghi baseline trước khi sửa; không dùng cờ này trên code mới vì sẽ ghi đè ảnh cũ. Các kiểm tra bao gồm không tràn trang ở 360px, H1 duy nhất, CTA tìm việc đúng đích và nút Tuần/Danh sách HR dùng được bằng bàn phím. Bộ lọc, tabs, phản hồi offer và khôi phục focus được kiểm chứng thêm trong suite journey/HR hiện có.
