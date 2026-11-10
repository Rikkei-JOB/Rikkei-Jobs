# NT-01 · Đăng nhập (SSO LMS + tài khoản doanh nghiệp)

| Mục | Giá trị |
| --- | --- |
| Mã task | T023082 |
| Sprint | Sprint 1 (ngày 8) |
| Service | `identity-service` (cổng 8081) qua `api-gateway` |
| Frontend | `web/src/features/auth` |
| Phụ thuộc | Mock LMS của task khung (`/lms/sso/verify`) |
| Task phụ thuộc vào NT-01 | DN-01 (tạo tài khoản HR trên bảng `users`) |

## 1. Mục tiêu

Cho 5 vai trò đăng nhập vào hệ thống:

- **Ứng viên, Mentor, Cán bộ tuyển dụng, Quản trị**: vào bằng vé SSO từ LMS (hiện dùng mock).
- **HR doanh nghiệp**: đăng nhập bằng email + mật khẩu do cán bộ cấp.

## 2. Vai trò và cách đăng nhập

| Vai trò | Mã `role` | Vào bằng | Làm gì |
| --- | --- | --- | --- |
| Ứng viên | `CANDIDATE` | Tài khoản LMS (SV, học viên, thực tập sinh IOC) | Hoàn thiện hồ sơ, CV, Portfolio; xem khoảng thiếu hụt; ứng tuyển; dùng AI Career Assistant |
| Doanh nghiệp (HR) | `HR` | Tài khoản riêng do cán bộ cấp | Đăng JD; tìm ứng viên theo kỹ năng; đặt nhu cầu thực tập; phỏng vấn; offer |
| Mentor | `MENTOR` | Tài khoản LMS, từ `lms-pm` | Đánh giá năng lực thực tập sinh, ứng viên sau phỏng vấn |
| Cán bộ tuyển dụng | `STAFF` | Tài khoản cán bộ LMS | Duyệt hồ sơ, JD; quản lý doanh nghiệp; mở liên hệ; theo dõi tuyển dụng |
| Quản trị | `ADMIN` | Tài khoản cán bộ LMS | Danh mục kỹ năng, trọng số, mẫu phiếu; sửa bằng chứng có lý do; nhật ký |

> Tên vai trò theo quy ước nhóm: `CANDIDATE`, `HR`, `MENTOR`, `STAFF`, `ADMIN`. Đổi tên ở `Role.java` (common-core) và CHECK `ck_users_role` trong migration.

### Nguồn vé SSO

| Ứng dụng LMS | Vai trò |
| --- | --- |
| `lms-student` | Ứng viên |
| `lms-admin` | Cán bộ, quản trị |
| `lms-pm` | Mentor |

Vé dùng một lần, hết hạn sau **60 giây** kể từ `issuedAt`.

### Ma trận quyền theo vai trò

Quyền kiểm ở **API**, không chỉ ẩn trên giao diện. Mỗi vai trò có test riêng.

| Việc | Ứng viên | Doanh nghiệp | Mentor | Cán bộ TD | Quản trị |
| --- | --- | --- | --- | --- | --- |
| Sửa hồ sơ, CV, Portfolio của mình | Có | – | – | – | – |
| Đồng ý / rút đồng ý công khai | Có | – | – | – | – |
| Duyệt hồ sơ, sửa phần tự khai | – | – | – | Có | Có |
| Đăng, sửa JD | – | Của mình | – | Có | Có |
| Duyệt JD | – | – | – | Có | Có |
| Tìm ứng viên đã công khai | – | Có | – | Có | Có |
| Ứng tuyển, rút ứng tuyển | Có | – | – | – | – |
| Phỏng vấn, offer | – | JD của mình | Ghi kết quả vòng được mời | Xem, hỗ trợ | Xem |
| Thấy email, điện thoại ứng viên | Của mình | Khi ứng viên ứng tuyển JD của họ hoặc được mở | – | Có | Có |
| Đặt nhu cầu thực tập | – | Có | – | Có | Có |
| Ghép ứng viên vào đợt IOC | – | – | – | Có | Có |
| Phiếu đánh giá năng lực | Tự đánh giá | – | Ứng viên được giao | Xem | Xem, sửa có lý do |
| Khoảng thiếu hụt, lộ trình bổ sung | Của mình | – | Của thực tập sinh mình | Xem | Xem |
| AI Career Assistant | Có | – | – | – | Cấu hình |
| Danh mục kỹ năng, trọng số, mẫu phiếu | – | – | – | – | Có |
| Nhật ký thao tác | – | – | – | Xem | Có |

