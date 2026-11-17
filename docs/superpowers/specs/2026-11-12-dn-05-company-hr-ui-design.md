# DN-05 · Giao diện doanh nghiệp: hồ sơ công ty, tài khoản HR — Thiết kế

Task: **[Doanh nghiệp & JD · DN-05] Giao diện doanh nghiệp: hồ sơ công ty, tài khoản HR**. Phụ thuộc DN-01 (đã merge vào `test`), dùng khung NT-02 (menu, route guard, springdoc). Nhánh `feature/DN-05-company-hr-ui` từ `test`, PR vào **`test`** (đề ghi "develop" là ghi nhầm, mentor yêu cầu nhánh `test`).

Ký hiệu: **(chốt)** là điều người giao task đã quyết; **(người thiết kế chọn)** là quyết định trong lúc thiết kế, người làm có thể nêu lại nếu thấy sai.

## 1. Mục đích

Giao diện hoàn chỉnh cho hai vai trò:

- **HR:** trang "Công ty của tôi" để sửa hồ sơ, có trang xem trước đúng như ứng viên sẽ thấy.
- **Cán bộ (STAFF):** màn "Doanh nghiệp" để xem và tìm công ty, xem danh sách HR của từng công ty, thêm HR (API DN-01), khoá và mở khoá tài khoản HR.

Khoá phải có hiệu lực ngay: HR bị khoá không gọi được API ở request kế tiếp và không đăng nhập lại được cho tới khi được mở khoá.

### Tiêu chí nghiệm thu (chốt)

1. staff1 khoá hr2 → request kế tiếp của hr2 bị 401/403; đăng nhập lại báo đã khoá.
2. Mở khoá → hr2 đăng nhập lại được.
3. Khoá mà không nhập lý do → bị chặn.
4. Trang xem trước của HR hiện đúng dữ liệu công ty đã lưu.
5. Tìm công ty theo tên hoặc mã số thuế ra đúng kết quả.
6. PR vào `test` có cách chạy, dữ liệu mẫu, ảnh màn hình; OpenAPI có endpoint mới.

### Ngoài phạm vi

- Trang công ty dành cho ứng viên (task ứng viên sau này dùng lại component xem trước của DN-05).
- Khoá/mở khoá **công ty** (chỉ khoá tài khoản HR). Bộ lọc trạng thái công ty vẫn có trên bảng.
- Số JD thật: cột "Số JD" và mục "Việc làm đang tuyển" tạm hiện `0` (chốt) cho tới khi task JD có bảng JD.
- Chặn HR bị khoá ở các service ngoài identity và company-service (tuyển dụng, thực tập...): ghi vào tài liệu để task sau áp dụng (chốt).

## 2. Ranh giới và luồng khoá (chốt)

- **identity-service** là nguồn sự thật của trạng thái tài khoản: `users.status`, `locked_reason`, `locked_at` (dùng chung với NT-01). Cũng là nơi duy nhất giữ email, họ tên.
- **company-service** nhận các API của cán bộ (`/staff/**`). Để khoá, nó gọi API nội bộ của identity rồi ghi **bản sao** trạng thái khoá vào `company_hr`. Bản sao này chỉ dùng để chặn request của HR ngay trong company-service mà không phải gọi identity mỗi request.
- Trang chi tiết công ty lấy email, họ tên, trạng thái của HR bằng một lời gọi tra cứu hàng loạt sang identity (chốt, phương án b). Identity tắt thì trang chi tiết lỗi 503.
- Mọi lời gọi company-service → identity dùng client và khoá nội bộ có sẵn từ DN-01 (`X-Internal-Key`, `INTERNAL_SERVICE_KEY`, `IDENTITY_SERVICE_URL`), không tự thử lại.

### Hai kiểu khoá trong `users`

| Kiểu | Do ai | Cột | Hết hạn |
| --- | --- | --- | --- |
| Khoá tạm (NT-01, giữ nguyên) | Sai mật khẩu 5 lần | `status = LOCKED`, `locked_until` | Tự hết sau 15 phút |
| Khoá bởi cán bộ (DN-05) | STAFF | `status = LOCKED`, `locked_reason`, `locked_at` | Chỉ khi mở khoá |

Quy tắc: **`locked_at` khác null nghĩa là khoá bởi cán bộ.** Mở khoá xoá `locked_reason`, `locked_at`, đặt `status = ACTIVE` (người thiết kế chọn: đồng thời xoá `locked_until` và đặt `failed_login_count = 0`, để mở khoá là sạch hẳn).

