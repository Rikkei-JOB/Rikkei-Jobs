# Quy ước làm việc nhóm Rikkei Jobs

Tài liệu ghi quy ước và hợp đồng theo đề bài mentor. Fixture mock hiện nằm trong `mock/data.json`; seed tài khoản và danh mục backend chưa được triển khai. Trạng thái mã nguồn và lệnh chạy xem [README](../README.md) và [mock README](../mock/README.md).

## 1. Repository, phân công và Git

### Cấu trúc nhóm thống nhất

```text
server/      # Backend: API, nghiệp vụ, migration và seed
web/         # Frontend: giao diện và API client
mock/        # Express mock và fixture hệ thống ngoài
infra/       # Docker Compose, reverse proxy và monitoring
docs/        # Quy ước, API, ERD, kiến trúc, quyết định kỹ thuật
scripts/     # Script hỗ trợ phát triển
```

Nhóm giữ nguyên cấu trúc hiện tại, dùng `server/` cho backend, `web/` cho frontend và `mock/` cho mock hệ thống ngoài. Mock đã có luồng LMS SSO; các endpoint LMS khác, khảo thí, lms-pm và Meet còn là TODO. Đường dẫn trong build, CI, Docker, script và tài liệu phải khớp cấu trúc đã thống nhất.

Trưởng nhóm chia task có mã (ví dụ `NT-01`), người phụ trách, đầu ra và tiêu chí nghiệm thu. Cả nhóm cùng dựng và kiểm tra luồng chạy trong buổi đầu. Mỗi task cập nhật tài liệu hoặc hợp đồng API nếu có thay đổi liên quan.

### Nhánh và pull request

| Nhánh | Mục đích |
| --- | --- |
| `main` | Bản ổn định để bàn giao/demo |
| `test` | Nhánh tích hợp công việc của nhóm |
| `feature/<mã>-<tên>` | Một task, ví dụ `feature/NT-01-login` |

- Tạo feature branch từ `test` mới nhất; phần tên dùng chữ thường và dấu `-`, giữ nguyên mã task.
- Mọi PR của nhóm có **base là `test`**, không mở PR feature vào `main`.
- Tiêu đề bắt đầu bằng mã task trong ngoặc vuông: **`[NT-01] Thêm đăng nhập JWT`**.
- Mô tả PR gồm thay đổi, task liên quan, cách chạy/kiểm tra, thay đổi cấu hình hoặc migration nếu có.
- Có ít nhất một thành viên khác review, xử lý conflict và hoàn tất kiểm tra liên quan trước merge.
- Không push trực tiếp vào `main` hoặc `test`. Trưởng nhóm quản lý mốc phát hành `main` sau khi tích hợp và nghiệm thu trên `test`.
- Không commit `.env`, token hoặc secret thật. `dev-key` và tài khoản mẫu chỉ dùng cho dev/demo.

Ví dụ sau khi clone, working tree sạch và remote đã có `test`:

```sh
git fetch origin
git switch test
git pull --ff-only origin test
git switch -c feature/NT-01-login
```

## 2. Quy ước API nội bộ

### URL, JSON và xác thực

- Tiền tố API: **`/api/v1`**, ví dụ `GET /api/v1/candidates`.
- Request có body và response có body dùng JSON, `Content-Type: application/json`; client gửi `Accept: application/json`.
- Endpoint bảo vệ nhận **`Authorization: Bearer <JWT>`**. Endpoint đăng nhập/khởi tạo SSO công khai phải được ghi rõ trong đặc tả API.
- Backend kiểm tra chữ ký, thời hạn JWT, role và quyền trên tài nguyên; không tin role hoặc company ID do client tự khai báo.
- Field JSON dùng `camelCase`; enum role dùng chữ hoa như phần 3.
- Đây là hợp đồng mục tiêu; gateway và security hiện chưa triển khai nó.

Ví dụ request khi đã có JWT hợp lệ:

```http
GET /api/v1/candidates?page=1&size=20
Accept: application/json
Authorization: Bearer <JWT>
```

### Lỗi

Body lỗi có hai field bắt buộc `code` và `message`:

