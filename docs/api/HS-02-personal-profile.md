# HS-02 — Giai đoạn 1: Thông tin cá nhân

Giai đoạn này bổ sung hồ sơ cá nhân tại `/candidate/profile`, giữ nguyên khối
Skill Profile HS-01. Học vấn, chứng chỉ, upload và cập nhật kỹ năng tự khai thuộc
các giai đoạn tiếp theo.

## Chạy local

Chạy identity, profile, gateway, mock LMS và frontend theo README gốc. Cấu hình
Java không tự đọc `.env`; cần đưa các biến vào môi trường của tiến trình:

| Biến profile-service | Mặc định chạy Java local |
| --- | --- |
| `IDENTITY_SERVICE_URL` | `http://localhost:8081` |
| `INTERNAL_SERVICE_KEY` | `dev-internal-key`, phải khớp identity |
| `LMS_BASE_URL` | `http://localhost:9000` |
| `LMS_SERVICE_KEY` | `dev-key`, phải khớp mock |

Compose gốc đặt URL nội bộ cho profile-service; chạy `docker compose up -d --build`
để build lại code. Không cần bật seed kỹ năng để dùng thông tin cá nhân. Cần seed
identity ở môi trường dev để đăng nhập candidate1 bằng LMS mẫu. Frontend dùng
`NEXT_PUBLIC_API_URL=http://localhost:8080/api/v1`.

Đăng nhập candidate1 và mở `/candidate/profile`: lần đầu lấy Nguyễn Văn An,
`candidate1@rikkeijobs.test`, hệ `Kỹ sư phần mềm`, lớp `KS25-CNTT1`, khoá `K25`
từ `mock/data.json`. Đây là dữ liệu mẫu, không phải kết nối LMS thật.

## API

OpenAPI tĩnh: [hs02-openapi.yaml](hs02-openapi.yaml). OpenAPI sinh từ controller
được gộp tự động ở `/api/docs`, nhóm profile-service.

- `GET /api/v1/profiles/me`: JWT CANDIDATE; trả hồ sơ hiện có hoặc khởi tạo từ LMS.
- `PUT /api/v1/profiles/me`: JWT CANDIDATE; cập nhật thông tin, trả hồ sơ sau lưu.
- `GET /internal/users/{id}/lms-identity`: profile gọi trực tiếp identity bằng
  `X-Internal-Key`; trả `{userId,lmsUserId}` cho ứng viên ACTIVE có liên kết LMS.
  Không đi qua gateway, không đưa khoá vào frontend.

Ví dụ body PUT:

```json
{
  "fullName": "Nguyễn Văn An",
  "email": "an@example.com",
  "dob": "2000-02-29",
  "gender": "MALE",
  "phone": "0912345678",
  "address": "Hà Nội",
  "headline": "Lập trình viên Java",
  "summary": "Mong muốn phát triển ứng dụng backend."
}
```

Họ tên/email được điền lần đầu từ LMS và cho phép chỉnh sửa trong hồ sơ. Thao tác
này không đổi email đăng nhập, thông tin identity hoặc dữ liệu LMS nguồn. Hệ/lớp/
khoá có nhãn “Lấy từ LMS” và không chỉnh sửa được. PUT bỏ qua `program`,
`classCode`, `courseName`, `userId`, `visibility`, `avatarUrl` và các trường lạ;
chỉ lấy chủ sở hữu từ JWT. PUT thay thế các trường được phép: bỏ trống các trường
tuỳ chọn sẽ xoá giá trị tương ứng. Đọc và lưu lại hồ sơ sẽ không gọi lại LMS khi
hồ sơ đã tồn tại.

Họ tên và email bắt buộc, tối đa 255 ký tự. Ngày sinh phải hợp lệ, không ở tương
lai; gender MALE/FEMALE/OTHER hoặc null. Điện thoại không bắt buộc, nếu có phải
gồm 10 chữ số và bắt đầu bằng 0. Địa chỉ tối đa 500, headline 255, summary 5000
ký tự. Các chuỗi được cắt khoảng trắng hai đầu; chuỗi rỗng ở trường tuỳ chọn
được lưu null. Hồ sơ mới có visibility PRIVATE; giai đoạn này chưa cung cấp API
công khai hồ sơ.

Lỗi: 400 dữ liệu không hợp lệ; 401 thiếu/sai JWT; 403 sai vai trò; 404 thiếu liên
kết LMS hoặc không tìm thấy sinh viên; 502 dữ liệu dịch vụ nguồn sai/không đầy đủ;
503 không kết nối được nguồn hoặc lỗi cấu hình khoá. Không lưu hồ sơ thiếu một
phần khi nguồn lỗi. Người dùng có thể thử lại.

Migration mới `V5__candidate_profiles.sql` thêm bảng trong schema `profile`;
không thay V1–V4, không tạo FK sang bảng identity. Khoá theo user của HS-01
được dùng khi tạo hồ sơ lần đầu để tránh tạo trùng khi request đồng thời.

## Kiểm tra

```powershell
mvn -f server/pom.xml -pl services/identity-service,services/profile-service -am test
npm --prefix web run type-check
npm --prefix web run lint
# Tại web/, dùng Edge cài sẵn trên Windows:
$env:UT01_BROWSER_CHANNEL = 'msedge'
npm run test:e2e
```

Backend test dùng H2 với HTTP server giả lập ranh giới identity/LMS. Browser test
dùng API doubles để kiểm tra giao diện. Các bài test này không thay thế nghiệm
thu với LMS thật. Các kết quả chạy được ghi trong báo cáo bàn giao.

### Kết quả xác minh 2026-11-13

- Hồi quy common-core, gateway, identity và profile: 362 tests, 0 failures,
  0 errors; 1 bài kiểm tra read consistency PostgreSQL có sẵn được skip trong
  suite mặc định. Identity 245, profile 92, common-core 14, gateway 11.
- Riêng 8 test API HS-02 chạy lại trên PostgreSQL 18 ở database kiểm thử riêng:
  tất cả qua, gồm Flyway V1–V5, request đầu đồng thời và thử lại sau lỗi nguồn.
  Có thể chạy bằng `HS02_TEST_DB_URL`, `HS02_TEST_DB_USER`,
  `HS02_TEST_DB_PASSWORD`; chỉ trỏ vào database kiểm thử riêng.
- Frontend type-check và production build qua; lint 0 lỗi, 1 warning có sẵn ở
  `commitlint.config.mjs`. Playwright Edge: 13/13 qua (4 HS-02, 9 UT-01).
- Đã xem ảnh desktop 1440px và mobile 390px: không tràn ngang; Skill Profile
  HS-01 được giữ nguyên dưới form mới. Ảnh local nằm trong
  `web/test-results/hs02-desktop.png` và `hs02-mobile.png` (gitignored).
- Review độc lập chưa phát hiện lỗi chặn trong phạm vi giai đoạn 1.
- Chưa chạy lại stack Docker hay đăng nhập qua các service triển khai thật;
  backend test dùng HTTP doubles ở ranh giới identity/LMS và browser dùng API
  doubles. Chưa nghiệm thu LMS thật hoặc các chức năng của giai đoạn 2–3.