**Khoá tạm trên màn cán bộ (người thiết kế chọn).** `users.status` không đáng tin để hiển thị: khoá tạm hết hạn nhưng `status` vẫn là `LOCKED` cho tới lần đăng nhập kế tiếp (`LoginServiceImpl`). Vì vậy:

- Trạng thái trả cho company-service và hiện trên màn cán bộ là **giá trị suy ra**, không phải `users.status` thô: `LOCKED` khi và chỉ khi `locked_at` khác null; ngược lại `ACTIVE`.
- Thêm field `tempLockedUntil`: bằng `locked_until` nếu thời điểm đó còn ở tương lai và `locked_at` null, ngược lại null. Màn cán bộ hiện dòng phụ "Tạm khoá do sai mật khẩu đến hh:mm" cạnh trạng thái "Hoạt động"; nút vẫn là **Khoá** (không có Mở khoá cho khoá tạm, nó tự hết).
- Cán bộ khoá một HR đang bị khoá tạm: được, chuyển thành khoá bởi cán bộ. Mở khoá sau đó xoá luôn khoá tạm (theo quy tắc trên).
- "Đang không bị khoá thì unlock trả 200, không đổi gì" hiểu là `locked_at` null; khi đó unlock không đụng tới `locked_until`.

**Quên / đặt lại mật khẩu khi bị cán bộ khoá (người thiết kế chọn).** `PasswordResetServiceImpl` hiện đặt `status = ACTIVE` và xoá `locked_until` khi đặt lại mật khẩu. Sửa để:

- `POST /auth/forgot-password` với user có `locked_at` khác null: **không gửi email**, vẫn trả 200 cùng câu thông báo như mọi trường hợp (không để lộ trạng thái tài khoản).
- `POST /auth/reset-password` với token hợp lệ của user đang bị cán bộ khoá: vẫn đổi mật khẩu nhưng **không** xoá khoá cán bộ (`status`, `locked_at`, `locked_reason` giữ nguyên). Khoá tạm vẫn được xoá như cũ.

## 3. Dữ liệu

### 3.1 identity-service — `V2__user_admin_lock.sql`

```sql
ALTER TABLE users ADD COLUMN locked_reason VARCHAR(500);
ALTER TABLE users ADD COLUMN locked_at TIMESTAMP WITH TIME ZONE;
```

Thêm field `lockedReason`, `lockedAt` (`Instant`) vào entity `User`. Cú pháp phải chạy trên cả PostgreSQL và H2 `MODE=PostgreSQL` (người làm kiểm tra H2 chấp nhận hai câu `ALTER TABLE ... ADD COLUMN`).

### 3.2 company-service — `V2__company_hr_lock.sql`

```sql
ALTER TABLE company_hr ADD COLUMN locked_at TIMESTAMP WITH TIME ZONE;
```

Bản sao: khác null là HR đang bị cán bộ khoá. Lý do không sao chép (lấy từ identity khi hiển thị). Schema `company` như DN-01.

### 3.3 Dữ liệu mẫu

Giữ nguyên seed DN-01 (Công ty A: hr1, hr-new; Công ty B: hr2; staff1). Không seed tài khoản bị khoá sẵn (người thiết kế chọn): kịch bản nghiệm thu tự khoá hr2.

## 4. API

Mọi lỗi theo `ErrorResponse {code, message, errors?}`, message tiếng Việt. Đường dẫn là ở service; qua gateway thêm `/api/v1`. Mã lỗi mới: **`ACCOUNT_DISABLED`**, message "Tài khoản đã bị khoá" (người thiết kế chọn tên mã).

Theo NT-02: mỗi endpoint mới có annotation springdoc (`@Tag`, `@Operation` mô tả vai trò được gọi, `@ApiResponse` cho từng mã) để hiện trong Swagger UI `/api/docs`; `docs/api/company-openapi.yaml` và `identity-openapi.yaml` cập nhật làm hợp đồng tham khảo.

### 4.1 identity — đăng nhập và chặn token

**Đăng nhập (`POST /auth/login`)**, sửa `LoginServiceImpl.authenticate`. Thứ tự kiểm tra:

