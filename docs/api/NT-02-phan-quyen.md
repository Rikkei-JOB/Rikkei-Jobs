# NT-02 · Khung phân quyền dùng chung và tài liệu API

| Mục | Giá trị |
| --- | --- |
| Mã task | T0230EB |
| Sprint | Sprint 2 (ngày 12), hạn 17/11/2026 |
| Nhánh | `feature/t0230eb-JOBS-NT-02-khung-phan-quyen` |
| Backend | `common-core` (guard dùng chung, OpenAPI), `company-service` (bảng `companies`, endpoint mẫu), `api-gateway` (Swagger UI) |
| Frontend | `web/src/app/(pages)/(app)`, `web/src/config/menu.ts`, `web/src/hooks/use-auth-guard.ts` |
| Phụ thuộc | NT-01: bảng `users`, JWT có `userId`, `role`, `companyId` |
| Task phụ thuộc vào NT-02 | Mọi task có API hoặc trang theo vai trò |

## 1. Mục tiêu

Dựng khung phân quyền và tài liệu API dùng chung để cả nhóm làm song song:

- **Backend:** khai báo vai trò được gọi ở từng API. Kiểm quyền sở hữu theo `company_id` hoặc theo chủ dữ liệu.
- **Frontend:** layout chung có menu theo vai trò, chặn route theo vai trò.
- **Tài liệu:** Swagger UI tại `/api/docs` gồm mọi endpoint đã merge, cùng sơ đồ ERD.

## 2. Tóm tắt cho người làm task khác

| Việc | Cách làm |
| --- | --- |
| Khai báo vai trò cho API | `@PreAuthorize(Roles.HR)`, `@PreAuthorize(Roles.HR_STAFF_ADMIN)`… trên method controller |
| HR chỉ thao tác dữ liệu công ty mình | `OwnershipGuard.requireCompany(principal, resource.getCompanyId(), "Không tìm thấy …")` → 404 |
| Ứng viên chỉ thao tác dữ liệu của mình | `OwnershipGuard.requireOwner(principal, resource.getOwnerId(), "Không tìm thấy …")` → 404 |
| Ghi tài liệu API | Thêm `@Operation`, `@ApiResponse` (mẫu: `CompanyController`). Tài liệu tự hiện ở `/api/docs` |
| Thêm bảng | Migration `V00n__...` trong schema của service, cập nhật [ERD](../database/erd.md) |
| Thêm trang frontend | Tạo file đúng `href` của mục menu, ví dụ `web/src/app/(pages)/(app)/hr/jobs/page.tsx`. Layout đã kiểm đăng nhập và vai trò |

## 3. Cơ sở dữ liệu

Chi tiết và sơ đồ: [docs/database/erd.md](../database/erd.md).

- **Mỗi service một schema** trong database chung `rikkei_jobs`. Bảng lịch sử Flyway nằm trong schema đó, nên các service migrate độc lập, khởi động theo thứ tự nào cũng được. `identity-service` đã merge trước quy ước này nên giữ schema `public`.
- **Migration** đánh số `V001__...`, `V002__...`. Không sửa migration đã merge; muốn đổi thì thêm migration mới. `V1__identity_init.sql` (NT-01) tương đương `V001`, nên migration tiếp theo của identity là `V002__`.
- **`companies`** (company-service, schema `company`, `V001__create_companies.sql`): bản tối thiểu `id`, `name`, `created_at`. DN-01 thêm cột bằng migration mới.
- `users.company_id` tham chiếu `companies.id` ở mức **logic, không có khoá ngoại vật lý**, vì hai bảng thuộc hai service.

### Seed

Chạy với profile `dev`, chạy lại không tạo trùng.

