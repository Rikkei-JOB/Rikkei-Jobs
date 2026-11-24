# HS-03 — Portfolio: dự án thực tập IOC và dự án cá nhân

Ứng viên trình bày dự án tại `/candidate/profile` (khối **Portfolio dự án**). Người xem
hồ sơ công khai thấy bản chỉ đọc tại `/profile/{userId}`.

## Chạy local

```powershell
$env:PROFILE_DEMO_DATA_ENABLED="true"; docker compose up -d --build
```

```powershell
cd web; npm run dev
```

Seed dự án IOC chỉ chạy khi profile-service ở profile `dev` **và**
`PROFILE_DEMO_DATA_ENABLED=true`. Seed cần dữ liệu identity dev, nên dùng chung database.
Frontend dùng `NEXT_PUBLIC_API_URL=http://localhost:8080/api/v1`. Đăng nhập **candidate1**
qua tab *Qua LMS*, rồi mở `/candidate/profile`.

## Dữ liệu mẫu

TT-01 chưa có, nên `ProfileDemoDataSeeder` tạo cho `lms-sv-001` (candidate1) một dự án IOC:

- Tên dự án: "Rikkei Jobs – Cổng tuyển dụng nội bộ (IOC mẫu)"
- `internshipId` cố định: `30000000-0000-0000-0000-000000000001`
- Kèm một minh chứng `EVIDENCE`

Seed idempotent: chạy lại không tạo bản trùng, vì có ràng buộc `UNIQUE(user_id, internship_id)`.
Khi TT-01 xong, TT-01 sẽ ghi dự án IOC theo cùng khoá này.

## Quy tắc

| Quy tắc | Hành vi |
| --- | --- |
| Tên dự án | Bắt buộc, tối đa 255 ký tự. |
| `repoUrl`, `demoUrl` | Chỉ URL `http(s)` có host. `abc`, `javascript:...`, `ftp://...` trả 400 `errors.repoUrl` hoặc `errors.demoUrl`. |
| Dự án IOC | `DELETE` trả 409 `IOC_PROJECT_LOCKED`. `PUT` chỉ đổi `role`, `description`, `visible`; các trường khác bị bỏ qua. |
| Ảnh | JPG/PNG theo magic bytes, ≤ 5MB (413 `FILE_TOO_LARGE`), tối đa 6 ảnh mỗi dự án (400 `TOO_MANY_IMAGES`). Dòng dự án được khoá khi đếm, nên hai lượt tải song song không vượt giới hạn. |
| Ảnh bìa | Ảnh IMAGE tải lên sớm nhất. |
| Thứ tự | `PUT /profiles/me/portfolio/order` nhận đúng toàn bộ id dự án; thiếu, thừa hoặc trùng trả 400. |
| Xem công khai | Mọi vai trò đã đăng nhập. Chỉ thấy dự án `visible` của hồ sơ PUBLIC; ngược lại trả 404. Ảnh cũng theo luật này. |

Ảnh cần JWT nên frontend đọc qua API thành blob, không gắn thẳng URL vào `<img src>`.
Khi xoá dự án, các dòng `portfolio_media` bị xoá theo (`ON DELETE CASCADE`). Tệp ảnh trên
đĩa được giữ lại để đối soát lưu trữ.

## API

OpenAPI: [hs03-openapi.yaml](hs03-openapi.yaml). Danh sách endpoint:

- `GET/POST /profiles/me/portfolio`
- `PUT /profiles/me/portfolio/order`
- `PUT/DELETE /profiles/me/portfolio/{id}`
- `POST /profiles/me/portfolio/{id}/media` (multipart `file`)
- `DELETE /profiles/me/portfolio/{id}/media/{mediaId}`
- `GET /profiles/{userId}/portfolio`
- `GET /profiles/{userId}/portfolio/media/{mediaId}`

DB: migration `V12__portfolio.sql`, gồm hai bảng `portfolio_projects` và `portfolio_media`.

## Kiểm thử

```powershell
mvn -f server/pom.xml -pl services/profile-service test "-Dtest=PortfolioApiTest,ProfileDemoDataSeederTest"
```

`PortfolioApiTest` phủ các tiêu chí nghiệm thu:

- Thêm dự án cá nhân có 2 ảnh, hiển thị và đọc được ảnh
- Xoá dự án IOC bị chặn
- `repoUrl` "abc" báo lỗi
- Ảnh thứ 7, ảnh > 5MB và tệp PDF bị từ chối
- Đổi thứ tự rồi đọc lại vẫn giữ thứ tự mới
- Người khác không xem được hồ sơ chưa công khai

## Ảnh màn hình

| [Lưới thẻ dự án (desktop)](screenshots/HS-03/portfolio-desktop.png) | [Form thêm dự án, xem trước 2 ảnh](screenshots/HS-03/form-preview.png) | [Mobile](screenshots/HS-03/portfolio-mobile.png) |
| --- | --- | --- |

Ảnh được chụp bởi `web/e2e/hs03-portfolio-live.spec.ts`. Test này chỉ chạy khi đặt
`HS03_LIVE=1`, `HS03_SCREENSHOT_DIR=../docs/api/screenshots/HS-03`, và cần stack dev đang
chạy. Gateway chỉ cho phép CORS từ `WEB_BASE_URL` (mặc định `http://localhost:3000`), nên
phải chạy test với `baseURL` trùng origin đó.
