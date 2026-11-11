# Rikkei Jobs – Mock hệ thống ngoài

Server giả lập các hệ thống bên ngoài mà backend Rikkei Jobs cần gọi: **LMS**, **Khảo thí**, **lms-pm** và **Rikkei Meet**.
Mock là một tiến trình riêng, chạy độc lập với backend, để có thể tắt mock khi kiểm tra backend xử lý lỗi (NT-05, TT-02, DT-02).

> Chỉ **backend** gọi mock. Frontend không bao giờ gọi thẳng mock, vì khoá dịch vụ chỉ nằm ở backend.

Hợp đồng API đầy đủ (body, kết quả, mã lỗi): [docs/mock-contract.md](../docs/mock-contract.md).

## Yêu cầu

- Node.js 18 trở lên; dùng Node.js 24.x nếu chạy cùng môi trường frontend theo [README gốc](../README.md#1-chuẩn-bị-môi-trường).

## Cách chạy

Mở terminal riêng cho mock, từ gốc repo:

```bash
cd mock
npm ci
npm start
```

Chạy thành công khi terminal in ra:

```
Mock chạy ở http://localhost:9000
```

Tắt mock: `Ctrl + C`. Dữ liệu sinh ra trong lúc chạy sẽ mất, như vậy là đúng. Đây cũng là cách kiểm tra ca "mock tắt" (NT-05, TT-02, DT-02).

PowerShell có thể dùng `npm.cmd` nếu `npm.ps1` bị chặn. Mock dùng biến môi trường của tiến trình, không tự đọc `.env` ở gốc.

## Biến môi trường

| Biến | Mặc định | Ý nghĩa |
| --- | --- | --- |
| `PORT` | `9000` | Cổng mock lắng nghe |
| `SERVICE_KEY` | `dev-key` | Giá trị header `X-Service-Key` hợp lệ |
| `BACKEND_URL` | `http://localhost:8080` | Chỉ dùng cho script TT-04: địa chỉ backend Rikkei Jobs |

Ví dụ đổi cổng:

- PowerShell: `$env:PORT=9100; npm start`
- Bash / Git Bash: `PORT=9100 npm start`

## Khoá dịch vụ

- Mọi request vào `/lms`, `/exam`, `/pm`, `/meet` phải có header `X-Service-Key: dev-key`.
- Thiếu hoặc sai khoá → `401 {"code":"INVALID_SERVICE_KEY","message":"..."}`.
- Middleware parse JSON chạy trước kiểm tra key: body JSON hỏng trả `400 INVALID_JSON`, kể cả khi thiếu/sai key.
- Các công cụ test dưới `/mock/*` **không** cần khoá; backend không bao giờ gọi tới chúng.

## Dữ liệu

- Dữ liệu cố định nằm trong `data.json`, nạp khi khởi động (không dùng database).
- Vé SSO, lượt ghi danh, thành viên đợt, phòng họp và nhật ký request chỉ giữ trong bộ nhớ.
- Sửa `data.json` rồi gọi `POST /mock/reload` để nạp lại mà không phải tắt mock. Reload không xoá trạng thái trong bộ nhớ; muốn xoá thì gọi `POST /mock/reset`.
- Fixture có điểm học tập/thi/IOC trên thang 10; `attendanceRate`, `homeworkRate` là phần trăm, `rpoint` là điểm tích luỹ riêng và có thể lớn hơn 100. Backend cần chuẩn hoá điểm về thang năng lực 100 theo quy tắc nghiệp vụ, không áp dụng trực tiếp bậc năng lực lên điểm thô.

Tài khoản mẫu trong LMS:

| lmsUserId | Họ tên | Vai trò | Đặc điểm dùng để test |
| --- | --- | --- | --- |
| `lms-sv-001` | Nguyễn Văn An | CANDIDATE | Mạnh Java, đã thực tập IOC (`IOC-JB-2026-09`) |
| `lms-sv-002` | Trần Thị Bình | CANDIDATE | Mạnh frontend, chưa thực tập |
| `lms-sv-003` | Lê Minh Châu | CANDIDATE | Điểm còn yếu, chưa thực tập |
| `lms-mt-001` | Phạm Quốc Dũng | MENTOR | |
| `lms-st-001` | Hoàng Thu Hà | STAFF | |
| `lms-ad-001` | Đỗ Thanh Hải | ADMIN | |

Tài khoản HR không có trong LMS (HR đăng nhập bằng email + mật khẩu ở backend).

## Endpoint

Đủ 11 endpoint theo hợp đồng. Chi tiết body và mã lỗi xem [docs/mock-contract.md](../docs/mock-contract.md).

| # | Method + đường dẫn | Thành công | Lỗi |
| --- | --- | --- | --- |
| 1 | `POST /lms/sso/tickets` | `201 {ticket, issuedAt}` | 404 user không có |
| 2 | `POST /lms/sso/verify` | `200 {lmsUserId, email, fullName, role, issuedAt}` | 404 vé không có |
| 3 | `GET /lms/students/{lmsUserId}` | `200 {lmsUserId, fullName, email, program, classCode, courseName}` | 404 không có hoặc không phải CANDIDATE |
| 4 | `GET /lms/students/{lmsUserId}/results` | `200` mảng kết quả môn | – |
| 5 | `GET /lms/courses?skill=JAVA` | `200` mảng khoá học; có `skill` thì lọc theo mã | – |
| 6 | `POST /lms/enrollments` | `201 {lmsUserId, courseId, progress, score, status, enrolledAt}` | 400 thiếu trường, 404, 409 `ALREADY_ENROLLED` |
| 7 | `GET /lms/enrollments/{lmsUserId}` | `200` mảng ghi danh | – |
| 8 | `GET /exam/students/{lmsUserId}/results` | `200` mảng điểm thi | – |
| 9 | `GET /pm/students/{lmsUserId}/internships` | `200` mảng đợt thực tập | – |
| 10 | `POST /pm/batches/{batchId}/members` | `201 {batchId, lmsUserId, addedAt}` | 400 thiếu trường, 404, 409 `ALREADY_MEMBER` |
| 11 | `POST /meet/rooms` | `201 {roomId, roomUrl, title, startAt, endAt}` | 400 thiếu trường / thời gian sai |

API trả danh sách luôn trả `[]` khi không có dữ liệu, không trả 404.

SSO: mock cho phép verify lại cùng vé và không tự kiểm tra tuổi vé; backend chịu trách nhiệm chặn vé đã dùng và vé quá 60 giây (NT-01).

## Công cụ test (không cần khoá)

| Endpoint | Việc nó làm |
| --- | --- |
| `POST /mock/reload` | Đọc lại `data.json` mà không tắt mock |
| `GET /mock/requests` | 100 request gần nhất (thời điểm, method, path, body, có khoá hay không) |
| `PATCH /mock/enrollments` | Sửa `progress`, `score`, `status` của một lượt ghi danh: `{lmsUserId, courseId, progress?, score?, status?}` |
| `POST /mock/reset` | Xoá vé, ghi danh, thành viên đợt, phòng họp và nhật ký request trong bộ nhớ (chạy trước mỗi lần demo) |

## Script TT-04: gửi link phiếu đánh giá

lms-pm gọi ngược sang backend để lấy link phiếu đánh giá cho mentor. Script đọc thực tập sinh của đợt trong `data.json`, gọi `POST {BACKEND_URL}/api/v1/integrations/pm/evaluation-links` với body `{mentorEmail, lmsUserId, batchId}` kèm `X-Service-Key`, rồi in các link nhận được.

```bash
cd mock
npm run evaluation-links -- IOC-JB-2026-09
```

Hoặc `node send-evaluation-links.js IOC-JB-2026-09`. Backend phải đang chạy; nếu không, script báo lỗi kết nối và thoát với mã 1.

## Định dạng lỗi

Mọi lỗi đều có dạng `{"code": "...", "message": "..."}`.

| HTTP | code | Khi nào |
| --- | --- | --- |
| 400 | `INVALID_JSON` | Body không phải JSON hợp lệ |
| 400 | `VALIDATION_ERROR` | Thiếu trường bắt buộc hoặc giá trị không hợp lệ |
| 401 | `INVALID_SERVICE_KEY` | Thiếu hoặc sai `X-Service-Key` |
| 404 | `NOT_FOUND` | Không có user, vé, khoá học, đợt, lượt ghi danh |
| 404 | `ROUTE_NOT_FOUND` | Gọi đường dẫn không tồn tại |
| 409 | `ALREADY_ENROLLED` | Ghi danh cùng khoá học lần thứ hai |
| 409 | `ALREADY_MEMBER` | Thêm cùng học viên vào cùng đợt lần thứ hai |
| 500 | `INTERNAL_ERROR` | Lỗi không mong muốn trong mock |

## Thử nhanh

Mở `requests.http` bằng IntelliJ (bấm mũi tên xanh) hoặc VS Code + extension REST Client (bấm **Send Request**).
File có đủ 11 endpoint cho cả 3 ứng viên, các ca lỗi (401, 404, 409, 400) và 4 công cụ `/mock/*`.