| Service | Dữ liệu |
| --- | --- |
| identity-service | 8 tài khoản theo [conventions.md §4](../conventions.md#4-tài-khoản-mẫu-và-seed) + `hr-new` (HR Công ty A phải đổi mật khẩu, phục vụ test NT-01) |
| company-service | Công ty A `00000000-0000-0000-0000-0000000000a1` (hr1, hr-new), Công ty B `00000000-0000-0000-0000-0000000000b2` (hr2) |

## 4. Backend

### 4.1 Guard theo vai trò

Dùng method security của Spring (bật sẵn trong `SecurityConfig` của common-core) với các hằng trong `Roles`:

```java
@GetMapping("/{id}")
@PreAuthorize(Roles.HR_STAFF_ADMIN)
public CompanyResponse getCompany(@AuthenticationPrincipal AuthPrincipal principal, @PathVariable UUID id) { ... }
```

| Hằng | Biểu thức |
| --- | --- |
| `Roles.CANDIDATE`, `HR`, `MENTOR`, `STAFF`, `ADMIN` | `hasRole('<vai trò>')` |
| `Roles.STAFF_OR_ADMIN` | `hasAnyRole('STAFF','ADMIN')` |
| `Roles.HR_STAFF_ADMIN` | `hasAnyRole('HR','STAFF','ADMIN')` |
| `Roles.AUTHENTICATED` | `isAuthenticated()` |

Tổ hợp khác viết trực tiếp, ví dụ `@PreAuthorize("hasAnyRole('MENTOR','ADMIN')")`. Mọi API có dữ liệu nghiệp vụ phải khai báo vai trò; review PR kiểm điểm này.

### 4.2 Kiểm quyền sở hữu: `OwnershipGuard`

| Vai trò | Được thao tác |
| --- | --- |
| `HR` | Dữ liệu có `company_id` trùng `companyId` trong JWT. HR không có `companyId` không thấy gì |
| `CANDIDATE` (và người dùng nói chung) | Dữ liệu có chủ là `userId` trong JWT |
| `STAFF`, `ADMIN` | Tất cả, trừ khi task tính năng quy định hẹp hơn |
| `MENTOR` | Theo task tính năng (ví dụ thực tập sinh được giao) |

Quy tắc:

1. Lấy `companyId`, `userId` **từ JWT** (`AuthPrincipal`), không lấy từ body, query hay header client gửi.
2. Dữ liệu của người khác trả **404 `NOT_FOUND`**, body giống hệt khi id không tồn tại, để không dò được id có thật. Không trả 403.
3. Khi được, lọc ngay trong truy vấn (ví dụ `findByIdAndCompanyId`) thay vì đọc lên rồi mới so.

### 4.3 Phân biệt 401, 403, 404

| Tình huống | Kết quả |
| --- | --- |
| Thiếu hoặc sai token | 401 `UNAUTHORIZED` |
| Sai vai trò cho cả API (CANDIDATE gọi API của HR) | 403 `FORBIDDEN` "Bạn không có quyền thực hiện thao tác này." |
| Tài khoản phải đổi mật khẩu | 403 `MUST_CHANGE_PASSWORD` (NT-01) |
| Đúng vai trò nhưng dữ liệu không thuộc mình (hr1 đọc Công ty B) | 404 `NOT_FOUND` |

### 4.4 Endpoint mẫu: `GET /api/v1/company/companies/{id}`

| Vai trò | Kết quả |
| --- | --- |
| `HR` | 200 với công ty của mình; công ty khác hoặc id không tồn tại → 404 cùng một body |
| `STAFF`, `ADMIN` | 200 với mọi công ty; id không tồn tại → 404 |
| `CANDIDATE`, `MENTOR` | 403 |

DN-01 phát triển tiếp các API doanh nghiệp trong `CompanyController`.

### 4.5 OpenAPI và Swagger UI

- `common-core` kèm springdoc: mỗi service tự sinh tài liệu tại `/<khu>/api-docs` (đặt bằng `springdoc.api-docs.path` trong `application.yml`). Đường dẫn này không cần JWT.
- Tài liệu khai báo server `/api/v1` và cơ chế `bearerAuth` cho mọi endpoint.
- api-gateway phục vụ **Swagger UI tại `http://localhost:8080/api/docs`**, có ô chọn 8 service, đọc tài liệu qua route sẵn có (`/api/v1/<khu>/api-docs`). Nút **Authorize** nhận `accessToken`, và được nhớ khi tải lại trang.
- Thêm service mới: đặt `springdoc.api-docs.path` cho service và thêm một dòng vào `springdoc.swagger-ui.urls` của gateway.
- `docs/api/identity-openapi.yaml` (viết tay ở NT-01) giữ làm hợp đồng tham khảo. Nguồn chính là tài liệu sinh từ code.

## 5. Frontend

### 5.1 Menu theo vai trò

Khai báo tại `web/src/config/menu.ts` (`ROLE_MENU`):

| Vai trò | Menu |
| --- | --- |
| `CANDIDATE` | Hồ sơ `/candidate/profile`, Việc làm `/candidate/jobs`, Ứng tuyển `/candidate/applications`, Lộ trình `/candidate/roadmap`, Trợ lý AI `/candidate/assistant` |
| `HR` | Công ty `/hr/company`, JD `/hr/jobs`, Tuyển dụng `/hr/recruitment`, Thực tập sinh `/hr/interns` |
| `MENTOR` | Cần đánh giá `/mentor/reviews` |
| `STAFF` | Duyệt hồ sơ `/staff/profiles`, Duyệt JD `/staff/jobs`, Doanh nghiệp `/staff/companies`, Ghép thực tập `/staff/internships`, Báo cáo `/staff/reports` |
| `ADMIN` | Quản trị `/admin/settings` |

- Layout chung (`app/components/layout/app-shell.tsx`): thanh trên có tên, vai trò, nút đăng xuất (HR có thêm nút đổi mật khẩu); menu bên trái. Trên điện thoại, menu thu gọn sau nút ☰ và tự đóng khi chuyển trang.
- Trang chủ vai trò (`/candidate`, `/hr`…) hiện lối tắt tới các mục menu.
- Mục chưa có trang thật hiển thị "Đang phát triển" (`[role]/[...section]/page.tsx`). Đường dẫn không có trong menu → 404.

### 5.2 Route guard

Layout `app/(pages)/(app)/layout.tsx` dùng `useRoleAccess`. Đoạn đầu của đường dẫn (`/hr/...`) quyết định vai trò được vào.

| Tình huống | Kết quả |
| --- | --- |
| Chưa đăng nhập | Nhớ trang đang mở, chuyển `/login`. Đăng nhập xong quay lại trang đó nếu thuộc vai trò của mình, ngược lại về trang chủ |
| Phải đổi mật khẩu | Chuyển `/change-password` |
| Mở trang của vai trò khác (candidate gõ `/hr/jobs`) | **Trang 403** ngay tại URL đó, kèm nút "Về trang chủ của tôi" |

Guard ở frontend chỉ để trải nghiệm tốt hơn; bảo vệ thật nằm ở API.

### 5.3 Gọi API (`services/api.ts`, có từ NT-01)

- Tự gắn `Authorization: Bearer <token>`.
- Gặp 401 (trừ các API đăng nhập) → xoá phiên, về `/login`.
- Gặp 403 `MUST_CHANGE_PASSWORD` → `/change-password`.
- 403 `FORBIDDEN`, 404 trả về cho trang tự hiển thị (`getErrorMessage`), không đăng xuất.

## 6. Cách chạy

```bash
docker compose --env-file .env -f infra/compose.yaml up -d postgres mailhog
cd mock && npm ci && npm start
cd server && mvn -pl services/identity-service -am spring-boot:run -Dspring-boot.run.profiles=dev
cd server && mvn -pl services/company-service -am spring-boot:run -Dspring-boot.run.profiles=dev
cd server && mvn -pl api-gateway -am spring-boot:run          # http://localhost:8080, Swagger UI: /api/docs
cd web && cp .env.example .env.local && npm ci && npm run dev  # http://localhost:3000/login
```

Gọi thử qua gateway:

```bash
TOKEN=$(curl -s -X POST localhost:8080/api/v1/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"hr1@rikkeijobs.test","password":"Test@123"}' | jq -r .accessToken)
curl -H "Authorization: Bearer $TOKEN" localhost:8080/api/v1/company/companies/00000000-0000-0000-0000-0000000000a1   # 200
curl -H "Authorization: Bearer $TOKEN" localhost:8080/api/v1/company/companies/00000000-0000-0000-0000-0000000000b2   # 404
```

### Test phân quyền

```bash
cd server && mvn -pl libs/common-core,services/company-service -am test \
  -Dtest='RoleGuardTest*,CompanyAuthorizationTest' -Dsurefire.failIfNoSpecifiedTests=false
```

| Test | Nội dung |
| --- | --- |
| `RoleGuardTest` (common-core, 14 ca) | Controller mẫu: 5 vai trò, mỗi vai trò ít nhất 1 ca được phép + 1 ca bị chặn 403; HR khác công ty và ứng viên đọc dữ liệu người khác → 404; không token → 401 |
| `CompanyAuthorizationTest` (company-service, 9 ca) | Trên endpoint thật và H2 chạy Flyway + seed: candidate/mentor → 403, hr1 đọc B → 404 giống id không tồn tại, STAFF/ADMIN đọc mọi công ty, `/company/api-docs` công khai và có endpoint |
| `SwaggerUiTest` (api-gateway) | `/api/docs` mở được, liệt kê đủ 8 service |

## 7. Kiểm thử và nghiệm thu

| # | Tiêu chí | Cách kiểm | Kết quả |
| --- | --- | --- | --- |
| 1 | `/api/docs` mở được, có endpoint auth và endpoint của module đã merge | Mở `http://localhost:8080/api/docs` | Có 8 service; identity có 7 endpoint `/auth/*`; company có `/company/companies/{id}` |
| 2 | Token candidate gọi API của HR → 403 | `GET /api/v1/company/companies/<A>` bằng token candidate1 | 403 `FORBIDDEN` |
| 3 | hr1 đọc dữ liệu công ty B → 404 | `GET /api/v1/company/companies/<B>` bằng token hr1 | 404 `NOT_FOUND`, giống hệt id không tồn tại |
| 4 | Migration từ DB trống không lỗi, seed đủ 8 tài khoản | Chạy service trên database mới tạo | identity `v1`, company `v001` đều thành công; 8 tài khoản + `hr-new`, 2 công ty; chạy lại không tạo trùng |
| 5 | Đăng nhập từng vai trò thấy đúng menu | Đăng nhập 5 tài khoản mẫu | Đúng bảng 5.1 |
| 6 | Gõ URL trang của vai trò khác → 403 | candidate gõ `/hr/jobs`, `/admin`; hr1 gõ `/candidate/profile`, `/staff/companies`… | Trang 403 |
| 7 | Test phân quyền pass bằng lệnh trong README | Lệnh ở mục 6 | Đạt |

Ngoài ra đã chạy kịch bản trình duyệt (Playwright): chưa đăng nhập → `/login` rồi quay lại đúng trang; token hỏng → 401 → `/login`; menu trên điện thoại; không có lỗi JS.

## 8. Ảnh màn hình

Lưu ở [screenshots/NT-02](screenshots/NT-02):

| Ảnh | Nội dung |
| --- | --- |
| `menu-candidate.png`, `menu-hr.png`, `menu-mentor.png`, `menu-staff.png`, `menu-admin.png` | Layout và menu của từng vai trò |
| `menu-mobile.png` | Menu trên điện thoại |
| `forbidden.png` | Candidate gõ `/hr/jobs` → trang 403 |
| `api-docs.png` | Swagger UI tại `/api/docs`, endpoint `/company/companies/{id}` |

## 9. Lưu ý

| Vấn đề | Cách xử lý |
| --- | --- |
| Đề bài ghi PR vào `develop`, [conventions.md](../conventions.md) quy định base `test` | Làm theo quy ước nhóm: PR vào `test`. Nếu mentor yêu cầu `develop` thì đổi base khi mở PR |
| Tiêu chí ghi "seed 8 tài khoản", seed có thêm `hr-new` | Giữ `hr-new` (cần cho test NT-01); 8 tài khoản theo quy ước vẫn đủ |
| CI (`.github/workflows/ci.yml`) chỉ chạy cho `main`, `develop` | Cần thống nhất với nhóm có thêm `test` hay không |

## 10. Checklist nộp PR

- [ ] Tiêu đề bắt đầu bằng `[NT-02]`, commit có mã `T0230EB`
- [x] Mô tả có: cách chạy, dữ liệu mẫu, ảnh màn hình
- [x] `/api/docs` có endpoint mới; ERD cập nhật
- [x] Lệnh chạy test phân quyền có trong README
- [ ] Một bạn trong nhóm review trước, mentor duyệt PR
