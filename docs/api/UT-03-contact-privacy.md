# UT-03 — Bảo vệ liên hệ ứng viên

UT-03 nằm ở recruitment-service. Email/phone trong JSON danh sách/chi tiết HR luôn null; chỉ GET `/api/v1/candidates/{userId}/contact` trả liên hệ và ghi audit trước khi trả response. Công ty từng nhận đơn vẫn có quyền sau WITHDRAWN/REJECTED; STAFF APPROVED cấp quyền toàn công ty, không hết hạn. Nếu có cả hai căn cứ, log ghi APPLICATION.

## Chạy và demo

Dùng cách chạy repo trong README, bật dev seed identity/company trên DB demo riêng. Migration recruitment `V9__contact_privacy.sql` tự tạo hai bảng; không sửa migration cũ. Không có biến môi trường mới. Các khóa `RECRUITMENT_IDENTITY_SERVICE_KEY`, `RECRUITMENT_PROFILE_SERVICE_KEY`, `RECRUITMENT_COMPANY_SERVICE_KEY` phải khớp service đích; API nội bộ không có route gateway.

Chạy fixture `docs/api/fixtures/ut03-contact-demo.sql` bằng psql trên **DB demo dùng một lần**, sau migration tất cả service. Fixture dùng ba ứng viên và hai công ty/HR từ dev seed; không thay đổi quyết định đã có khi chạy lại. Hồ sơ đã tồn tại ở trạng thái PRIVATE cần công khai bằng HS-05 trước khi xin mở mới. Fixture application chỉ minh họa lịch sử WITHDRAWN, không có tệp CV thật.

- candidate1: từng ứng tuyển công ty A; HR1 xem được, HR2 công ty B nhận 403.
- candidate2: một yêu cầu PENDING và lịch sử REJECTED của công ty A; STAFF có thể duyệt/từ chối.
- candidate3: yêu cầu APPROVED cho công ty A; HR1 có nút Xem liên hệ.

URL HR: `/hr/candidates/{userId}/contact`; chi tiết đơn `/hr/recruitment/{applicationId}` dùng cùng panel. STAFF: `/staff/contact-requests` và `/staff/contact-access-logs`. Ứng viên: menu Hồ sơ → Lịch sử xem liên hệ, URL `/candidate/contact-access-logs`.

STAFF lọc log bằng ID công ty/ứng viên và ngày Việt Nam. Ngày kết thúc được gồm trọn ngày, BE dùng khoảng UTC `[from,to)`. Ứng viên xem từng lần truy cập, nhóm công ty trong trang hiện tại; không suy ra tổng công ty/lần xem cuối từ một trang.

Xem [HTTP mẫu](UT-03.http) và [OpenAPI](ut03-openapi.yaml). Script `scripts/ut03-contact-demo.ps1` nhận token qua tham số ở máy local, kiểm công ty B bị khóa rồi gọi contact hai lần và xác minh lịch sử tăng hai dòng; không in token hoặc contact.

## API và lỗi

Mọi endpoint có prefix gateway `/api/v1`. Danh sách trả `{items,total,page,size}`, page bắt đầu 1, size mặc định 20 và tối đa 100.

| API | Vai trò |
| --- | --- |
| POST /candidates/{userId}/contact-requests {jobId,reason} | HR |
| GET /candidates/{userId}/contact-access | HR; chỉ metadata quyền |
| GET /candidates/{userId}/contact | HR; dữ liệu liên hệ + audit |
| GET /staff/contact-requests?status= | STAFF |
| POST /staff/contact-requests/{id}/approve | STAFF |
| POST /staff/contact-requests/{id}/reject {reason} | STAFF |
| GET /staff/contact-access-logs?companyId=&userId=&from=&to= | STAFF |
| GET /me/contact-access-logs | CANDIDATE |

Không có quyền: 403 CONTACT_LOCKED. Yêu cầu đang chờ trùng: 409 CONTACT_REQUEST_PENDING. Đã đủ quyền: 409 CONTACT_ALREADY_ACCESSIBLE. Yêu cầu đã xử lý: 409 CONTACT_REQUEST_ALREADY_REVIEWED. Lý do trim, bắt buộc, tối đa 2000 ký tự. JD phải thuộc công ty hiện hành; chưa có quyền chỉ xin mở hồ sơ PUBLIC. APPROVED/historic application không tự mất khi hồ sơ chuyển PRIVATE. Role sai nhận 403, HR bị khóa nhận 401 ACCOUNT_DISABLED theo guard hiện có.

## Dữ liệu và tính nhất quán

contact_requests lưu company/user/job/reason/status, created_by/created_at, reviewed_by/reviewed_at và rejection_reason riêng. Partial unique company/user khi PENDING chặn gửi đồng thời; khóa hàng chặn duyệt hai lần. REJECTED được gửi lại, giữ nguyên lịch sử.

contact_access_logs chỉ lưu ID công ty/HR/ứng viên, via và timestamp server; không lưu email/phone. Không có API sửa/xóa log. Chỉ call contact thành công sinh log; status/list/request/review/lỗi không sinh log. Không deduplicate giữa các lần chủ động xem. Nếu lookup liên hệ hoặc commit audit lỗi, không trả contact. API contact/history có Cache-Control:no-store; FE không prefetch/retry tự động hoặc persist contact.

Tên trong lịch sử lấy theo metadata hiện hành, fallback về ID nếu công ty/HR không còn hoặc service metadata không sẵn sàng. Không bỏ mất dòng nhật ký khi thiếu tên.

## Kiểm chứng

Backend dùng JUnit/MockMvc và PostgreSQL riêng; profile/company/gateway có kiểm thử boundary. ContactLiveFlowTest chạy HTTP thật, JWT thật, RestClient thật gọi upstream HTTP fixture và DB thật, kiểm luồng xin → STAFF duyệt → hai lần xem → lịch sử ứng viên. ContactCommitFailureTest dùng deferred PostgreSQL trigger để chứng minh lỗi tại commit không trả liên hệ. ContactDemoFixtureTest chạy fixture SQL với migration thật của identity/profile/company trên DB test và xác minh chạy lại không ghi đè quyết định STAFF.

```powershell
# Maven trên PATH, Docker hoặc DB PostgreSQL test riêng theo ApplicationIntegrationSupport.
mvn -f server/pom.xml -pl services/recruitment-service,services/profile-service,services/company-service,api-gateway -am test
npm --prefix web run type-check
npm --prefix web run lint
npm --prefix web run build
$env:PLAYWRIGHT_PORT='3113' # Có thể đổi nếu cổng đã được checkout khác sử dụng.
npm --prefix web run test:e2e -- e2e/ut03-hr-contact.spec.ts e2e/ut03-contact-workflow.spec.ts e2e/ut03-contact.visual.spec.ts --workers=2
```

Ảnh nghiệm thu ở `docs/api/screenshots/ut03/`, desktop 1440px, tablet 768px, mobile 390px. Các test UI dùng HTTP fixture để kiểm riêng hành vi trình duyệt; test HTTP backend kiểm transaction và contract thực.

## Phạm vi

Không triển khai DN-03, không thêm shortlist/invitation/tìm kiếm ứng viên. Quyền tự xem của ứng viên và gửi email hệ thống giữ nguyên. STAFF/ADMIN giữ quyền đọc workflow đã có, endpoint UT-03 dành cho STAFF chỉ cho STAFF. Tệp CV/cover letter/nội dung tự do có thể chứa liên hệ ứng viên tự ghi: UT-03 không sửa/redact nội dung tệp; bảo vệ trường JSON email/phone có cấu trúc.
