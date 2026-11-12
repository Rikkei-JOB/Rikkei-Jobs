# Profile service · backend HS-01

Đã nối NT-01 JWT, quyền đọc hồ sơ, Flyway và recompute từ dữ liệu nguồn trong database. Đợt này chỉ làm backend; frontend HS-01 chưa được nghiệm thu.

API qua gateway `http://localhost:8080/api/v1`:

| Method | Path | Quyền |
| --- | --- | --- |
| GET | `/profiles/me/skills` | CANDIDATE; UUID từ JWT |
| GET | `/profiles/{userId}/skills` | Có JWT; chủ sở hữu hoặc hồ sơ có cả đồng ý công khai và duyệt |
| POST | `/profiles/me/skills/recompute` | CANDIDATE; không body; đọc nguồn đã lưu của chính mình |

Trả mảng kỹ năng kèm điểm xác thực, điểm tự khai riêng, bậc và bằng chứng có ngày/nguồn. Người khác đọc hồ sơ riêng hoặc không tồn tại nhận cùng `404 NOT_FOUND`. `/profile/ping` cũ giữ nguyên.

Flyway tự quản lý các bảng trong schema `profile`, lịch sử `profile.flyway_schema_history`; `public.users` do identity-service quản lý. `PROFILE_DB_URL/USER/PASSWORD` có thể đặt riêng, mặc định dùng `POSTGRES_*` như identity. JWT giữa các service phải cùng `JWT_SECRET`.

Từ gốc repo, Java 21 và Maven:

```powershell
mvn -f server/pom.xml clean verify
```

Các test mặc định dùng H2 trong bộ nhớ. Test tích hợp API/nguồn có thể nhận `HS01_TEST_DB_URL`, `HS01_TEST_DB_USER`, `HS01_TEST_DB_PASSWORD` để chạy trên PostgreSQL thử nghiệm riêng.

Demo chỉ bật khi **cùng có** `SPRING_PROFILES_ACTIVE=dev` và `PROFILE_DEMO_DATA_ENABLED=true`. Chạy identity ở chế độ dev trước để có tài khoản; hai service dùng cùng database khi seed. Seeder tra `public.users.lms_user_id` lấy UUID thật, không tạo user hay công khai hồ sơ. Candidate1 có thêm ENGLISH tự khai 65, không có điểm xác thực.

Hướng dẫn đầy đủ gồm build/install, mock, identity, profile, gateway, bảng nguồn và công thức: [HS-01 backend integration](../../../docs/hs01-independent-backend.md).

Đặc tả: [OpenAPI](../../../docs/api/hs01-openapi.yaml). Sau khi khởi động các service và seed demo:

```powershell
postman collection run postman/hs01.postman_collection.json
```

[Collection](../../../postman/hs01.postman_collection.json) tự SSO ba ứng viên, kiểm tra đọc/recompute/cách ly/401/404; không sửa trạng thái công khai và không có endpoint import bằng chứng từ client.
