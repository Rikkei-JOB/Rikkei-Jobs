# Rikkei Jobs – Mock hệ thống ngoài và API JD/CV

Server giả lập các hệ thống bên ngoài mà backend Rikkei Jobs cần gọi: **LMS**, **Khảo thí**, **lms-pm** và **Rikkei Meet**.
Mock là một tiến trình riêng, chạy độc lập với backend, để có thể tắt mock khi kiểm tra backend xử lý lỗi (NT-05, TT-02, DT-02). Cùng tiến trình còn cung cấp 3 endpoint JD/CV cho hợp đồng consumer UT-01; xem [hướng dẫn JD/CV](#mock-jdcv-cho-ut-01).

> Các API hệ thống ngoài (`/lms`, `/exam`, `/pm`, `/meet`) và JD nội bộ chỉ do **backend** gọi; khoá dịch vụ nằm ở backend. Riêng mock danh sách/PDF CV cho phép frontend dev gọi bằng JWT ứng viên, không cần service key.

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
| `RECRUITMENT_COMPANY_SERVICE_KEY` | fallback `INTERNAL_SERVICE_KEY`, rồi `dev-internal-key` | Giá trị `X-Internal-Key` của mock JD; tách biệt `SERVICE_KEY` |
| `JWT_SECRET` | không có | Khoá HMAC giống identity-service, ít nhất 32 byte; thiếu/sai cấu hình thì API CV trả 503 |
| `MOCK_WEB_ORIGIN` | `WEB_BASE_URL`, rồi `http://localhost:3000` | Origin frontend được phép gọi CV qua CORS, không có dấu `/` cuối |
| `MOCK_CV_OWNER_ID` | không có | UUID ứng viên thật sở hữu hai CV chính khi chạy local; dữ liệu fixture trên đĩa giữ UUID mẫu |

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

## Mock JD/CV cho UT-01

Mã route/xác thực ở `jd-cv.js`, dữ liệu trong `data.json` (`jobs`, `cvs`), PDF thật trong `fixtures/cvs/sample.pdf`. File PDF này không được public bằng static server. Hợp đồng máy đọc được: [ut01-dependencies-openapi.yaml](../docs/api/ut01-dependencies-openapi.yaml); ngữ cảnh tích hợp: [UT-01-applications.md](../docs/api/UT-01-applications.md#hợp-đồng-consumer-cho-các-task-phụ-thuộc). Đây là mock theo hợp đồng consumer đề xuất; DN-04/HS-04 vẫn cần review và xác nhận trước khi chốt.

| Method + đường dẫn | Xác thực | Kết quả |
| --- | --- | --- |
| `GET /internal/jobs/{jobId}/application-context` | `X-Internal-Key` | JSON context JD, không tăng job_views |
| `GET /jobs` | JWT CANDIDATE | Danh sách JD APPROVED, còn hạn và công ty hoạt động để hiển thị trang việc làm mẫu |
| `GET /jobs/{jobId}` | JWT CANDIDATE | Chi tiết JD mẫu, trạng thái nhận hồ sơ, chỉ trả thông tin ứng viên được xem |
| `GET /profile/cvs` | `Authorization: Bearer <JWT CANDIDATE>` | Mảng `{id,name,targetPosition}` thuộc `sub` |
| `GET /profile/cvs/{cvId}/pdf` | JWT CANDIDATE + đúng chủ CV | `application/pdf` riêng tư |

Hai route CV còn có alias `/api/v1/profile/cvs` và `/api/v1/profile/cvs/{cvId}/pdf` khi gọi trực tiếp mock trong dev. JD nội bộ không đi qua gateway. Namespace JD/CV không yêu cầu `X-Service-Key`; namespace hệ thống ngoài giữ nguyên guard cũ.

JWT phải có chữ ký HS256/HS384/HS512 phù hợp độ dài khoá, `sub` UUID và `exp` còn hạn; `nbf` nếu có phải hợp lệ. Token sai/hết hạn trả 401 `UNAUTHORIZED`, role khác CANDIDATE trả 403 `FORBIDDEN`, token `mustChangePassword=true` trả 403 `MUST_CHANGE_PASSWORD`. CV không tồn tại trả 404 `NOT_FOUND`, CV của người khác trả 403 `FORBIDDEN`, ID sai định dạng trả 400 `VALIDATION_ERROR`. Thiếu/sai internal key trả 401 `INVALID_INTERNAL_KEY`; không đọc được PDF fixture trả 503 `CV_STORAGE_UNAVAILABLE`. Mọi lỗi đều có `{code,message}`.

### Dữ liệu mẫu

| ID JD | Ca kiểm thử |
| --- | --- |
| `10000000-0000-0000-0000-000000000001` | APPROVED, còn hạn, công ty hoạt động |
| `10000000-0000-0000-0000-000000000002` | PENDING, chưa duyệt |
| `10000000-0000-0000-0000-000000000003` | APPROVED nhưng hết hạn |
| `10000000-0000-0000-0000-000000000004` | Công ty bị khoá |

Context của cả bốn JD trả 200 khi có key đúng để recruitment-service tự kiểm luật nhận đơn. Công ty A có ID `00000000-0000-0000-0000-0000000000a1`, khớp seed DN-01; email HR dùng miền dữ liệu thử `.test`.

| CV ID | ownerId mẫu |
| --- | --- |
| `40000000-0000-0000-0000-000000000001` (Backend) | `30000000-0000-0000-0000-000000000001` |
| `40000000-0000-0000-0000-000000000002` (Frontend) | `30000000-0000-0000-0000-000000000001` |
| `40000000-0000-0000-0000-000000000003` (CV khác) | `30000000-0000-0000-0000-000000000002` |

Ứng viên `30000000-0000-0000-0000-000000000003` không có CV. Các CV dùng cùng PDF mẫu tổng quát, không phải PDF cá nhân hoá hoặc output HS-04 thật. Muốn đổi dữ liệu, sửa `data.json` và gọi `POST /mock/reload`. `POST /mock/reset` chỉ xoá dữ liệu sinh ra trong bộ nhớ, không sửa fixture.

### Chạy riêng và thử hợp đồng

PowerShell, từ gốc repo:

```powershell
cd mock
npm.cmd ci
$env:JWT_SECRET = 'mock-jd-cv-local-secret-at-least-32-bytes'
$env:INTERNAL_SERVICE_KEY = 'dev-internal-key'
npm.cmd start
```

Terminal thứ hai, dùng cùng `JWT_SECRET` và tạo JWT ứng viên thử:

```powershell
cd mock
$env:JWT_SECRET = 'mock-jd-cv-local-secret-at-least-32-bytes'
npm.cmd run --silent token -- 30000000-0000-0000-0000-000000000001
```

Dán output vào biến `candidateToken` ở đầu `requests.http`. Token thử có hạn một giờ, chỉ phục vụ gọi mock; script không tạo tài khoản trong identity. Để thử CV của người khác, giữ token ứng viên đầu tiên rồi tải CV ID kết thúc `0003`.

### Nối với frontend và recruitment-service thật

1. Chạy identity/gateway theo NT-01. Đặt `JWT_SECRET` của mock **giống giá trị đang dùng bởi identity**, không dùng khoá ví dụ khác khi kiểm tra token đăng nhập thật.
2. Đăng nhập lấy `accessToken`; gọi `GET /api/v1/auth/me` để lấy UUID `id` thật. Với Docker, đặt `MOCK_CV_OWNER_ID=<UUID vừa lấy>` trong `.env` ở gốc repo. Chạy mock trực tiếp bằng PowerShell thì đặt `$env:MOCK_CV_OWNER_ID='<UUID vừa lấy>'` trước khi khởi động. Mock gắn hai CV chính với UUID này khi nạp dữ liệu. `candidate-token.js` cũng dùng biến này khi bỏ qua đối số userId. Các CV riêng khác có thể cấu hình `ownerId` trong `data.json` rồi `POST /mock/reload`.
3. Ở terminal chạy recruitment-service, bật cấu hình local sau, giữ PostgreSQL/storage/SMTP như hướng dẫn UT-01:

```powershell
$env:COMPANY_SERVICE_URL = 'http://localhost:9000'
$env:PROFILE_SERVICE_URL = 'http://localhost:9000'
$env:RECRUITMENT_COMPANY_SERVICE_KEY = 'dev-internal-key'
```

Các path mặc định của recruitment đã khớp mock, không cần sửa code client. Nếu đổi internal key, đặt cùng giá trị trong tiến trình mock.

4. Trong `web/.env.local`, giữ `NEXT_PUBLIC_API_URL` trỏ gateway thật và chỉ đổi nguồn danh sách CV:

```dotenv
NEXT_PUBLIC_CV_API_PATH=http://localhost:9000/profile/cvs
```

Để xem JD mẫu từ menu **Việc làm** và mở form ứng tuyển, thêm vào `web/.env.local`:

```dotenv
NEXT_PUBLIC_JOB_API_PATH=http://localhost:9000/jobs
NEXT_PUBLIC_JOB_PAGE_PATH=/candidate/jobs
```

Khởi động lại frontend sau khi đổi env. `MOCK_WEB_ORIGIN` trên mock phải bằng origin frontend (ví dụ `http://127.0.0.1:3000` nếu trình duyệt dùng địa chỉ đó). Khoá internal/service được cấu hình ở backend. Mở `/candidate/jobs` để xem JD mẫu, hoặc `/candidate/applications/new?jobId=10000000-0000-0000-0000-000000000001` để mở form ứng tuyển trực tiếp. Đơn đã nộp được lưu và theo dõi bởi recruitment-service thật. Hai API `/jobs` trả thông tin công khai của JD cho ứng viên; danh sách email HR chỉ có ở API nội bộ.

Với Docker Compose ở gốc repo, đặt `DOCKER_RECRUITMENT_COMPANY_URL=http://mock:9000` và `DOCKER_RECRUITMENT_PROFILE_URL=http://mock:9000` trong `.env`. Sau khi cấu hình `MOCK_CV_OWNER_ID` hoặc sửa fixture, chạy `docker compose up -d --build mock recruitment-service` để áp dụng cấu hình và dữ liệu mới.

### Kiểm thử tự động

```powershell
cd mock
npm.cmd test
```

Suite tự chạy mock ở các cổng tạm, kiểm tra HTTP thực cho JD/CV và regression 11 endpoint ngoài; không cần bật backend, PostgreSQL hoặc SMTP. CI có job riêng chạy suite này. Các test này xác minh mock/consumer contract, chưa thay thế nghiệm thu tích hợp với producer DN-04/HS-04 thật.