1. Không có user / không có mật khẩu → 401 `INVALID_CREDENTIALS` (như cũ).
2. **`locked_at` khác null → 403 `ACCOUNT_DISABLED` "Tài khoản đã bị khoá"**, kể cả khi mật khẩu đúng. Không tăng `failed_login_count`, không tự mở khoá. (Thông báo không nêu lý do, người thiết kế chọn.)
3. Khoá tạm còn hiệu lực → 429 `ACCOUNT_LOCKED` (như cũ).
4. Nhánh "hết thời gian khoá tạm thì đặt lại ACTIVE" chỉ chạy khi `locked_at` null. Đây là chỗ phải sửa: hiện nó đặt lại ACTIVE cho mọi `status = LOCKED`.

SSO (`POST /auth/sso`) áp dụng cùng quy tắc bước 2 cho user LMS bị khoá (người thiết kế chọn, cho nhất quán; DN-05 chỉ khoá HR nên hiếm gặp).

**Bộ lọc `IdentityJwtAuthenticationFilter`:** sau khi đọc user từ DB, nếu `locked_at` khác null → **401 `ACCOUNT_DISABLED`**, cho mọi đường dẫn kể cả `/auth/**` (để `GET /auth/me` cũng bị chặn). Kiểm tra này đặt trước kiểm tra `mustChangePassword`.

Frontend đã có sẵn: 401 ngoài các API đăng nhập → xoá phiên, về `/login` (`services/api.ts`). Màn đăng nhập hiển thị `message` từ backend, nên sẽ thấy "Tài khoản đã bị khoá".

### 4.2 identity — API nội bộ mới

Cùng chain và `InternalKeyFilter` của DN-01 (`InternalKeyFilter.INTERNAL_PATTERN`), không qua gateway.

Trong mọi response nội bộ dưới đây, `status` là giá trị suy ra (`LOCKED` khi `lockedAt` khác null, ngược lại `ACTIVE`) và có thêm `tempLockedUntil` theo mục 2.

**`PATCH /internal/users/{id}/lock`** body `{reason}` → `200 InternalUserResponse` (mở rộng thêm `status`, `lockedReason`, `lockedAt`, `tempLockedUntil`)

- `reason`: bắt buộc, sau khi trim không rỗng, tối đa 500 ký tự → sai thì 400 `VALIDATION_ERROR` `errors.reason`.
- User không có → 404. Không phải role HR → 409 `CONFLICT` "Chỉ khoá được tài khoản HR".
- Đặt `status = LOCKED`, `locked_reason = reason` (đã trim), `locked_at = now`. Đang bị cán bộ khoá sẵn → ghi đè lý do và thời điểm, vẫn 200 (người thiết kế chọn).

**`PATCH /internal/users/{id}/unlock`** → `200 InternalUserResponse`. Đang không bị khoá → 200, không đổi gì. Không có → 404; không phải HR → 409.

**`POST /internal/users/lookup`** body `{userIds: [uuid, ...]}` (1–100 phần tử) → `200 [{userId, email, fullName, role, status, lockedReason, lockedAt, tempLockedUntil}]`. Id không tồn tại thì bỏ qua (không lỗi). Danh sách rỗng hoặc quá 100 → 400.

### 4.3 company-service — API của cán bộ

Tất cả `@PreAuthorize(Roles.STAFF)` (như DN-01: chỉ STAFF). Token HR → 403 `FORBIDDEN`.

**`GET /staff/companies?q=&status=&page=&size=`** → `200 PageResponse<StaffCompanyItem>`

- `q` (tuỳ chọn, trim, tối đa 100): khớp khi **tên công ty chứa `q`** (không phân biệt hoa thường) **hoặc mã số thuế chứa `q`**. Rỗng = không lọc.
- `status` (tuỳ chọn): `ACTIVE` | `LOCKED` của công ty; giá trị khác → 400.
- `page` từ 1, mặc định 1; `size` mặc định 20, tối đa 100; ngoài khoảng → 400 `VALIDATION_ERROR` (quy ước chung).
- Sắp xếp `created_at` giảm dần (người thiết kế chọn).
- `StaffCompanyItem`: `id, name, taxCode, status, hrCount, jdCount, createdAt`. `hrCount` = số dòng `company_hr` của công ty (kể cả HR bị khoá). `jdCount` luôn `0` (chốt, tạm thời).
- Đếm `hrCount` bằng một truy vấn gộp cho cả trang, không truy vấn từng dòng.

**`GET /staff/companies/{id}`** → `200 StaffCompanyDetail`