```json
{
  "code": "UNAUTHORIZED",
  "message": "Token không hợp lệ hoặc đã hết hạn."
}
```

`code` là mã ổn định cho client xử lý; `message` mô tả để người dùng hiểu. Không trả stack trace hoặc thông tin bí mật. Dùng đúng HTTP status, không trả `200` cho lỗi.

| HTTP | Code mẫu | Trường hợp |
| --- | --- | --- |
| `400` | `VALIDATION_ERROR` | JSON sai, thiếu field, giá trị hoặc phân trang không hợp lệ |
| `401` | `UNAUTHORIZED` | Thiếu/sai/hết hạn JWT; thiếu/sai service key trên mock |
| `403` | `FORBIDDEN` | Đã xác thực nhưng thiếu quyền hoặc truy cập sai phạm vi doanh nghiệp |
| `404` | `NOT_FOUND` | Không tìm thấy tài nguyên |
| `409` | `CONFLICT` | Dữ liệu trùng hoặc trạng thái nghiệp vụ xung đột |
| `429` | `RATE_LIMITED` | Vượt giới hạn request |

Lỗi hạ tầng ngoài dự kiến dùng HTTP `5xx` phù hợp, vẫn giữ `{code, message}`; không đổi mọi lỗi server thành `400`.

### Danh sách và phân trang

- Query: **`?page=1&size=20`**; `page` bắt đầu từ **1**, mặc định `1`; `size` mặc định `20`.
- Nhóm quy ước `size` tối đa `100`; giá trị không phải số nguyên, `page < 1`, `size < 1` hoặc `size > 100` trả `400`.
- `total` là tổng bản ghi sau lọc và trước phân trang; `page`, `size` phản ánh request đã áp dụng.
- Trang vượt tổng số trang vẫn trả `200`, `items: []`, giữ đúng `total`.
- Với Spring Data, chuyển `page - 1` ở tầng xử lý nội bộ, trả lại chỉ số trang bắt đầu từ 1.

```json
{
  "items": [{"id": "candidate-1", "fullName": "Nguyễn Văn An"}],
  "total": 3,
  "page": 1,
  "size": 20
}
```

Endpoint mock được đề bài chỉ định trả mảng giữ nguyên **mảng JSON**, không bọc theo phân trang API nội bộ.

### Thời gian

Dùng **ISO 8601 kèm múi giờ**, ưu tiên UTC: `2026-11-01T08:00:00Z`; chấp nhận offset như `2026-11-01T15:00:00+07:00`. Áp dụng cho `issuedAt`, `takenAt`, `acceptedAt`, `startAt`, `endAt` và timestamp khác. Không trả dạng `01/11/2026 15:00`. Giao diện chuyển sang giờ hiển thị phù hợp, ví dụ `Asia/Ho_Chi_Minh`.

## 3. Vai trò

| Giá trị | Tên | Phạm vi nghiệp vụ dự kiến |
| --- | --- | --- |
| `CANDIDATE` | Ứng viên | Hồ sơ, năng lực, học tập và ứng tuyển của mình |
| `HR` | Doanh nghiệp | JD, ứng viên và tuyển dụng trong doanh nghiệp được liên kết |
| `MENTOR` | Mentor | Ứng viên/nhóm được phân công, đánh giá theo nhiệm vụ |
| `STAFF` | Cán bộ tuyển dụng | Điều phối và hỗ trợ tuyển dụng theo phân quyền |
| `ADMIN` | Quản trị | Quản lý tài khoản, vai trò, danh mục và cấu hình |

Quyền chi tiết từng endpoint cần ghi trong đặc tả API; tên vai trò không thay thế kiểm tra quyền sở hữu tài nguyên. HR Công ty A không truy cập dữ liệu riêng của Công ty B.

## 4. Tài khoản mẫu và seed

Tất cả tài khoản demo dùng mật khẩu **`Test@123`**, email đuôi **`@rikkeijobs.test`**.

