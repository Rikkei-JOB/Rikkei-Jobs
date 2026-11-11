# Hợp đồng API mock hệ thống ngoài

Nguồn: tài liệu hướng dẫn dựng mock của mentor, mục 3 và mục 5. Cả nhóm code theo bảng này. **Muốn đổi thì cả nhóm thống nhất và sửa file này trước**, không tự ý đổi trong code.

Mock chạy riêng ở `http://localhost:9000` (thư mục `mock/`). Cách chạy: [mock/README.md](../mock/README.md).

## Quy tắc chung

- Header bắt buộc cho `/lms`, `/exam`, `/pm`, `/meet`: `X-Service-Key: dev-key`. Thiếu hoặc sai → `401 {"code":"INVALID_SERVICE_KEY","message":"..."}`.
- Lỗi luôn có dạng `{"code": "...", "message": "..."}`, giống quy ước API của backend.
- API trả danh sách (kết quả môn, điểm thi, thực tập, ghi danh, khoá học) trả `[]` khi không có dữ liệu, không trả 404.
- Body gửi lên và trả về đều là JSON; tên trường dạng camelCase.
- `{lmsUserId}` trong đường dẫn là ID LMS dạng chuỗi, ví dụ `lms-sv-001`.
- Đường dẫn mock **không** có tiền tố `/api/v1`.

## 11 endpoint

| # | Method + đường dẫn | Body gửi lên | Trả về khi đúng | Lỗi | Task dùng |
| --- | --- | --- | --- | --- | --- |
| 1 | `POST /lms/sso/tickets` | `{lmsUserId}` | `201 {ticket, issuedAt}` | `404` user không có | NT-01 (giả lập bấm nút từ LMS) |
| 2 | `POST /lms/sso/verify` | `{ticket}` | `200 {lmsUserId, email, fullName, role, issuedAt}` | `404` vé không có | NT-01 |
| 3 | `GET /lms/students/{lmsUserId}` | – | `200 {lmsUserId, fullName, email, program, classCode, courseName}` | `404` không có hoặc không phải CANDIDATE | HS-02 |
| 4 | `GET /lms/students/{lmsUserId}/results` | – | `200 [{subjectCode, subjectName, score, attendanceRate, homeworkRate, rpoint}]` | – | NT-05, HS-01 |
| 5 | `GET /lms/courses?skill=JAVA` | – | `200 [{courseId, name, skills[], level}]`; có `skill` thì chỉ trả khoá có mã đó | – | DT-02 |
| 6 | `POST /lms/enrollments` | `{lmsUserId, courseId}` | `201 {lmsUserId, courseId, progress: 0, score: null, status: "ENROLLED", enrolledAt}` | `404` user hoặc khoá không có; `409 ALREADY_ENROLLED` | DT-02 |
| 7 | `GET /lms/enrollments/{lmsUserId}` | – | `200` mảng ghi danh; `status` là `ENROLLED`, `IN_PROGRESS` hoặc `COMPLETED` | – | DT-02, DT-03 |
| 8 | `GET /exam/students/{lmsUserId}/results` | – | `200 [{examCode, subjectCode, score, takenAt}]` | – | NT-05, HS-01 |
| 9 | `GET /pm/students/{lmsUserId}/internships` | – | `200 [{batchId, batchName, groupName, topicName, mentorEmail, finalScore, tasks: [{title, acceptedScore, acceptedAt}]}]` | – | NT-05, TT-01 |
| 10 | `POST /pm/batches/{batchId}/members` | `{lmsUserId}` | `201 {batchId, lmsUserId, addedAt}` | `404` đợt hoặc user không có; `409 ALREADY_MEMBER` | TT-02 |
| 11 | `POST /meet/rooms` | `{title, startAt, endAt}` | `201 {roomId, roomUrl, title, startAt, endAt}` | `400` thiếu trường | UT-06 |

Bổ sung của nhóm (không trái hợp đồng):

- Endpoint 6, 10, 11 thiếu trường bắt buộc → `400 VALIDATION_ERROR`. Endpoint 11 còn trả 400 khi `startAt`/`endAt` không phải ISO 8601 hoặc `endAt <= startAt`.
- Endpoint 10 coi học viên đã có bản ghi thực tập của đợt trong `data.json` là đã là thành viên → `409 ALREADY_MEMBER`.
- Bản ghi thực tập trong `data.json` có thêm `startDate`, `endDate`.
- Đường dẫn không tồn tại → `404 ROUTE_NOT_FOUND`; body JSON hỏng → `400 INVALID_JSON`; lỗi không mong muốn → `500 INTERNAL_ERROR`.

