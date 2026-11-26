# UT-03 — Bảo vệ liên hệ và nhật ký truy cập

Ngày: 2026-11-26. Trạng thái: bản thiết kế chờ người dùng duyệt.

## Mục tiêu và phạm vi

Bảo vệ email/số điện thoại ứng viên ở BE và lưu mọi lần HR lấy liên hệ thành công. Triển khai đầy đủ migration, BE, FE, tài liệu API và kiểm thử UT-03 theo Spring Boot/Java 21, PostgreSQL, Next.js/React/TypeScript hiện có. PR vào `test`, tiêu đề bắt đầu `[UT-03]`.

Không triển khai DN-03: không thêm tìm kiếm/xếp hạng ứng viên, tổng hợp JD, shortlist hoặc invitation. Không thay đổi nghiệp vụ gửi email phỏng vấn/offer tự động hay quyền ứng viên xem hồ sơ của chính mình.

## Các quyết định đã được người dùng chốt

- UT-03 do `recruitment-service` sở hữu; lấy thông tin liên hệ qua API nội bộ của service sở hữu dữ liệu.
- Quyền thuộc cặp công ty–ứng viên, áp dụng cho HR hợp lệ của công ty, không giới hạn người tạo JD/yêu cầu.
- Có bất kỳ đơn ứng tuyển nào của ứng viên vào JD công ty là đủ; vẫn đủ khi đơn WITHDRAWN hoặc REJECTED. Không lọc trạng thái đơn.
- Ngoài lịch sử ứng tuyển, yêu cầu được STAFF APPROVED cũng cấp quyền cho công ty.
- HR xem liên hệ bằng nút **Xem liên hệ**. API danh sách/chi tiết không tự trả liên hệ; mỗi lần gọi `/contact` thành công tạo một dòng nhật ký.
- STAFF có màn duyệt và màn nhật ký có bộ lọc; ứng viên có danh sách công ty đã xem liên hệ.
- Không triển khai DN-03. Nhánh đích PR là `test`.

## Các hướng đã cân nhắc

1. **Recruitment sở hữu quyền, yêu cầu và nhật ký — được chọn.** Đơn ứng tuyển và companyId đã ở đây, kiểm tra quyền và ghi log tập trung, ít phụ thuộc liên service trên nhánh kiểm tra quyền.
2. **Profile sở hữu UT-03.** Gần dữ liệu hồ sơ nhưng cần gọi recruitment xác minh ứng tuyển. Không chọn vì phân tán nghiệp vụ quyền và nhật ký.

## Hiện trạng và điểm tích hợp

- `HrApplicationReadService.detail` hiện ghép email từ `IdentityCandidateClient` với phone từ `CandidateContactClient`, và FE `hr-application-detail-view.tsx` hiển thị ngay. Phải chuyển phần HR sang DTO liên hệ rỗng và contact controls.
- `ApplicationRepository` lưu trực tiếp companyId/userId. Thêm truy vấn tồn tại không lọc trạng thái để kiểm tra lịch sử ứng tuyển theo công ty.
- Profile có API phone nội bộ bảo vệ bằng JWT và internal key. Identity có client lấy thông tin ứng viên. Dùng các client hiện có; không mở dữ liệu liên hệ nội bộ ra gateway.
- Gateway hiện route recruitment theo `/api/v1/recruitment/**`. Thêm route chính xác cho endpoint UT-03 dưới `/api/v1/candidates/**`, `/api/v1/staff/contact-requests/**`, `/api/v1/staff/contact-access-logs`, `/api/v1/me/contact-access-logs`; không route toàn bộ `/staff/**` hoặc `/me/**` để tránh chiếm API service khác.
- DN-03 chưa có luồng tìm ứng viên. UT-03 cung cấp trang liên hệ riêng theo userId và component có thể gắn vào thẻ/chi tiết tương lai; không xây màn tìm kiếm để thay DN-03.

## Quyền và hợp đồng trả dữ liệu

`ContactAccessService.canSeeContact(companyId, userId)` dùng chung, true khi có application hoặc APPROVED request. Một hàm nội bộ trả cả căn cứ cấp quyền để ghi log; nếu có cả hai, ưu tiên APPLICATION cho kết quả ổn định.