| Email | Role | Tên mẫu / liên kết | `lmsUserId` |
| --- | --- | --- | --- |
| `candidate1@rikkeijobs.test` | `CANDIDATE` | Nguyễn Văn An | `lms-sv-001` |
| `candidate2@rikkeijobs.test` | `CANDIDATE` | Trần Thị Bình | `lms-sv-002` |
| `candidate3@rikkeijobs.test` | `CANDIDATE` | Lê Minh Châu | `lms-sv-003` |
| `hr1@rikkeijobs.test` | `HR` | Công ty A | — |
| `hr2@rikkeijobs.test` | `HR` | Công ty B | — |
| `mentor1@rikkeijobs.test` | `MENTOR` | Phạm Quốc Dũng | `lms-mt-001` |
| `staff1@rikkeijobs.test` | `STAFF` | Hoàng Thu Hà | `lms-st-001` |
| `admin1@rikkeijobs.test` | `ADMIN` | Đỗ Thanh Hải | `lms-ad-001` |

ID LMS và tên đầy đủ khớp `mock/data.json`. `lmsUserId` dùng chuỗi như `"lms-sv-001"` trong JSON; không đồng nhất với ID tài khoản nội bộ. `email`, `fullName`, `role` từ SSO phải khớp fixture. HR không có định danh LMS. Các định danh mock không có mật khẩu và không thay thế seed tài khoản backend.

Seed phải tạo đủ hai doanh nghiệp, liên kết đúng HR, 8 tài khoản và kỹ năng. Chạy lại seed không tạo bản ghi trùng; lưu mật khẩu đã hash. Chỉ bật seed demo trong dev/test. **Repo chưa có seed hoặc lệnh chạy seed**; người triển khai cần bổ sung lệnh vào README.

## 5. Mock hệ thống ngoài

### Vận hành và xác thực

- Mã nguồn, fixture và hướng dẫn thuộc **`mock/`**, chạy độc lập tại **`http://localhost:9000`**.
- Mock hiện dùng Express; từ gốc repo chạy `cd mock`, `npm ci`, `npm start`. Cấu hình qua biến môi trường `PORT` (mặc định `9000`) và `SERVICE_KEY` (mặc định `dev-key`), không tự nạp `.env`.
- Endpoint nghiệp vụ `/lms`, `/exam`, `/pm`, `/meet` bắt buộc **`X-Service-Key: dev-key`**. Request có JSON hợp lệ mà thiếu/sai key trả `401 INVALID_SERVICE_KEY` cùng `{code, message}`. JSON hỏng trả `400 INVALID_JSON` trước kiểm tra key do thứ tự middleware hiện tại.
- Công cụ local `/mock/*` không yêu cầu key; backend không gọi các công cụ này.
- Backend giữ key trong cấu hình server; không nhúng vào biến frontend `NEXT_PUBLIC_*`.
- Đường dẫn giữ nguyên `/lms`, `/exam`, `/pm`, `/meet`, **không thêm `/api/v1`**.
- Body POST dùng JSON. Mock hiện có lỗi parse JSON `400`, key `401`, tài nguyên/route `404`, lỗi nội bộ `500`. Validation nghiệp vụ và lỗi xung đột `409` cho các endpoint TODO là hợp đồng mục tiêu, chưa được triển khai.
- Dữ liệu GET cho 3 ứng viên phải cố định và nhất quán. Khởi động lại mock đưa fixture về ban đầu; dữ liệu tạo từ POST có thể giữ trong bộ nhớ ở chế độ dev.
- Email cấu hình riêng bằng Mailpit/MailHog hoặc `email_outbox`, không bắt buộc giả lập SMTP qua HTTP cổng 9000.

**Trạng thái hiện tại:** đã có cấp/verify ticket SSO, `GET /mock/requests` và `POST /mock/reload`. Chín endpoint nghiệp vụ còn lại trong hợp đồng bên dưới chưa có handler, gọi với đúng key sẽ trả `404 ROUTE_NOT_FOUND`. `PATCH /mock/enrollments` và `POST /mock/reset` cũng chưa có. Reload chỉ nạp lại `data.json`, không xóa trạng thái trong bộ nhớ; cần khởi động lại tiến trình để xóa trạng thái.

Ví dụ thiếu/sai key:

```json
{"code": "INVALID_SERVICE_KEY", "message": "Thiếu hoặc sai X-Service-Key"}
```

### LMS

| Method | Path | JSON request | JSON response thành công |
| --- | --- | --- | --- |
| `POST` | `/lms/sso/tickets` | `{lmsUserId}` | `201 {ticket, issuedAt}` — đã có |
| `POST` | `/lms/sso/verify` | `{ticket}` | `200 {lmsUserId, email, fullName, role, issuedAt}` — đã có |
| `GET` | `/lms/students/{id}` | — | `{program, classCode, courseName}` |
| `GET` | `/lms/students/{id}/results` | — | `[{subjectCode, subjectName, score, attendanceRate, homeworkRate, rpoint}]` |
| `GET` | `/lms/courses` | — | `[{courseId, name, skills: [], level}]` |
| `POST` | `/lms/enrollments` | Nhóm quy ước `{lmsUserId, courseId}` | Nhóm quy ước `{id, lmsUserId, courseId, status}` |
| `GET` | `/lms/enrollments/{id}` | — | Nhóm quy ước `{id, lmsUserId, courseId, status}` |

Các dòng không ghi “đã có” là hợp đồng mục tiêu. `{id}` trong URL student là `lmsUserId`. Đề bài chưa quy định schema enrollment; tài liệu đề xuất `status: "ENROLLED"`, POST trả `201`, GET trả `200`, đăng ký trùng trả `409`, tra cứu theo ID lượt ghi danh. Trong mã mock hiện chỉ có mảng enrollment dự kiến chứa `{lmsUserId, courseId, progress, score, status, enrolledAt}`, chưa có `id` hoặc handler; cần thống nhất schema/ID khi triển khai, chưa thể kiểm tra POST → GET hiện nay.

Luồng SSO mô phỏng bấm nút từ LMS:

1. Gọi `POST /lms/sso/tickets` với `{"lmsUserId":"lms-sv-001"}` và service key để nhận ticket.
2. Truyền ticket vào luồng SSO của backend sau khi triển khai endpoint tương ứng.
3. Backend gọi `POST /lms/sso/verify` với ticket và service key, nhận định danh LMS.
4. Backend ánh xạ/tạo tài khoản theo chính sách SSO rồi phát JWT nội bộ. Ticket mock không phải JWT.

Cấp vé trả `201`, verify trả `200` khi hợp lệ; user/ticket không tồn tại trả `404 NOT_FOUND`. Mock cố ý cho phép verify lại cùng vé và không tự kiểm tra tuổi vé để kiểm tra backend xử lý SSO. Backend phải triển khai kiểm tra hết hạn/chống dùng lại theo chính sách xác thực; mock hiện không trả `409` cho vé đã dùng. `issuedAt` là thời điểm cấp vé thực tế theo ISO 8601.

Ví dụ verify (timestamp minh họa):

```json
{
  "lmsUserId": "lms-sv-001",
  "email": "candidate1@rikkeijobs.test",
  "fullName": "Nguyễn Văn An",
  "role": "CANDIDATE",
  "issuedAt": "2026-11-01T08:00:00Z"
}
```

### Khảo thí

`GET /exam/students/{id}/results` chưa có handler. Khi triển khai, trả mảng kết quả theo fixture, ví dụ candidate1:

```json
[
  {"examCode": "EX-JAVA-2026-10", "subjectCode": "JAVA01", "score": 8.0, "takenAt": "2026-10-20T08:00:00+07:00"}
]
```

### lms-pm

`GET /pm/students/{id}/internships` chưa có handler. Dữ liệu mục tiêu theo fixture candidate1 (có thể có thêm field như ngày bắt đầu/kết thúc):