### Ví dụ endpoint 2

```http
POST http://localhost:9000/lms/sso/verify
X-Service-Key: dev-key
Content-Type: application/json

{ "ticket": "9f2c4e1ab37d4c0e8a51b6d2f0c93e77" }
```

```json
{
  "lmsUserId": "lms-sv-001",
  "email": "candidate1@rikkeijobs.test",
  "fullName": "Nguyễn Văn An",
  "role": "CANDIDATE",
  "issuedAt": "2026-11-11T09:15:02.311Z"
}
```

Mock chỉ cấp vé và trả thông tin người dùng của vé. Mock **không** chặn vé cũ hay vé đã dùng: với vé đó nó vẫn trả `200`. Chặn vé đã dùng (`used_sso_tickets`) và vé quá 60 giây là việc của backend (NT-01).

## Quy ước giá trị dữ liệu

- `score`, `finalScore`, `acceptedScore`: thang 10. Khi đổi sang chỉ số kỹ năng 0–100 thì nhân 10 (HS-01, TT-01).
- `attendanceRate`, `homeworkRate`, `progress`: phần trăm, 0–100.
- Thời gian: ISO 8601 có múi giờ, ví dụ `2026-10-20T08:00:00+07:00`.
- `role`: `CANDIDATE`, `MENTOR`, `STAFF`, `ADMIN`. Tài khoản HR không có trong LMS (HR đăng nhập bằng email + mật khẩu, NT-01).
- `skills` của khoá học dùng mã kỹ năng chung của nhóm, xem [conventions](conventions.md#6-danh-mục-kỹ-năng-và-bậc-năng-lực).

## Công cụ test `/mock/*`

Không cần khoá, backend không bao giờ gọi tới.

| Endpoint | Việc nó làm | Ca kiểm thử dùng |
| --- | --- | --- |
| `POST /mock/reload` | Đọc lại `data.json` mà không tắt mock | NT-05, TT-01: sửa điểm trong mock rồi bấm Đồng bộ lại |
| `GET /mock/requests` | 100 request gần nhất: thời điểm, method, path, body, có khoá hay không | TT-02: mock nhận đúng `lmsUserId`; NT-05: backend có gửi khoá |
| `PATCH /mock/enrollments` | Sửa `progress`, `score`, `status` của một lượt ghi danh `{lmsUserId, courseId, ...}` | DT-02, DT-03: khoá học chuyển hoàn thành |
| `POST /mock/reset` | Xoá vé, ghi danh, thành viên đợt, phòng họp và nhật ký request trong bộ nhớ | Trước mỗi lần demo |

Ca "tắt mock" (NT-05, TT-02, DT-02): dừng tiến trình mock bằng `Ctrl + C`, không cần endpoint.

## TT-04: lms-pm gọi ngược vào backend

Script `mock/send-evaluation-links.js <batchId>` (hoặc `npm run evaluation-links -- <batchId>`):

1. Đọc các thực tập sinh của đợt trong `data.json`.
2. Với mỗi bạn, gọi `POST {BACKEND_URL}/api/v1/integrations/pm/evaluation-links` với body `{mentorEmail, lmsUserId, batchId}` kèm `X-Service-Key: dev-key`. `BACKEND_URL` mặc định `http://localhost:8080`.
3. In ra các link nhận được để mentor bấm thử.

## Backend gọi mock thế nào

- Cấu hình (không gõ cứng `localhost:9000` trong code): nhóm dùng `LMS_BASE_URL=http://localhost:9000`, `LMS_SERVICE_KEY=dev-key` (xem `.env.example`; tài liệu mentor gọi là `EXTERNAL_BASE_URL`, `EXTERNAL_SERVICE_KEY`). Khảo thí, lms-pm, Meet chạy chung địa chỉ mock nên dùng lại hai biến này.
- Một client dùng chung (ví dụ `ExternalApiClient`) tự gắn `X-Service-Key`, timeout 5 giây, thử lại 2 lần khi lỗi mạng. Các module NT-01, HS-02, NT-05, TT-01, TT-02, DT-02, UT-06 đều gọi qua client này.
- Đổi lỗi: mock trả 404 → lỗi nghiệp vụ tương ứng (vé sai → 401); mock tắt hoặc quá thời gian → backend trả `503 {"code":"EXTERNAL_UNAVAILABLE"}`. Không để lộ lỗi 500 ra ngoài.
- Không chép dữ liệu mock vào backend. Backend luôn lấy qua HTTP; dữ liệu lưu lại (ví dụ `external_records` của NT-05) là kết quả đồng bộ, không phải seed gõ tay.
