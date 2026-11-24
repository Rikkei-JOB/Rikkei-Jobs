# Candidate top bar và thông báo trong website

Hoàn thành ngày 24/11/2026. Chỉ khu vực ứng viên dùng top bar; các vai trò khác giữ AppShell cũ.

## Hành vi

Top bar gồm Việc làm, nhóm Ứng tuyển (Đơn ứng tuyển/Offer/Phỏng vấn), Hồ sơ, nhóm Phát triển (Lộ trình/Trợ lý AI), chuông và tài khoản. Dưới 1200px, điều hướng chuyển thành menu; chuông luôn hiển thị. Tên tài khoản thiếu có fallback để không chặn màn nghiệp vụ.

Thông báo lưu trong PostgreSQL theo user ID từ JWT, có số chưa đọc, phân trang, đọc từng mục/đọc tất cả và liên kết đến nội dung. Frontend cập nhật định kỳ 60 giây khi tab hoạt động, khi lấy lại focus và mở panel. Query cache gắn với user/token, dùng AbortSignal; phản hồi ghi đến muộn sau đăng xuất không điều hướng lại hoặc tác động phiên tiếp theo. Lỗi cập nhật không làm mất dấu chưa đọc.

Các sự kiện được lưu cùng giao dịch nghiệp vụ: thay đổi trạng thái đơn, offer mới (đúng offer ID), lời mời/chọn/huỷ lịch phỏng vấn và nhắc 24h/1h. Dedupe theo sự kiện; giao dịch rollback không để lại thông báo. Không phụ thuộc email gửi thành công, không đưa ghi chú nội bộ HR vào nội dung. Các trạng thái đã tồn tại trước migration không được tự dựng lại thành thông báo cũ.

## API và triển khai

Hợp đồng: [OpenAPI](candidate-notifications-openapi.yaml), [request mẫu](candidate-notifications.http). Endpoint dưới gateway `/api/v1/recruitment/me/notifications`; GET trả `{items,total,page,size,unreadCount}`, POST `/{id}/read` và `/read-all` trả 204. Chỉ CANDIDATE; 404 cho ID của người khác; giới hạn page/size và lỗi theo quy ước dự án.

Flyway **V8__candidate_notifications.sql** tạo bảng/index mới khi recruitment-service bản mới khởi động. Cần build và khởi động lại backend theo quy trình môi trường của bạn; không cần đổi route gateway. Không restart stack đang chạy trong nhiệm vụ này. Đây là thông báo trong website, không yêu cầu cấp quyền browser push.

## Kiểm chứng

- Recruitment: **154 tests, 0 failures/errors**; có kiểm tra API qua HTTP server thật bằng JDK HttpClient, PostgreSQL thật, ownership, rollback, dedupe, offer href và các sự kiện phỏng vấn. Common modules trong reactor cùng được chạy thành công.
- Frontend: **71 passed, 1 skipped**. Bài live HS05 bỏ qua vì cần stack nghiệm thu riêng. Lượt toàn bộ phát hiện và sửa lỗi avatar khi auth/me thiếu fullName; lượt cuối UT-07 cũng qua.
- Thêm regression cho phản hồi read đến muộn sau logout và tài khoản kế tiếp, phân trang/global unread count, read-all, lỗi đọc và retry, empty/error và mobile Escape/focus/overflow.
- Rà soát độc lập không có phát hiện material mới; khoảng trống kiểm thử session đã được bổ sung bằng regression ở trên.
- Production build và type-check đã qua. Lint: 0 errors, 1 cảnh báo có sẵn ở `commitlint.config.mjs`. Cả cache Playwright và các thư mục `.next` phát sinh lồng nhau được bỏ qua khỏi Git/ESLint.
- Ảnh được xem trực tiếp ở desktop 1440px, tablet 834px, mobile 390px; test thêm popup 360×640. Ảnh dùng **API fixture**, backend riêng đã được kiểm tra với DB/HTTP thật; chưa nghiệm thu toàn stack đang chạy của người dùng.

Playwright dùng `.next-playwright` để không tranh lock với dev server 3000 đang có. Thư mục được ignore trong Git và ESLint; Next thêm type paths tương ứng vào tsconfig. Build kiểm chứng dùng `PLAYWRIGHT_TEST=1` để tiếp tục cách ly thư mục output; runtime API vẫn lấy cấu hình môi trường, không có fixture fallback trong production.

## Lệnh chạy lại

Tại gốc repo, Docker tạo DB kiểm thử độc lập (không trỏ vào DB ứng dụng):

```powershell
docker run -d --name rikkei-candidate-notifications-test -p 127.0.0.1:55447:5432 -e POSTGRES_HOST_AUTH_METHOD=trust -e POSTGRES_DB=candidate_notification_tests --tmpfs /var/lib/postgresql/data postgres:16-alpine
$env:UT01_TEST_DB_URL='jdbc:postgresql://127.0.0.1:55447/candidate_notification_tests'
$env:UT01_TEST_DB_USER='postgres'
$env:UT01_TEST_DB_PASSWORD=''
mvn -f server/pom.xml -pl services/recruitment-service -am test
```

Trong `web/`:

```powershell
npm run test:e2e -- --workers=2
npm run type-check
npm run lint
$env:PLAYWRIGHT_TEST='1'
npm run build
```

Ảnh mới lưu tại [candidate-topbar](../design/candidate-topbar/). Ảnh cũ do các test khác ghi lại đã được khôi phục để không làm lẫn thay đổi. Chưa commit/push.