```json
[
  {
    "batchId": "IOC-JB-2026-09",
    "batchName": "IOC Java Backend 09/2026",
    "groupName": "Nhóm 2",
    "topicName": "Hệ thống đặt lịch phòng họp",
    "mentorEmail": "mentor1@rikkeijobs.test",
    "finalScore": 8.5,
    "tasks": [
      {"title": "API đặt phòng và kiểm trùng lịch", "acceptedScore": 9, "acceptedAt": "2026-09-25T17:00:00+07:00"},
      {"title": "Phân quyền quản trị phòng", "acceptedScore": 8, "acceptedAt": "2026-10-09T17:00:00+07:00"},
      {"title": "Báo cáo tần suất sử dụng phòng", "acceptedScore": 8.5, "acceptedAt": "2026-10-23T17:00:00+07:00"}
    ]
  }
]
```

`POST /pm/batches/{id}/members` chưa có handler. Ví dụ mục tiêu: thêm `{"lmsUserId":"lms-sv-003"}` vào batch `IOC-JB-2026-12` có trong fixture. Đề xuất trả `201 {batchId, lmsUserId}`, thêm trùng trả `409`, batch/ứng viên không tồn tại trả `404`; cần hoàn thiện xử lý trước nghiệm thu.

### Rikkei Meet

`POST /meet/rooms` chưa có handler; hợp đồng request mục tiêu:

```json
{"title": "Phỏng vấn candidate1", "startAt": "2026-11-12T02:00:00Z", "endAt": "2026-11-12T03:00:00Z"}
```

Nhóm quy ước trả `201`:

```json
{"roomUrl": "https://meet.rikkeijobs.test/rooms/demo-001"}
```

Kiểm tra `title` không rỗng, timestamp hợp lệ và `endAt > startAt`, sai trả `400`. `roomUrl` là URL giả lập, không yêu cầu có phòng họp thật.

### Fixture cố định cho 3 ứng viên

Nguồn dữ liệu là [mock/data.json](../mock/data.json). Fixture đã có dữ liệu học tập, khảo thí và thực tập, nhưng các GET tương ứng chưa được triển khai. Ví dụ môn đầu tiên của mỗi ứng viên:

| `lmsUserId` | `program` | `classCode` | `courseName` | Môn học | `score` | `attendanceRate` | `homeworkRate` | `rpoint` |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `lms-sv-001` | Kỹ sư phần mềm | `KS25-CNTT1` | K25 | `JAVA01` / Lập trình Java | 8.5 | 95 | 90 | 120 |
| `lms-sv-002` | Kỹ sư phần mềm | `KS25-CNTT2` | K25 | `WEB01` / HTML/CSS/JavaScript | 9.0 | 98 | 95 | 110 |
| `lms-sv-003` | Kỹ sư phần mềm | `KS25-CNTT1` | K25 | `JAVA01` / Lập trình Java | 5.5 | 70 | 60 | 20 |

| `lmsUserId` | `examCode` | `subjectCode` | Điểm thi | `takenAt` | Thực tập |
| --- | --- | --- | --- | --- | --- |
| `lms-sv-001` | `EX-JAVA-2026-10` | `JAVA01` | 8.0 | `2026-10-20T08:00:00+07:00` | `IOC-JB-2026-09`, điểm cuối 8.5 |
| `lms-sv-002` | `EX-REACT-2026-10` | `REACT01` | 8.0 | `2026-10-22T08:00:00+07:00` | Mảng rỗng `[]` |
| `lms-sv-003` | `EX-JAVA-2026-10` | `JAVA01` | 4.5 | `2026-10-20T08:00:00+07:00` | Mảng rỗng `[]` |

Fixture có hai batch `IOC-JB-2026-09`, `IOC-JB-2026-12`. Candidate2/3 chưa có thực tập; mảng rỗng là dữ liệu hợp lệ, không tự tạo thực tập giả để lấp đầy.

Sáu khóa học có ID `LMS-C-JAVA-02`, `LMS-C-SQL-01`, `LMS-C-SPRING-01`, `LMS-C-REACT-01`, `LMS-C-GIT-01`, `LMS-C-DOCKER-01`. `skills` dùng mã như phần 6; `level` hiện là tên tiếng Việt `Cơ bản`/`Khá`, không phải mã `BASIC`/`FAIR`. Backend cần ánh xạ nếu dùng enum nội bộ.

