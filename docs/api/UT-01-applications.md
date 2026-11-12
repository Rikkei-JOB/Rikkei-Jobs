# UT-01 — Ứng tuyển bằng CV và theo dõi trạng thái

## Endpoint qua gateway

Các endpoint ứng viên yêu cầu JWT role CANDIDATE. Controller recruitment giữ prefix `/recruitment`; gateway bỏ `/api/v1`.

| Method | URL dưới `/api/v1/recruitment` | Kết quả |
| --- | --- | --- |
| POST | `/jobs/{jobId}/apply` | 201 detail; body `{cvId, coverLetter?}` |
| GET | `/me/applications?status=&page=1&size=20` | 200 `{items,total,page,size}` |
| GET | `/me/applications/{id}` | 200 detail kèm lịch sử |
| POST | `/me/applications/{id}/withdraw` | 200 detail; body `{reason}` |
| GET | `/applications/{id}/cv` | PDF riêng tư; chủ đơn hoặc HR cùng companyId |

Thư giới thiệu tối đa 5.000 ký tự, lý do rút bắt buộc sau trim và tối đa 1.000. Thời gian UTC ISO 8601; FE hiển thị giờ Việt Nam. Phân trang từ 1, size 1–100, mặc định 20. Status không hợp lệ trả 400.

Luật: SUBMITTED → SCREENING → INTERVIEW → OFFER → HIRED; REJECTED từ mọi bước trước HIRED; WITHDRAWN chỉ từ 3 bước đầu. Không mở lại bản ghi terminal. Được nộp lại cùng JD sau REJECTED/WITHDRAWN bằng bản ghi và PDF mới. Các trạng thái khác chặn đơn mới với 409, kể cả request đồng thời.

Lỗi giữ `{code,message}`: 400 validation; 401 token; 403 quyền; 404 tài nguyên; 409 trạng thái/trùng đơn; 502 dữ liệu dependency sai; 503 dependency/storage không sẵn sàng. JD được kiểm tra APPROVED, `deadline > now`, company không khoá trước và sau khi xuất PDF; deadline kiểm lại trong transaction ghi đơn. Thay đổi trạng thái JD ngay sau lần kiểm cuối không có transaction nguyên tử xuyên service.

## Hợp đồng consumer cho các task phụ thuộc

**HS-04 và DN-04 chưa có trong checkout tại lúc triển khai.** Các URL dưới đây là default đề xuất, cấu hình được; owner cần đối chiếu khi tích hợp. Không có production fixture fallback hoặc truy cập bảng của service khác.