- Gồm toàn bộ trường của `CompanyResponse` (DN-01) cộng `hrAccounts: [{userId, email, fullName, status, lockedReason, lockedAt, tempLockedUntil}]` (`status` là giá trị suy ra, mục 2).
- Danh sách HR: lấy `user_id` từ `company_hr`, gọi `POST /internal/users/lookup` một lần. Sắp xếp theo email (người thiết kế chọn).
- User có trong `company_hr` mà identity không trả về → bỏ khỏi danh sách và log WARN (người thiết kế chọn).
- Không có công ty → 404. Identity tắt/lỗi → 503 `EXTERNAL_UNAVAILABLE`.
- Công ty không có HR → `hrAccounts: []`, không gọi identity.

**`POST /staff/hr-accounts/{userId}/lock`** body `{reason}` → `200 StaffHrAccount` (cùng dạng một phần tử `hrAccounts`)

1. `reason` bắt buộc, trim không rỗng, tối đa 500 → 400 `VALIDATION_ERROR` `errors.reason` (tiêu chí 3). Kiểm tra trước khi gọi identity.
2. `userId` không có trong `company_hr` → 404 (không khoá được người không phải HR của một công ty).
3. Gọi identity `PATCH /internal/users/{userId}/lock`. Identity lỗi/tắt → 503 `EXTERNAL_UNAVAILABLE`, **không** ghi bản sao.
4. Ghi `company_hr.locked_at = lockedAt` identity trả về. Trả phần tử HR đã cập nhật.

**`POST /staff/hr-accounts/{userId}/unlock`** (không body) → `200 StaffHrAccount`. Như trên: 404 nếu không có trong `company_hr`, gọi identity unlock, rồi đặt `company_hr.locked_at = null`.

Lock/unlock gọi lại nhiều lần đều 200 (lặp lại an toàn). Nếu identity đã đổi mà ghi bản sao lỗi (hiếm) → 500; bấm lại sẽ đồng bộ (người thiết kế chọn, ghi rõ trong tài liệu). Lời gọi HTTP không nằm trong transaction DB đang mở (như DN-01).

### 4.4 company-service — chặn HR bị khoá

Thêm một bộ lọc chạy sau `JwtAuthenticationFilter` trong `CompanySecurityConfig`: nếu người gọi đã xác thực, role `HR`, và `company_hr` của `userId` có `locked_at` khác null → **401 `ACCOUNT_DISABLED`** (cùng dạng body). HR không có dòng `company_hr` → cho qua (hành vi DN-01 giữ nguyên). Đường công khai (`GET /companies/logos/**`, actuator, api-docs) không bị ảnh hưởng.

### 4.5 Thêm HR

Nút "+ Thêm HR" gọi `POST /staff/companies/{id}/hr-accounts` có sẵn từ DN-01, không đổi API.

### 4.6 Gateway

Route `company` hiện chỉ nhận `/api/v1/company/**,/api/v1/companies/**,/api/v1/staff/companies/**`, nên `/api/v1/staff/hr-accounts/**` sẽ 404 qua gateway. Thêm `/api/v1/staff/hr-accounts/**` vào predicate của route `company` (giữ `StripPrefix=2`, không thêm route id), mở rộng `CompanyRouteIntegrationTest` cho đường mới, cập nhật bảng route trong `docs/architecture/service-conventions.md`.

## 5. Frontend

Theo khung NT-02: trang nằm dưới `web/src/app/(pages)/(app)/`, đường dẫn tĩnh được ưu tiên hơn trang tạm `[role]/[...section]`; menu đã có "Doanh nghiệp" (`/staff/companies`) và "Công ty" (`/hr/company`). Dùng route guard của NT-02 (`useAuthGuard`), TanStack Query (khoá cache có user id như DN-01), react-hook-form + zod. Đọc `web/AGENTS.md` trước khi viết code Next. Dialog dùng component dialog theo style shadcn `base-mira` (`@base-ui/react`) thêm vào `app/components/ui` nếu chưa có (người thiết kế chọn).

### 5.1 Cán bộ (chốt, đã xem mockup)

**`/staff/companies`** — bảng công ty:

- Ô tìm "Tìm theo tên hoặc mã số thuế…" (gõ xong chờ ~300 ms mới gọi API), bộ lọc trạng thái (Tất cả / Hoạt động / Đã khoá), cột Tên công ty (link sang chi tiết) / Mã số thuế / Số HR / Số JD / Trạng thái, phân trang.
- `q`, `status`, `page` giữ trên URL (`?q=&status=&page=`) để tải lại hoặc quay lại vẫn đúng (người thiết kế chọn).
- Không có kết quả → dòng "Không tìm thấy công ty phù hợp".

**`/staff/companies/[id]`** — chi tiết:

- Đường dẫn quay lại "‹ Doanh nghiệp", đầu trang: logo, tên, MST, trạng thái, ngành · quy mô · thành phố.
- Bảng "Tài khoản HR (n)": họ tên + email, trạng thái; HR bị khoá hiện thêm thời điểm và lý do khoá. Mỗi dòng nút **Khoá** hoặc **Mở khoá**. Nút **"+ Thêm HR"** trên bảng.
- **Hộp xác nhận khoá:** tiêu đề "Khoá tài khoản `{email}`?", câu nhắc "HR sẽ bị đăng xuất ngay ở thao tác kế tiếp và không đăng nhập được cho tới khi được mở khoá.", ô **Lý do khoá \*** (nhiều dòng, tối đa 500), nút Huỷ / Khoá tài khoản. Lý do trống → báo "Vui lòng nhập lý do khoá" và không gọi API (backend vẫn kiểm lại).
- **Hộp mở khoá:** chỉ xác nhận, không lý do.
- **Hộp thêm HR:** Email, Họ tên; lỗi 409 "Email đã được sử dụng" hiện ngay trong hộp; thành công đóng hộp, danh sách cập nhật, báo "Đã tạo tài khoản và gửi email mật khẩu tạm".
- Sau khoá/mở khoá/thêm HR: cập nhật lại chi tiết và bảng công ty (số HR).
- Lỗi 503 → thông báo "Không kết nối được dịch vụ tài khoản, vui lòng thử lại".

### 5.2 HR (chốt, phương án B)

**`/hr/company`** — giữ bố cục hai cột của DN-01, thêm:

- **Giới thiệu nhiều dòng:** textarea cao (≥ 6 dòng), xuống dòng được giữ.
- **Logo có xem trước:** chọn file thì **chưa tải lên**; hiện ảnh mới cạnh logo cũ cùng nút "Dùng ảnh này" / "Huỷ". "Dùng ảnh này" mới gọi `POST /companies/me/logo`. File > 2MB hoặc không phải JPG/PNG bị chặn ngay, không hiện xem trước.
- **Nút Lưu báo kết quả:** đang lưu → nút quay vòng và khoá; thành công → "✓ Đã lưu lúc hh:mm"; lỗi → thông báo đỏ dưới nút và lỗi từng trường dưới ô (giữ cách sửa của DN-01: không mất nội dung đang sửa dở khi đổi logo).
- Nút **"Xem như ứng viên"** → `/hr/company/preview`. Khi form đang có thay đổi chưa lưu, dưới nút hiện nhắc "Xem trước chỉ hiện bản đã lưu" (người thiết kế chọn).

**`/hr/company/preview`** — trang xem trước:

- Banner đầu trang: "Bạn đang xem trang công ty như ứng viên thấy · ‹ Quay lại chỉnh sửa".
- Nội dung là component dùng chung **`CompanyPublicView`** (props: dữ liệu công ty) trong `app/components/company/`: logo, tên, ngành · quy mô nhân sự · thành phố, website (link mở tab mới, chỉ `http`/`https`), địa chỉ, giới thiệu giữ xuống dòng, mục "Việc làm đang tuyển (0)". Không hiện mã số thuế, trạng thái, ngày tạo (thông tin nội bộ, người thiết kế chọn).
- Dữ liệu lấy từ `GET /companies/me`, tải lại mỗi lần mở trang (không dùng dữ liệu cũ trong cache) để luôn đúng bản đã lưu (tiêu chí 4).

## 6. Cấu hình

Không thêm biến môi trường mới (dùng lại `INTERNAL_SERVICE_KEY`, `IDENTITY_SERVICE_URL`).

## 7. Kiểm thử

Test backend không cần Postgres (H2 + Flyway như DN-01). company-service giả lập identity như các test DN-01 (`@MockBean IdentityClient`) và test client bằng HTTP server nhỏ.