NT-01 chỉ cung cấp nền (role trong JWT, `companyId`); từng quyền ở bảng trên do task của tính năng tương ứng cài đặt.

## 3. Cơ sở dữ liệu

Task này tạo bảng `users`; các task khác dùng chung. Migration Flyway đặt tại
`server/services/identity-service/src/main/resources/db/migration/`.

### `users`

| Cột | Kiểu gợi ý | Ghi chú |
| --- | --- | --- |
| `id` | UUID / BIGINT, PK | |
| `email` | VARCHAR, **UNIQUE**, NOT NULL | Lưu chữ thường |
| `password_hash` | VARCHAR, NULL | bcrypt/argon2. NULL với user SSO |
| `full_name` | VARCHAR, NOT NULL | |
| `role` | VARCHAR, NOT NULL | 5 vai trò ở mục 2 |
| `lms_user_id` | VARCHAR, NULL | Định danh bên LMS |
| `company_id` | UUID/BIGINT, NULL | Chỉ HR doanh nghiệp |
| `must_change_password` | BOOLEAN, NOT NULL, default `false` | `true` cho HR đăng nhập lần đầu |
| `failed_login_count` | INT, NOT NULL, default 0 | |
| `locked_until` | TIMESTAMPTZ, NULL | |
| `status` | VARCHAR, NOT NULL | `ACTIVE` \| `LOCKED` |
| `created_at` | TIMESTAMPTZ, NOT NULL | |

### `used_sso_tickets`

| Cột | Ghi chú |
| --- | --- |
| `ticket` | PK. Chống dùng lại vé |
| `used_at` | |

### `password_reset_tokens`

| Cột | Ghi chú |
| --- | --- |
| `id` | PK |
| `user_id` | FK → `users.id` (có index) |
| `token_hash` | Chỉ lưu **hash** của token (index/unique) |
| `expires_at` | now + 30 phút |
| `used_at` | NULL cho đến khi dùng |

## 4. API

Qua gateway mọi API nằm dưới **`/api/v1`** (quy ước nhóm, [conventions.md](../conventions.md)): trình duyệt gọi `/api/v1/auth/...`, gateway bỏ tiền tố `/api/v1` nên identity-service nhận `/auth/...`. Body thành công trả **thẳng**, không bọc. Mọi lỗi có đúng hai field bắt buộc `code`, `message` và (khi cần) `errors`:

```json
{ "code": "TICKET_EXPIRED", "message": "Vé SSO đã hết hạn" }
```

| `code` | HTTP | Khi nào |
| --- | --- | --- |
| `VALIDATION_ERROR` | 400 | Thiếu/sai định dạng trường, kể cả mật khẩu mới < 8 ký tự hoặc thiếu chữ/số (`errors.<trường>`) |
| `INVALID_OLD_PASSWORD` | 400 | Mật khẩu cũ sai khi đổi mật khẩu |
| `SAME_PASSWORD` | 400 | Mật khẩu mới trùng mật khẩu cũ |
| `RESET_TOKEN_INVALID` | 400 | Link đặt lại không tồn tại, đã dùng hoặc hết hạn |
| `UNAUTHORIZED` | 401 | Thiếu/sai/hết hạn JWT |
| `INVALID_CREDENTIALS` | 401 | Sai email/mật khẩu |
| `INVALID_TICKET` / `TICKET_EXPIRED` / `TICKET_ALREADY_USED` | 401 | Vé SSO không hợp lệ / quá 60 giây / đã dùng |
| `MUST_CHANGE_PASSWORD` | 403 | Chưa đổi mật khẩu bắt buộc |
| `ACCOUNT_LOCKED` | 429 | Đang bị khoá 15 phút do sai mật khẩu 5 lần (`errors.minutesRemaining`) |
| `LMS_UNAVAILABLE` | 502 | Không gọi được LMS/mock, hoặc sai `X-Service-Key` |

### 4.1 `POST /auth/sso`

Request: `{ "ticket": "..." }`

Luồng:

1. Gọi LMS (mock `http://localhost:9000`, header `X-Service-Key`) `POST /lms/sso/verify` `{ticket}` → `{lmsUserId, email, fullName, role, issuedAt}`. Mock trả `404 NOT_FOUND` khi không có vé (→ 401 `INVALID_TICKET`) và **không** chặn vé cũ/đã dùng, nên bước 2 và 3 do backend làm.
2. Từ chối **401** nếu vé không hợp lệ, `now - issuedAt > 60s`, hoặc đã có trong `used_sso_tickets`.
3. Ghi vé vào `used_sso_tickets` (INSERT trùng khoá chính → 401, để an toàn khi 2 request đồng thời).
4. Tìm user theo `lms_user_id`, rồi theo email; chưa có thì tạo với `role` mock trả về. Vé mang role `HR`, hoặc email trùng tài khoản doanh nghiệp / tài khoản đã gắn LMS khác → 401 (không cho chiếm tài khoản).
5. Phát JWT.

