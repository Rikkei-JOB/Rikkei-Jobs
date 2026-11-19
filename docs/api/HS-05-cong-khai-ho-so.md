# HS-05 · Công khai hồ sơ có đồng ý

Ứng viên thao tác tại **Hồ sơ của tôi → Công khai hồ sơ**. Khi ứng viên tích đồng ý và gửi, hồ sơ chuyển sang chờ duyệt. Cán bộ có thể trả lại kèm lý do hoặc duyệt công khai qua API. Ứng viên có thể rút khi đang chờ hoặc đã công khai. Lịch sử hiển thị lần gần nhất trước. Sáu endpoint và mẫu dữ liệu nằm trong [OpenAPI HS-05](hs05-openapi.yaml).

## Chạy và thử trên máy

Từ gốc repo, dùng PostgreSQL và tài khoản seed của môi trường dev. Cấu hình dưới đây dùng gateway cổng 8080 và web cổng 3000.

```powershell
$env:WEB_BASE_URL = 'http://localhost:3000'
docker compose up -d --build
npm.cmd ci --prefix web
$env:NEXT_PUBLIC_API_URL = 'http://localhost:8080/api/v1'
$env:NEXT_PUBLIC_LMS_MOCK = 'true'
npm.cmd --prefix web run dev
```

Mở `http://localhost:3000/candidate/profile`. Cán bộ dùng nhóm profile-service tại `http://localhost:8080/api/docs`. Dùng JWT tương ứng từng vai trò.

| Vai trò mẫu | Đăng nhập dev | Thao tác |
|---|---|---|
| Ứng viên | Qua LMS với `lms-sv-001` | Xem trạng thái riêng tư, tích đồng ý, gửi duyệt |
| Cán bộ | Qua LMS với `lms-st-001` | Lấy `userId` từ hàng đợi PENDING, trả lại với lý do “Vui lòng bổ sung mục tiêu nghề nghiệp.” |
| Ứng viên | Cùng tài khoản | Xem lý do, sửa giới thiệu, tích đồng ý và gửi lại |
| Cán bộ | Cùng tài khoản | Duyệt hồ sơ đang chờ |
| HR | `hr1@rikkeijobs.test` / `Test@123` | Đọc `GET /api/v1/profiles/{userId}/skills` được 200. Ứng viên rút trên web, HR gọi lại được 404 |

## Chạy nghiệm thu trình duyệt với API thật

Test dùng database dev riêng, bắt đầu và kết thúc với ứng viên mẫu ở trạng thái PRIVATE. Test cập nhật phần giới thiệu và tạo năm bản ghi lịch sử cho mỗi lần chạy. Playwright tự mở Next.js tại cổng 3101. Gateway và các service phải cho phép đúng origin này. Từ gốc repo, trên stack Compose dùng cho nghiệm thu, chạy:

```powershell
$env:WEB_BASE_URL = 'http://127.0.0.1:3101'
docker compose up -d --build
$env:NEXT_PUBLIC_API_URL = 'http://localhost:8080/api/v1'
$env:HS05_LIVE = '1'
$env:UT01_BROWSER_CHANNEL = 'msedge'
npm.cmd --prefix web run test:e2e -- hs05-publication-live --workers=1 --trace=off
```

Mặc định ảnh được ghi vào thư mục output của Playwright. Đặt `HS05_SCREENSHOT_DIR` để chọn thư mục ảnh. Các ảnh dưới được chụp trên Edge qua gateway, identity-service và profile-service thật với PostgreSQL 18 và LMS mock của repo.

| Desktop | Mobile |
|---|---|
| [Riêng tư](screenshots/HS-05/private-desktop.png) | [Trả lại kèm lý do](screenshots/HS-05/rejected-mobile.png) |
| [Chờ duyệt](screenshots/HS-05/pending-desktop.png) | [Đã công khai và lịch sử](screenshots/HS-05/public-mobile.png) |

## Kết quả nghiệm thu

- Nút gửi bị khóa trước khi tích đồng ý. Approve hồ sơ PRIVATE trả 409. Reject thiếu lý do trả 400.
- Sau khi cán bộ trả lại ở phiên khác, yêu cầu rút từ giao diện cũ nhận 409. Giao diện tự tải lại trạng thái và lý do, cho phép sửa rồi gửi lại.
- Sau duyệt, HR đọc được kỹ năng. Request HR ngay sau khi ứng viên rút nhận 404. Chuỗi sự kiện là SUBMIT, REJECT, SUBMIT, APPROVE, WITHDRAW với đúng người thực hiện và thời điểm.
- Trên PostgreSQL 18, đã áp dụng SQL V9/V10 cho bảng V5 có sẵn hai hồ sơ mẫu PRIVATE và PUBLIC. Cả hai hồ sơ còn nguyên. Hồ sơ PUBLIC cũ thiếu thời điểm đồng ý được loại khỏi phạm vi công khai.
- HS-05 phụ thuộc HS-02 và bàn giao điều kiện lọc hồ sơ công khai qua view `profile.public_candidate_ids`. DN-03 phụ thuộc HS-05 và dùng điều kiện này cho `GET /jobs/{id}/candidates`. Khi nghiệm thu tích hợp DN-03, chạy lại cùng bộ lọc trước và sau khi ứng viên rút để kiểm tra danh sách và tổng số kết quả.