Đã có **mock HTTP dùng chung JD/CV** trong `mock/`, chạy cùng Express cổng 9000. Xem [cách chạy, fixture IDs, JWT và nối với ứng dụng thật](../../mock/README.md#mock-jdcv-cho-ut-01), [request mẫu](../../mock/requests.http) và [OpenAPI hợp đồng đề xuất](ut01-dependencies-openapi.yaml). Mock này cho phép các bên thử request/response trước khi producer sẵn sàng; việc owner HS-04/DN-04 duyệt hợp đồng vẫn là bước cần xác nhận riêng. Mock không tự được bật trong cấu hình production.

- Company: GET `${COMPANY_SERVICE_URL}${RECRUITMENT_JOB_CONTEXT_PATH}`, default path `/internal/jobs/{jobId}/application-context`, header `X-Internal-Key` dùng `RECRUITMENT_COMPANY_SERVICE_KEY`, hoặc `INTERNAL_SERVICE_KEY` nếu biến riêng chưa được đặt. Cấu hình khoá rỗng bị từ chối. Theo quy ước nội bộ mới nhất trên nhánh test, endpoint `/internal/**` chỉ gọi trực tiếp service, không đi qua route gateway. Chỉ endpoint nội bộ được xác thực mới được trả `hrEmails`; không ghi job_views.
- Body: `{jobId, companyId, jobTitle, companyName, status, deadline, companyLocked, hrEmails: ["hr@company.test"]}`. IDs UUID; deadline ISO 8601 có offset; HR phải thuộc công ty và còn hoạt động. Thiếu hoặc sai dữ liệu phải bị từ chối, không coi là APPROVED.
- Profile: GET `${PROFILE_SERVICE_URL}${RECRUITMENT_CV_PDF_PATH}`, default `/profile/cvs/{cvId}/pdf`, chuyển tiếp JWT ứng viên. HS-04 phải xác minh CV ownership; response `application/pdf`, bytes PDF hoàn chỉnh. Không dùng public CV URL.
- FE: list CV từ `NEXT_PUBLIC_CV_API_PATH`, default `/profile/cvs`, trả mảng hoặc PageResponse có item `{id,name,targetPosition}`. `NEXT_PUBLIC_CV_PAGE_PATH` default `/candidate/cvs`; `NEXT_PUBLIC_JOB_PAGE_PATH` default `/jobs`. Chỉ dùng các route thật khi HS-04/DN-04 cung cấp.
- DN-04 import `ApplyButton` từ `@/app/components/applications/apply-button`, truyền `jobId` và `acceptingApplications`; không tự dựng lại trang JD trong UT-01.
- UT-02 gọi cùng `ApplicationTransitionService` và `ApplicationStatusPolicy`, kiểm quyền HR trước khi gọi. Không update status trực tiếp; transition service khoá hàng và ghi history trong transaction.

## PDF và lịch sử

`cvSnapshotUrl` là URL tương đối qua gateway đến endpoint PDF được bảo vệ. File đặt trong `RECRUITMENT_CV_STORAGE_DIR`, không static/public. Sửa/xoá CV không ảnh hưởng file đã nộp. QR vẫn trỏ trang PUBLIC hiện tại của HS-04, không snapshot quyền công khai. Danh sách đơn dùng metadata công ty/JD tại lúc nộp, không cần gọi endpoint công khai DN-04.

Ứng viên thấy đủ transition; note chỉ hiện với WITHDRAWN do chính ứng viên thực hiện. Ghi chú HR giữ nội bộ.

## Kiểm thử và vận hành

Test PostgreSQL dùng Testcontainers mặc định; có thể dùng DB kiểm thử riêng qua `UT01_TEST_DB_URL`, `UT01_TEST_DB_USER`, `UT01_TEST_DB_PASSWORD`. **Không trỏ vào database sản xuất hoặc DB có dữ liệu cần giữ**, vì test xoá các bảng recruitment giữa các case. Cách chạy và kết quả kiểm chứng được cập nhật khi hoàn thành triển khai.

### Chạy backend và frontend

Từ root repo với JDK 21, Maven 3.9+, PostgreSQL và các dependency đã cấu hình:

```powershell
mvn -f server/pom.xml -pl services/recruitment-service -am package
java -jar server/services/recruitment-service/target/recruitment-service-1.0.0-SNAPSHOT.jar
```

Mặc định Flyway tạo schema riêng `recruitment`, không dùng chung migration history với identity/profile. Snapshot local mặc định nằm dưới `.runtime/recruitment-cvs` tính từ **working directory lúc chạy Java**; đặt `RECRUITMENT_CV_STORAGE_DIR` thành đường tuyệt đối nếu thay working directory. Sao lưu DB và volume PDF cùng nhau; không xoá snapshot của đơn REJECTED/WITHDRAWN.

Container chạy user 10001 với storage `/app/data/cv-snapshots`. Gắn **named volume** bền vững, ví dụ `-v ut01-cv-snapshots:/app/data/cv-snapshots`; bind mount cần cấp quyền ghi cho uid 10001. Chỉ xoá snapshot khi thất bại trước transaction ghi hoặc transaction xác nhận ROLLED_BACK. Nếu mất phản hồi commit/kết quả UNKNOWN thì giữ file để tránh xoá PDF của đơn đã commit. Log có applicationId để đối soát khi kết quả ghi không rõ hoặc cleanup lỗi; file không có đơn tương ứng phải được kiểm tra thủ công sau khoảng chờ an toàn, không xoá hàng loạt khi có request đang xử lý.

Trong `web/`, tạo `.env.local` bằng cấu hình phù hợp rồi chạy `npm ci`, `npm run dev`. Truy cập `/candidate/applications`; `/candidate/applications/new?jobId=<UUID>` là đường nhập đơn thuộc UT-01 để DN-04 có thể liên kết khi chưa tích hợp component trực tiếp. Không có JD giả hoặc bộ dữ liệu CV fallback trong production.

### Email outbox

Email gửi qua `MAIL_HOST/MAIL_PORT/MAIL_USER/MAIL_PASSWORD/MAIL_FROM`, dev có thể dùng MailHog localhost:1025. Tắt worker khi cần qua `RECRUITMENT_OUTBOX_ENABLED=false`. Mỗi người nhận có một outbox unique; worker claim từng hàng bằng SKIP LOCKED, lease 60 giây, SMTP IO có timeout, gửi tối đa 5 lần với khoảng chờ 1/5/15/60 phút. Sau đó FAILED; đơn ứng tuyển vẫn được giữ nguyên.

Theo dõi hàng FAILED bằng `SELECT id, application_id, attempts FROM recruitment.application_notification_outbox WHERE status='FAILED';`. Để thử lại một hàng đã được kiểm tra, operator chạy câu lệnh parameterized: `UPDATE recruitment.application_notification_outbox SET status='PENDING', attempts=0, next_attempt_at=CURRENT_TIMESTAMP, lease_until=NULL WHERE id=:notificationId AND status='FAILED';`. Không cung cấp API requeue công khai. Email có Message-ID ổn định; SMTP là **at-least-once**, có thể trùng nếu crash sau gửi nhưng trước cập nhật SENT.

### Lệnh kiểm chứng

```powershell
mvn -f server/pom.xml -pl services/recruitment-service,api-gateway -am test
```

Trong `web/`:

```powershell
npm run type-check
npm run lint
npm run build
npx playwright install chromium
npm run test:e2e
```

Windows có Edge sẵn: đặt `$env:UT01_BROWSER_CHANNEL='msedge'` để dùng Edge headless. CI mặc định dùng Chromium. Browser tests dùng API test doubles qua Playwright; PostgreSQL tests chạy services/transactions thật với mock ở ranh giới JD/PDF. Chúng không chứng minh HS-04/DN-04 live hoặc QR PUBLIC integration đã hoàn tất.

### Kết quả xác minh 2026-11-12

- Đã hợp nhất `origin/test` tại `fae21dc` vào feature UT-01; giữ nguyên thay đổi DN-01 và shared conventions của nhóm.
- `mvn -f server/pom.xml clean test`: 454 tests, 0 failures, 0 errors, 1 test có sẵn skipped; 12 module đều SUCCESS. Recruitment riêng có 38 test, dùng PostgreSQL 18 local riêng trên port 55439; CI dùng Testcontainers PostgreSQL khi không đặt UT01_TEST_DB_URL.
- Frontend: type-check, lint và production build đều qua; 9 Playwright test qua trên Edge headless, gồm ứng tuyển, giữ thư khi lỗi, chưa có CV, auth, lọc giữ sau F5, rút đơn, OFFER, ứng tuyển lại và viewport mobile. Suite cũng qua khi đổi JD root thành `/vacancies`.
- Review độc lập đã thực hiện; các phát hiện về snapshot khi mất xác nhận commit và liên kết JD cấu hình đã được sửa với regression RED→GREEN. Test MIME xác minh subject/body tiếng Việt, recipient và Message-ID ổn định.
- Chưa nghiệm thu live HS-04/DN-04, QR PUBLIC/job_views hoặc SMTP thật vì các dependency/môi trường đó chưa có trong checkout kiểm thử. Docker daemon local không sẵn sàng nên chưa build container tại máy này. PR giữ draft cho đến khi tích hợp và kiểm tra các mục này.