Response 200: `{ "accessToken": "...", "user": { ... } }`

### 4.2 `POST /auth/login`

Request: `{ "email", "password" }`

Response 200: `{ "accessToken", "user", "mustChangePassword" }`

Lỗi:

- 401 `INVALID_CREDENTIALS`: sai email hoặc mật khẩu (cùng một thông báo cho email không tồn tại). Lần sai thứ 5 vẫn trả 401 nhưng tài khoản bắt đầu bị khoá.
- 429 `ACCOUNT_LOCKED`: đang khoá, kể cả khi nhập đúng mật khẩu. Body có `errors.minutesRemaining` và `errors.lockedUntil`.

### 4.3 `POST /auth/change-password`

Cần đăng nhập. Request: `{ "oldPassword", "newPassword" }`. Thành công trả `200 {"message", "accessToken"}` và đặt `must_change_password = false`. FE phải thay token cũ bằng `accessToken` mới, vì token cũ còn claim `mustChangePassword` và bị các service khác từ chối.

### 4.4 `POST /auth/forgot-password`

Request: `{ "email" }`. **Luôn trả 200** (không lộ email có tồn tại hay không). Nếu có user, tạo token ngẫu nhiên, lưu hash, gửi email chứa link `<WEB_URL>/reset-password/<token>`, hạn 30 phút.

### 4.5 `POST /auth/reset-password`

Request: `{ "token", "newPassword" }`. Token hết hạn hoặc đã dùng → lỗi "hết hạn". Thành công: đặt `used_at`, cập nhật mật khẩu, reset `failed_login_count`, `locked_until`.

### 4.6 `POST /auth/dev/sso-ticket` (chỉ dev/demo)

**Mặc định tắt.** Chỉ có khi chạy identity-service với profile `dev` (hoặc đặt `LMS_DEV_TICKETS_ENABLED=true`), vì endpoint này cấp vé cho bất kỳ `lmsUserId` nào mà không cần xác thực. Request `{ "lmsUserId": "lms-sv-001" }` → `200 {ticket, issuedAt}`. Backend gọi `POST /lms/sso/tickets` của mock hộ trang đăng nhập, vì frontend không được giữ khoá dịch vụ và không gọi thẳng mock. Dùng cho tab "Qua LMS" ở môi trường dev; trên môi trường thật tắt endpoint này và dùng nút của LMS thật.

### 4.7 `GET /auth/me`

Trả thông tin user hiện tại (`id`, `email`, `fullName`, `role`, `companyId`, `mustChangePassword`).

## 5. Quy tắc nghiệp vụ

| Quy tắc | Chi tiết |
| --- | --- |
| Khoá tài khoản | Sai mật khẩu **5 lần liên tiếp** → khoá 15 phút, báo số phút còn lại. Khi đang khoá, nhập **đúng** mật khẩu vẫn bị từ chối |
| Đếm lại | Đăng nhập đúng → `failed_login_count = 0` |
| Đổi mật khẩu bắt buộc | `must_change_password = true` → mọi API ngoài `/auth/**` ở **mọi service** trả **403 `MUST_CHANGE_PASSWORD`** cho đến khi đổi. Token cấp cho tài khoản này mang claim `mustChangePassword: true`; `JwtAuthenticationFilter` của common-core chặn theo claim. Đổi mật khẩu xong, `/auth/change-password` trả **token mới** không có claim, FE thay token cũ. identity-service đọc thêm cờ từ DB mỗi request |
| Độ mạnh mật khẩu | ≥ 8 ký tự, có chữ và số |
| Lưu mật khẩu | bcrypt hoặc argon2; không lưu thô |
| JWT | Chứa `userId`, `role`, `companyId`; hạn **8 giờ** |
| Vé SSO | Dùng một lần; hết hạn 60 giây |
| Reset token | Hạn 30 phút, dùng một lần, chỉ lưu hash |

### Cấu hình

`JWT_EXPIRATION_MS=28800000` (8 giờ) đã được cập nhật trong `.env.example`. Biến của NT-01: `WEB_BASE_URL`, `LMS_BASE_URL` (mặc định `http://localhost:9000`), `LMS_SERVICE_KEY` (mặc định `dev-key`, khớp `SERVICE_KEY` của mock), `LMS_DEV_TICKETS_ENABLED`, `DEV_DATA_ENABLED` (cả hai mặc định `false`, profile `dev` bật lên), `MAIL_*`. `JWT_SECRET` phải ≥ 32 byte. Khoá dịch vụ chỉ nằm ở backend, không đặt vào biến `NEXT_PUBLIC_*`.