companyId và hrId luôn lấy từ phiên HR đã xác thực, không nhận từ body/query. Dùng cơ chế xác minh tài khoản HR/công ty hiện có của repo; HR không có công ty hợp lệ bị từ chối. Không cấp quyền dựa trên việc đã gửi yêu cầu PENDING, shortlist, lời mời, hoặc công ty sở hữu JD khác.

Quy tắc mặc định: DTO ứng viên trả cho HR trong danh sách/chi tiết có `email: null`, `phone: null`, kể cả HR đã đủ quyền. Chỉ endpoint `/contact` trả hai trường thật. Có thể thêm metadata quyền vào DTO hoặc gọi endpoint trạng thái; không gọi `/contact` ngầm để kiểm tra quyền.

Quét các response có cấu trúc chứa liên hệ ứng viên (chi tiết đơn, lịch phỏng vấn, offer và dữ liệu lồng nhau) để không có đường lấy email/phone HR mà bỏ qua cơ chế audit. STAFF/ADMIN giữ phạm vi nghiệp vụ đang có; endpoint duyệt/nhật ký mới chỉ cấp STAFF theo đề bài. Các endpoint hồ sơ của chính ứng viên và API nội bộ có xác thực service không thuộc API liên hệ của HR.

Tệp CV ứng viên đã nộp có thể chứa liên hệ trong nội dung tự do. UT-03 bảo vệ trường dữ liệu có cấu trúc; không tự sửa PDF/CV/cover letter hoặc che nội dung tự do. Ghi rõ giới hạn này trong tài liệu bàn giao.

## DB

Migration Flyway tại recruitment, theo quy ước tên và schema của service. Không dùng FK xuyên database/service.

`contact_requests`:

- id UUID PK; company_id, user_id, job_id UUID NOT NULL.
- reason varchar(2000) NOT NULL; status PENDING/APPROVED/REJECTED, mặc định PENDING.
- reviewed_by UUID và reviewed_at timestamptz nullable khi PENDING.
- Bổ sung `rejection_reason varchar(2000)` để lưu lý do STAFF từ chối, `created_by UUID` và `created_at timestamptz` để biết ai gửi/khi nào. Không ghi đè lý do xin mở bằng lý do từ chối.
- Index theo company/user/status và status/created_at/id. Partial unique theo (company_id,user_id) khi PENDING để nhiều HR không tạo yêu cầu đang chờ trùng nhau.

`contact_access_logs`:

- id UUID PK; company_id, hr_id, user_id UUID NOT NULL; via APPLICATION/APPROVED_REQUEST; created_at timestamptz NOT NULL từ đồng hồ server.
- Append-only ở API; không có endpoint sửa/xóa. Không lưu bản sao email/phone.
- Index theo user_id/created_at/id và company_id/created_at/id để phục vụ bộ lọc và lịch sử cá nhân.

## API

Các đường dưới đây có prefix `/api/v1` tại gateway; lỗi theo `{code,message}` hiện có.

| Endpoint | Quyền và hành vi |
| --- | --- |
| POST `/candidates/{userId}/contact-requests` `{jobId,reason}` | HR; kiểm tra ứng viên tồn tại và PUBLIC nếu chưa có quyền, JD thuộc công ty hiện tại. Lý do trim, bắt buộc, tối đa 2000 ký tự. Tạo PENDING, trả 201. |
| GET `/candidates/{userId}/contact-access` | HR; bổ sung cho FE: trả `canSeeContact`, `via` nullable, `requestStatus` nullable và yêu cầu gần nhất của chính công ty; không có email/phone. |
| GET `/candidates/{userId}/contact` | HR; thiếu quyền trả 403 `CONTACT_LOCKED`; đủ quyền lấy `{email,phone}`, ghi một log rồi mới trả 200. |
| GET `/staff/contact-requests?status=&page=&size=` | STAFF; phân trang, lọc trạng thái; có tên công ty/ứng viên/JD và lý do phục vụ duyệt, không trả liên hệ. |
| POST `/staff/contact-requests/{id}/approve` | STAFF; chỉ chuyển PENDING thành APPROVED, lưu người duyệt/thời gian. |
| POST `/staff/contact-requests/{id}/reject` `{reason}` | STAFF; chỉ chuyển PENDING thành REJECTED; lý do trim bắt buộc, tối đa 2000 ký tự. |
| GET `/staff/contact-access-logs?companyId=&userId=&from=&to=&page=&size=` | STAFF; từng lần truy cập, tên công ty/HR/ứng viên, via và thời gian, không trả liên hệ. |
| GET `/me/contact-access-logs?page=&size=` | CANDIDATE; lọc userId từ JWT, trả từng log của chính mình với tên công ty, via và thời gian; FE nhóm theo công ty. |

