# Profile service

HS-01 đã có lõi dữ liệu, mapping và recompute trong service nội bộ. Phần xác thực, quyền xem hồ sơ, route HTTP và FE sẽ ghép sau NT-01.

Từ gốc repo:

```sh
mvn -f server/pom.xml -pl services/profile-service -am clean test
```

Test dùng H2 trong bộ nhớ, không cần đăng nhập/mock hay database thật. SQL chuẩn bị nằm trong `src/main/resources/db/hs01/`, chưa được tự chạy bằng Flyway. Khi khởi chạy ứng dụng, `ddl-auto=validate` yêu cầu các bảng đã được tạo trên database phát triển riêng; không tự chạy SQL vào database chung.

Chi tiết đầu vào, công thức, ví dụ tính tay, dữ liệu ba ứng viên và danh sách việc chờ tích hợp: [HS-01 independent backend](../../../docs/hs01-independent-backend.md).