### Áp dụng ở các service khác

Mọi service dùng `JwtAuthenticationFilter` của common-core nên tự động: kiểm tra chữ ký và hạn JWT bằng chung `JWT_SECRET`, và chặn token có claim `mustChangePassword` (403). Mỗi service có test `tokenOfAccountThatMustChangePasswordIs403`.

## 6. Frontend

| Route | Mô tả |
| --- | --- |
| `/login` | 2 tab: **Qua LMS** \| **Doanh nghiệp** |
| `/sso-callback?ticket=` | Nhận vé, gọi `/auth/sso`, lưu token, chuyển trang |
| `/change-password` | Đổi mật khẩu (bị đưa tới khi `mustChangePassword`) |
| `/forgot-password` | Nhập email |
| `/reset-password/:token` | Đặt mật khẩu mới |

Sau đăng nhập, chuyển về trang chủ theo vai trò (ví dụ `CANDIDATE` → hồ sơ, `HR` → quản lý JD, `ADMIN`/`STAFF` → bảng điều khiển). Bảng ánh xạ cụ thể thống nhất với nhóm.

## 7. Sơ đồ luồng

```text
SSO:    LMS ──ticket──▶ /sso-callback ──▶ POST /auth/sso ──▶ mock /lms/sso/verify
                                              │ hợp lệ, chưa dùng, ≤ 60s
                                              ▼
                                  tạo/tìm user ──▶ JWT 8h ──▶ trang chủ theo vai trò

HR:     /login (tab Doanh nghiệp) ──▶ POST /auth/login
          ├─ sai ≥ 5 lần ──▶ khoá 15 phút
          ├─ must_change_password ──▶ /change-password
          └─ OK ──▶ JWT 8h ──▶ trang chủ
```

## 8. Cách chạy

Cần: Docker (Postgres), Java 21 + Maven, Node.js. Mở từng terminal từ gốc repo:

```bash
cp .env.example .env
docker compose --env-file .env -f infra/compose.yaml up -d postgres mailhog   # DB + hộp thư dev (UI: http://localhost:8025)
cd mock && npm ci && npm start                                   # mock LMS: http://localhost:9000
cd server && mvn -pl services/identity-service -am spring-boot:run -Dspring-boot.run.profiles=dev   # :8081, Flyway tạo bảng; profile dev bật seed + /auth/dev/sso-ticket
cd server && mvn -pl api-gateway -am spring-boot:run             # http://localhost:8080 (/api/v1/...)
cd web && cp .env.example .env.local && npm ci && npm run dev   # http://localhost:3000
cd server && mvn test                                            # toàn bộ test (H2, server giả lập mock, không cần Docker)
```

Ví dụ gọi qua gateway (mock phải đang chạy):

```bash
# 1. SSO: xin vé (dev) rồi đổi lấy token (phải trong 60 giây, dùng một lần)
curl -X POST localhost:8080/api/v1/auth/dev/sso-ticket -H 'Content-Type: application/json' -d '{"lmsUserId":"lms-sv-001"}'
curl -X POST localhost:8080/api/v1/auth/sso -H 'Content-Type: application/json' -d '{"ticket":"<vé>"}'
curl localhost:8080/api/v1/auth/me -H 'Authorization: Bearer <accessToken>'

# 2. HR doanh nghiệp
curl -X POST localhost:8080/api/v1/auth/login -H 'Content-Type: application/json' -d '{"email":"hr1@rikkeijobs.test","password":"Test@123"}'
```

Email đặt lại mật khẩu gửi qua SMTP tới MailHog (`localhost:1025`); xem thư ở http://localhost:8025. Không có SMTP: đặt `MAIL_ENABLED=false`, link đặt lại sẽ được in ra log của service. Khi mock tắt, `POST /auth/sso` trả 502 `LMS_UNAVAILABLE`.

## 9. Dữ liệu mẫu