Quy ước bổ sung do người thiết kế đề xuất:

- Đã đủ quyền thì gửi yêu cầu trả 409 `CONTACT_ALREADY_ACCESSIBLE`; có yêu cầu PENDING thì trả 409 `CONTACT_REQUEST_PENDING`. Sau REJECTED được gửi yêu cầu mới; yêu cầu cũ giữ nguyên để tra cứu.
- Duyệt/từ chối dùng khóa bản ghi hoặc cập nhật có điều kiện status=PENDING. Hai STAFF duyệt đồng thời: chỉ một thao tác thành công, thao tác còn lại trả 409 `CONTACT_REQUEST_ALREADY_REVIEWED`.
- APPROVED không hết hạn, không có thu hồi trong UT-03. Một công ty được cấp quyền không phụ thuộc jobId sau khi duyệt; jobId là ngữ cảnh giải trình và phải thuộc công ty khi gửi.
- Chỉ ứng viên PUBLIC mới được xin mở khi chưa có lịch sử ứng tuyển/quyền được duyệt. Xác minh qua API nội bộ tối thiểu; không tạo API tìm kiếm DN-03. Quyền đã cấp không tự mất khi hồ sơ chuyển riêng tư.
- Danh sách dùng PageResponse và page/size hiện có, size tối đa 100; sắp mới nhất trước, thêm id làm tie-breaker. from/to là ISO-8601 có offset, lọc UTC `[from,to)`, from>=to trả 400. FE dùng múi giờ Asia/Ho_Chi_Minh và đổi ngày kết thúc sang đầu ngày tiếp theo.
- Không ghi log cho xem trạng thái, gửi/duyệt yêu cầu, tải danh sách hay lần gọi `/contact` bị 403/lỗi phụ thuộc.

## Ghi nhật ký và lỗi

Luồng `/contact`: xác thực HR/công ty → xác định quyền/căn cứ → lấy dữ liệu liên hệ qua client nội bộ → transaction ghi log và commit → trả response. Nếu không lấy được dữ liệu hoặc commit log thất bại, trả lỗi, không trả email/phone. Không coi ứng viên không có phone là lỗi; phone có thể null thật.

Mỗi request HTTP thành công sinh đúng một log. HR chủ động xem lần nữa hoặc gọi API lại sinh log mới; không deduplicate theo ngày/phiên. Ghi log thể hiện server đã phục vụ dữ liệu, không thể xác nhận người dùng đã đọc hoặc response đã tới trình duyệt.

`Cache-Control: no-store` cho endpoint liên hệ và lịch sử riêng tư. FE không lưu contact vào localStorage, persisted store hoặc cache dùng chung; không tự retry GET `/contact`, không prefetch hay gọi trong render/effect. Chống double click khi request đang chạy. Khi đổi ứng viên/công ty, đăng xuất hoặc rời trang, bỏ dữ liệu contact trong state.

## FE

Tái sử dụng component/UI và tuân thủ `DESIGN.md`, `web/AGENTS.md`, layout theo vai trò; trạng thái loading, empty, error và responsive đầy đủ.

HR:

- Component `CandidateContactPanel` nhận userId/jobId, đọc trạng thái quyền mà không lấy liên hệ.
- Chưa đủ quyền: “Liên hệ: đã ẩn”, **Xin mở liên hệ**, dialog chọn JD của công ty nếu chưa có ngữ cảnh và nhập lý do. Đang PENDING: hiển thị chờ duyệt và khóa gửi trùng. REJECTED: hiển thị lý do, cho gửi lại.
- Đủ quyền: **Xem liên hệ**; bấm mới gọi `/contact`, hiển thị email/phone; có hành động **Xem lại** nếu muốn lấy lại dữ liệu và sinh log mới. Không có phone thì hiển thị “Chưa cập nhật”.
- Gắn component vào chi tiết đơn HR đang có. Thẻ ứng viên hiện có nếu có chỗ phù hợp dùng cùng component; không tự tạo danh sách tìm ứng viên DN-03.
- Bổ sung trang `/hr/candidates/[userId]/contact` làm điểm truy cập UT-03 cho ứng viên chưa nộp đơn. Chỉ hiển thị metadata tối thiểu của ứng viên PUBLIC và contact panel, không tìm kiếm/xếp hạng. Các màn tương lai có thể dẫn vào trang này; tài liệu demo cung cấp URL userId fixture.

STAFF:

- `/staff/contact-requests`: bảng yêu cầu, tab/bộ lọc PENDING/APPROVED/REJECTED, xem lý do, duyệt hoặc từ chối có lý do. Refresh sau thao tác, xử lý 409 khi người khác đã duyệt.
- `/staff/contact-access-logs`: bảng từng lần xem, bộ lọc công ty/ứng viên/từ ngày/đến ngày, phân trang; hiển thị nguồn “Đã ứng tuyển” hoặc “Yêu cầu được duyệt”.

Ứng viên:

- `/candidate/contact-access-logs`: danh sách công ty cùng các lần xem, thời gian và nguồn truy cập. Nhóm log theo công ty trong trang hiện tại; không hiển thị tổng số hoặc lần cuối toàn bộ lịch sử nếu API chỉ trả một trang. Không cho query userId người khác.
- Thêm navigation phù hợp cho STAFF và ứng viên.

## Kiểm chứng và bàn giao

- Integration/API: chưa đủ quyền → 403 CONTACT_LOCKED, không log; tất cả DTO HR ngoài `/contact` không chứa email/phone thật, kể cả dữ liệu lồng nhau.
- Ứng viên nộp JD công ty A → HR A xem được, HR B không; HR khác cùng A xem được; WITHDRAWN/REJECTED vẫn xem được.
- PENDING/REJECTED chưa cấp quyền; APPROVED cấp quyền toàn công ty; JD công ty khác không dùng để gửi yêu cầu.
- Hai lần `/contact` thành công → hai log đúng hrId/companyId/userId/via. Nếu có cả hai căn cứ → APPLICATION. Lỗi lấy liên hệ hoặc ghi DB → không trả dữ liệu.
- Gửi trùng/duyệt đồng thời, lý do trống/quá dài, phân quyền endpoint STAFF, lọc ngày và phân trang, ứng viên không đọc được log người khác.
- E2E: HR xin mở → STAFF duyệt → HR bấm xem → ứng viên thấy công ty và lần xem; kèm nhánh từ chối, chờ duyệt, lỗi và responsive desktop/tablet/mobile.
- Chạy kiểm thử backend đúng module và các module bị ảnh hưởng; FE type-check, lint, build và E2E trọng tâm theo môi trường repo. Chuẩn bị seed hai công ty, nhiều HR, ứng viên có/chưa ứng tuyển và yêu cầu đủ ba trạng thái.
- Cập nhật OpenAPI/hướng dẫn UT-03: migration, endpoint, demo, cách chạy, kết quả kiểm chứng và giới hạn CV/nội dung tự do. PR `[UT-03] ...` vào `test`; không merge tự động.

## Phần giao người triển khai quyết định

Tên class/DTO, thứ tự migration, truy vấn batch metadata để tránh N+1, bố cục chi tiết theo design system và cách tổ chức test theo convention hiện tại. Không thêm service, hàng đợi, kho cache hoặc thư viện mới chỉ cho UT-03.