Điểm học tập/thi/IOC trong fixture dùng thang **0–10**; khi quy đổi tuyến tính sang thang năng lực 100 thì nhân 10 trước khi phân bậc. `attendanceRate`, `homeworkRate` dùng phần trăm **0–100**. `rpoint` là điểm tích lũy riêng, có thể lớn hơn 100; không coi là phần trăm hoặc tự chặn ở 100. Công thức tổng hợp năng lực cần được triển khai riêng theo nghiệp vụ.

### Email

Nhóm chọn một cách, ghi lệnh khởi động/kiểm tra trong README:

- Mailpit/MailHog: backend gửi SMTP tới mock, mentor xem thư qua hộp thư local.
- Bảng `email_outbox`: lưu tối thiểu `id`, `toEmail`, `subject`, `body`, `status`, `createdAt`; có migration và cách truy vấn người nhận/nội dung.

Demo không gửi email thật. Repo chưa triển khai lựa chọn nào.

## 6. Danh mục kỹ năng và bậc năng lực

Seed khoảng 20 kỹ năng; nhóm thống nhất 20 mục sau, mã ổn định và không trùng:

| Mã | Tên kỹ năng |
| --- | --- |
| `JAVA` | Java |
| `SPRING_BOOT` | Spring Boot |
| `SQL` | SQL |
| `GIT` | Git |
| `REST_API` | REST API |
| `HTML_CSS` | HTML/CSS |
| `JAVASCRIPT` | JavaScript |
| `TYPESCRIPT` | TypeScript |
| `REACTJS` | ReactJS |
| `NEXTJS` | Next.js |
| `DOCKER` | Docker |
| `LINUX` | Linux |
| `OOP` | Lập trình hướng đối tượng |
| `DATA_STRUCTURES_ALGORITHMS` | Cấu trúc dữ liệu và giải thuật |
| `UNIT_TESTING` | Kiểm thử đơn vị |
| `CI_CD` | CI/CD |
| `ENGLISH` | Tiếng Anh |
| `COMMUNICATION` | Giao tiếp |
| `TEAMWORK` | Làm việc nhóm |
| `PROBLEM_SOLVING` | Giải quyết vấn đề |

Chuỗi bậc trong đề bài được hiểu là khoảng **0–39, 40–69, 70–84, 85–100**, bao gồm hai đầu mút, trên thang 100:

| Mã bậc | Tên | Điểm nguyên |
| --- | --- | --- |
| `BASIC` | Cơ bản | 0–39 |
| `FAIR` | Khá | 40–69 |
| `GOOD` | Tốt | 70–84 |
| `EXCELLENT` | Xuất sắc | 85–100 |

Nhóm quy ước điểm thập phân: `0 <= score < 40`, `40 <= score < 70`, `70 <= score < 85`, `85 <= score <= 100`; không làm tròn trước khi phân bậc. Điểm ngoài `0–100` bị từ chối; thiếu dữ liệu dùng `null`/chưa đánh giá, không tự gán 0. Hệ thống ngoài có thang điểm khác phải chuẩn hóa về 100 trước khi phân bậc.

## 7. Tiêu chí kiểm tra chung

- Chạy từ clone sạch theo README, không phụ thuộc file riêng trên máy thành viên.
- API giữ đúng `/api/v1`, JSON, JWT, role, phân trang và lỗi đã quy định.
- Đủ 8 tài khoản, HR thuộc đúng Công ty A/B, 3 ID LMS khớp fixture và 20 kỹ năng.
- Mock thiếu/sai key với JSON hợp lệ trả `401`; đúng key cần đọc được dữ liệu cho `lms-sv-001`, `lms-sv-002`, `lms-sv-003` trên LMS, khảo thí, lms-pm khi các endpoint TODO được hoàn thiện.
- SSO tạo ticket → verify trả đúng ứng viên; enrollment POST → GET đọc lại; thêm thành viên batch và tạo Meet room kiểm tra được.
- Timestamp có timezone; điểm biên `39/40`, `69/70`, `84/85`, `100` cho đúng bậc.
- Kiểm tra được email qua hộp thư local hoặc `email_outbox`.
- PR base `test`, tiêu đề bắt đầu `[<mã task>]`, có bằng chứng kiểm tra và review.