Seed khi chạy với profile `dev` (hoặc `DEV_DATA_ENABLED=true`; mặc định tắt). Chạy lại không tạo trùng, khớp [conventions.md](../conventions.md#4-tài-khoản-mẫu-và-seed) và `mock/data.json`. Mật khẩu của HR là `Test@123`; tài khoản LMS không có mật khẩu, chỉ đăng nhập bằng SSO.

| Email | Vai trò | `lmsUserId` | Ghi chú |
| --- | --- | --- | --- |
| `candidate1@rikkeijobs.test` | `CANDIDATE` | `lms-sv-001` | Nguyễn Văn An |
| `candidate2@rikkeijobs.test` | `CANDIDATE` | `lms-sv-002` | Trần Thị Bình |
| `candidate3@rikkeijobs.test` | `CANDIDATE` | `lms-sv-003` | Lê Minh Châu |
| `mentor1@rikkeijobs.test` | `MENTOR` | `lms-mt-001` | Phạm Quốc Dũng |
| `staff1@rikkeijobs.test` | `STAFF` | `lms-st-001` | Hoàng Thu Hà |
| `admin1@rikkeijobs.test` | `ADMIN` | `lms-ad-001` | Đỗ Thanh Hải |
| `hr1@rikkeijobs.test` | `HR` | — | Công ty A |
| `hr2@rikkeijobs.test` | `HR` | — | Công ty B (khác `companyId` với hr1) |
| `hr-new@rikkeijobs.test` | `HR` | — | Công ty A, **`must_change_password = true`**, thêm để thử luồng đổi mật khẩu bắt buộc |

Không bật profile `dev` ở môi trường dùng chung hoặc production: khi đó không có tài khoản mật khẩu mẫu và không có endpoint cấp vé (test `DevFeaturesDefaultOffTest` kiểm tra điều này).

## 10. Kiểm thử và nghiệm thu

| # | Kịch bản | Kết quả mong đợi |
| --- | --- | --- |
| 1 | Xin vé `lms-sv-001` (tab "Qua LMS" hoặc `/auth/dev/sso-ticket`), mở `/sso-callback?ticket=<vé>` | Vào được; `GET /auth/me` trả `role = CANDIDATE` |
| 2 | Dùng lại cùng vé lần 2 | 401 |
| 3 | Vé quá 60 giây (mock không tự hết hạn; test tự động dùng server giả lập lùi `issuedAt`) | 401 `TICKET_EXPIRED` |
| 4 | `hr-new` đăng nhập lần đầu | Bị đưa sang trang đổi mật khẩu |
| 5 | `hr-new` gọi API khác trước khi đổi (ví dụ `GET /api/v1/profile/ping`) | 403 `MUST_CHANGE_PASSWORD` ở mọi service; sau khi đổi, token mới gọi được |
| 6 | Nhập sai mật khẩu `hr2` 5 lần | Lần 6 báo khoá 15 phút, kể cả khi nhập đúng |
| 7 | Đăng nhập đúng sau vài lần sai (<5) | Bộ đếm về 0 |
| 8 | Quên mật khẩu | MailHog nhận email chứa link `/reset-password/<token>`; mở link đặt mật khẩu mới rồi đăng nhập được |
| 9 | Đặt lại xong, mở lại link lần 2 | Báo hết hạn |
| 10 | `forgot-password` với email không tồn tại | Vẫn 200 |
| 11 | Mật khẩu mới < 8 ký tự hoặc thiếu chữ/số | Bị từ chối |
| 12 | Kiểm tra DB | `password_hash` là hash bcrypt/argon2, không phải mật khẩu thô |

## 11. Ảnh màn hình

Chụp tự động khi chạy kịch bản kiểm thử trình duyệt, lưu ở [screenshots/NT-01](screenshots/NT-01):

| Ảnh | Nội dung |
| --- | --- |
| `login-lms.png`, `login-hr.png` | Trang `/login`, tab Qua LMS (6 người dùng mock) và tab Doanh nghiệp |
| `role-home.png` | Đăng nhập SSO `candidate1` xong, về trang chủ vai trò |
| `hr-error.png`, `hr-locked.png` | Sai mật khẩu; bị khoá 15 phút sau 5 lần sai |
| `change-password.png` | Lần đăng nhập đầu của `hr-new`, bắt đổi mật khẩu |
| `forgot-password-sent.png`, `reset-password.png`, `reset-expired.png` | Quên mật khẩu, đặt lại từ link trong email, link hết hạn |

## 12. Checklist nộp PR

- [ ] PR vào `test` (quy ước nhóm), tiêu đề bắt đầu bằng `[NT-01]`, nhánh `feature/<mã>-<tên>`, commit có mã `T023082`
- [x] Có: cách chạy, dữ liệu mẫu, ảnh màn hình
- [x] OpenAPI (`docs/api/identity-openapi.yaml`) cập nhật 6 endpoint + endpoint dev
- [ ] Một bạn trong nhóm review trước, mentor duyệt PR