| # | Ca | Ở đâu | Tiêu chí |
| --- | --- | --- | --- |
| 1 | Khoá hr2 qua internal → `GET /auth/me` bằng token cũ → 401 `ACCOUNT_DISABLED`; `POST /auth/login` đúng mật khẩu → 403 `ACCOUNT_DISABLED` "Tài khoản đã bị khoá", `failed_login_count` không tăng | identity | 1 |
| 2 | company-service: `company_hr.locked_at` khác null → `GET /companies/me` bằng token hr2 → 401 `ACCOUNT_DISABLED`; HR khác vẫn 200 | company | 1 |
| 3 | `POST /staff/hr-accounts/{hr2}/lock` → identity nhận lock, `company_hr.locked_at` được ghi; identity lỗi → 503 và bản sao không đổi | company | 1 |
| 4 | Mở khoá → identity unlock, `locked_at` xoá; hr2 đăng nhập lại 200; `GET /companies/me` 200 | identity + company | 2 |
| 5 | Lý do rỗng / toàn dấu cách / > 500 ký tự → 400 `errors.reason`, identity **không** được gọi; cùng luật ở API nội bộ | company + identity | 3 |
| 6 | Khoá tạm 15 phút của NT-01 vẫn như cũ: sai 5 lần → 429; hết hạn → đăng nhập được; không bị nhầm thành `ACCOUNT_DISABLED`; lookup trả `status = ACTIVE` + `tempLockedUntil` khi đang khoá tạm, `tempLockedUntil = null` khi khoá tạm đã hết hạn dù `users.status` còn `LOCKED` | identity | — |
| 6b | Forgot-password với user bị cán bộ khoá → 200, không gửi email; reset-password bằng token hợp lệ → đổi mật khẩu nhưng vẫn bị khoá (login 403 `ACCOUNT_DISABLED`) | identity | 1 |
| 6c | Gateway chuyển `/api/v1/staff/hr-accounts/{id}/lock` sang `/staff/hr-accounts/{id}/lock` của company-service | gateway | 1 |
| 7 | Lock/unlock gọi lặp → 200; userId không thuộc `company_hr` → 404; internal lock user không phải HR → 409 | company + identity | — |
| 8 | `GET /staff/companies?q=` theo một phần tên (khác hoa thường) và theo một phần MST ra đúng công ty; `status` lọc đúng; phân trang `total`/`page`; `hrCount` đúng; `jdCount = 0`; tham số sai → 400 | company | 5 |
| 9 | `GET /staff/companies/{id}`: có HR → gọi lookup một lần, trả email/họ tên/trạng thái/lý do; không có HR → `[]` không gọi identity; identity tắt → 503; 404 | company | — |
| 10 | Token HR gọi `/staff/**` → 403; không token → 401 | company | — |
| 11 | `POST /internal/users/lookup`: id lạ bị bỏ qua; rỗng / > 100 → 400; thiếu khoá → 401 | identity | — |
| 12 | Trang xem trước dùng dữ liệu `GET /companies/me` vừa tải (kiểm tra tay: sửa, lưu, mở xem trước thấy đúng; sửa chưa lưu thì xem trước vẫn là bản cũ) | web (tay) | 4 |

Frontend không có test runner: chạy `npm run type-check`, `npm run lint`, `npm run build`, và kiểm tra tay theo kịch bản nghiệm thu.

## 8. Tài liệu bàn giao

- `docs/api/DN-05-company-hr-ui.md` theo mẫu DN-01: mục tiêu, DB, API, luồng khoá, cách chạy (lệnh `install` rồi `spring-boot:run` từng module, có dấu nháy trên PowerShell), dữ liệu mẫu, ca kiểm thử ↔ tiêu chí, ảnh màn hình, trạng thái xác minh, checklist PR. Ghi rõ: chỉ identity và company-service chặn HR bị khoá; service khác cần làm tương tự ở task sau.
- `docs/api/DN-05.http`: lấy token staff1, danh sách và tìm kiếm công ty, chi tiết, khoá thiếu lý do, khoá hr2, hr2 gọi `/companies/me` (401), hr2 đăng nhập (403), mở khoá, hr2 đăng nhập lại.
- Annotation springdoc cho mọi endpoint mới; cập nhật `docs/api/company-openapi.yaml`, `docs/api/identity-openapi.yaml`.
- Cập nhật `docs/architecture/service-conventions.md` (mã `ACCOUNT_DISABLED`, quy tắc chặn HR bị khoá) và `docs/api/NT-01-authentication.md` mục quy tắc khoá (hai kiểu khoá) — báo người làm NT-01.
- Ảnh màn hình trong `docs/api/screenshots/DN-05/` theo tiêu chí 6.

## 9. Để người làm tự quyết

- Tên lớp, DTO, cách chia service/impl.
- Cách viết truy vấn tìm kiếm và đếm HR (JPQL, Specification, native), miễn là một truy vấn đếm cho cả trang.
- Chi tiết giao diện trong khuôn mockup đã duyệt: khoảng cách, icon, hiệu ứng.
- Câu chữ tiếng Việt của các thông báo không ghi cụ thể ở trên.
