# Rikkei Jobs – Mock hệ thống ngoài

Server giả lập các hệ thống bên ngoài mà backend Rikkei Jobs cần gọi: **LMS**, **Khảo thí**, **lms-pm** và **Rikkei Meet**.
Mock là một tiến trình riêng, chạy độc lập với backend, để có thể tắt mock khi kiểm tra backend xử lý lỗi (NT-05, TT-02, DT-02).

> Chỉ **backend** gọi mock. Frontend không bao giờ gọi thẳng mock, vì khoá dịch vụ chỉ nằm ở backend.

## Yêu cầu

- Node.js 18 trở lên; dùng Node.js 24.x nếu chạy cùng môi trường frontend theo [README gốc](../README.md#1-chuẩn-bị-môi-trường).

## Cách chạy

```bash
cd mock
npm ci
npm start
```

Chạy thành công khi terminal in ra:

```
Mock chạy ở http://localhost:9000
```

Tắt mock: `Ctrl + C`. Dữ liệu sinh ra trong lúc chạy sẽ mất, như vậy là đúng.

Chạy từ gốc repo và mở terminal riêng cho mock. PowerShell có thể dùng `npm.cmd` nếu `npm.ps1` bị chặn. Mock dùng biến môi trường của tiến trình, không tự đọc `.env` ở gốc.

## Biến môi trường

| Biến | Mặc định | Ý nghĩa |
| --- | --- | --- |
| `PORT` | `9000` | Cổng mock lắng nghe |
| `SERVICE_KEY` | `dev-key` | Giá trị header `X-Service-Key` hợp lệ |

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
- Vé SSO, lượt ghi danh, thành viên đợt và nhật ký request chỉ giữ trong bộ nhớ.
- Sửa `data.json` rồi gọi `POST /mock/reload` để nạp lại mà không phải tắt mock.
- Reload không xóa vé, ghi danh, thành viên đợt hoặc nhật ký. Khởi động lại tiến trình để xóa trạng thái trong bộ nhớ; `/mock/reset` chưa được triển khai.
- Fixture có điểm học tập/thi/IOC trên thang 10; `attendanceRate`, `homeworkRate` là phần trăm, `rpoint` là điểm tích lũy riêng và có thể lớn hơn 100. Backend cần chuẩn hóa điểm về thang năng lực 100 theo quy tắc nghiệp vụ, không áp dụng trực tiếp bậc năng lực lên điểm thô.

Tài khoản mẫu trong LMS:

| lmsUserId | Họ tên | Vai trò | Đặc điểm dùng để test |
| --- | --- | --- | --- |
| `lms-sv-001` | Nguyễn Văn An | CANDIDATE | Mạnh Java, đã thực tập IOC |
| `lms-sv-002` | Trần Thị Bình | CANDIDATE | Mạnh frontend, chưa thực tập |
| `lms-sv-003` | Lê Minh Châu | CANDIDATE | Điểm còn yếu, chưa thực tập |
| `lms-mt-001` | Phạm Quốc Dũng | MENTOR | |
| `lms-st-001` | Hoàng Thu Hà | STAFF | |
| `lms-ad-001` | Đỗ Thanh Hải | ADMIN | |

Tài khoản HR không có trong LMS (HR đăng nhập bằng email + mật khẩu ở backend).

## Endpoint

Hợp đồng mục tiêu và các phần chưa triển khai: xem [conventions](../docs/conventions.md#5-mock-hệ-thống-ngoài). Bảng dưới đây phản ánh trạng thái trong `server.js`; fixture đã có không có nghĩa endpoint đã được triển khai.

SSO hiện tại: cấp vé trả `201 {ticket, issuedAt}`, verify trả `200 {lmsUserId, email, fullName, role, issuedAt}`. Vé không tồn tại trả `404 NOT_FOUND`. Mock cho phép verify lại vé và không tự kiểm tra tuổi vé; backend chịu trách nhiệm kiểm tra hết hạn/chống dùng lại.

| # | Method + đường dẫn | Trạng thái |
| --- | --- | --- |
| 1 | `POST /lms/sso/tickets` | Đã có |
| 2 | `POST /lms/sso/verify` | Đã có |
| 3 | `GET /lms/students/{lmsUserId}` | Đang làm |
| 4 | `GET /lms/students/{lmsUserId}/results` | Đang làm |
| 5 | `GET /lms/courses?skill=JAVA` | Đang làm |
| 6 | `POST /lms/enrollments` | Đang làm |
| 7 | `GET /lms/enrollments/{id}` | Đang làm; hợp đồng mục tiêu tra theo ID lượt ghi danh |
| 8 | `GET /exam/students/{lmsUserId}/results` | Đang làm |
| 9 | `GET /pm/students/{lmsUserId}/internships` | Đang làm |
| 10 | `POST /pm/batches/{batchId}/members` | Đang làm |
| 11 | `POST /meet/rooms` | Đang làm |

Công cụ test (không cần khoá):

| Endpoint | Việc nó làm | Trạng thái |
| --- | --- | --- |
| `POST /mock/reload` | Đọc lại `data.json` | Đã có |
| `GET /mock/requests` | 100 request gần nhất (thời điểm, method, path, body, có khoá hay không) | Đã có |
| `PATCH /mock/enrollments` | Sửa `progress`, `score`, `status` của một lượt ghi danh | Đang làm |
| `POST /mock/reset` | Xoá vé, ghi danh, thành viên đợt, nhật ký request | Đang làm |

## Định dạng lỗi

Mọi lỗi đều có dạng `{"code": "...", "message": "..."}`.

| HTTP | code | Khi nào |
| --- | --- | --- |
| 400 | `INVALID_JSON` | Body không phải JSON hợp lệ |
| 401 | `INVALID_SERVICE_KEY` | Thiếu hoặc sai `X-Service-Key` |
| 404 | `NOT_FOUND` | Không có user, vé, khoá học, đợt... |
| 404 | `ROUTE_NOT_FOUND` | Gọi đường dẫn không tồn tại |
| 500 | `INTERNAL_ERROR` | Lỗi không mong muốn trong mock |

## Thử nhanh

Mở `requests.http` bằng IntelliJ (bấm mũi tên xanh) hoặc VS Code + extension REST Client (bấm **Send Request**).
File có sẵn các ca: thiếu/sai khoá → 401, cấp vé → verify, vé bịa → 404, sai đường dẫn, nhật ký request, reload, body hỏng.
