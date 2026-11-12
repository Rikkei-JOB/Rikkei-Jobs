# UT-01: yêu cầu và cơ sở lập kế hoạch

Nguồn: mô tả UT-01, HS-04, DN-04 do người dùng cung cấp trong cuộc hội thoại. Tài liệu này lưu yêu cầu và các đề xuất phục vụ kế hoạch; không coi các đề xuất chưa được trả lời là quyết định đã được duyệt.

## 1. Kết quả cần đạt

Ứng viên chọn một CV để ứng tuyển JD, theo dõi trạng thái và lịch sử, rút đơn trước OFFER. HR xem đúng PDF tại lần nộp. Bàn giao đủ DB, BE, FE; PR vào `test`, tiêu đề bắt đầu `[UT-01]`. Người dùng đã xác nhận nhóm dùng `test` để phát triển, thay cho nhánh develop trong mô tả task ban đầu.

## 2. Yêu cầu gốc

- `applications(id, job_id, user_id, cv_id, cv_snapshot_url, cover_letter, status, created_at, updated_at)`; mỗi ứng viên chỉ có một đơn đang mở cho mỗi JD.
- `application_status_history(id, application_id, from_status, to_status, actor_id, note, created_at)`; ghi đủ mỗi lần đổi trạng thái.
- Tiến trình: `SUBMITTED → SCREENING → INTERVIEW → OFFER → HIRED`.
- Có thể `REJECTED` từ các bước trước HIRED. Có thể `WITHDRAWN` trước OFFER.
- POST `/jobs/{id}/apply` nhận `{cvId, coverLetter}`. JD phải APPROVED và còn hạn. Lưu bản PDF tại thời điểm nộp.
- GET `/me/applications?status=`; GET `/me/applications/{id}` kèm lịch sử.
- POST `/me/applications/{id}/withdraw` nhận `{reason}`.
- Có đơn mới thì thông báo/email cho HR công ty.
- FE: nút Ứng tuyển tại chi tiết JD, hộp chọn CV và thư giới thiệu; trang đơn của tôi có JD, công ty, ngày nộp, nhãn trạng thái có màu, tiến trình, lịch sử và nút Rút.
- Tiêu chí: nộp trùng trả 409; rút rồi nộp lại được; JD hết hạn bị chặn; sửa CV không đổi PDF đã nộp; OFFER không được rút; đủ lịch sử.
- **Người dùng đã xác nhận:** ứng viên được nộp lại cùng JD sau REJECTED, nếu JD vẫn đủ điều kiện nhận hồ sơ và không có đơn khác đang chặn nộp mới.

## 3. Ràng buộc từ HS-04

- HS-04 sở hữu `cvs`, `cv_sections`, CRUD CV và xuất PDF, tối đa 5 CV mỗi ứng viên.
- PDF được xuất từ dữ liệu hồ sơ ở thời điểm xuất; sửa hồ sơ thì PDF xuất lần sau thay đổi.
- UT-01 không sao chép lại các bảng hồ sơ, không tự viết renderer PDF, không sửa giới hạn 5 CV.
- Lấy PDF bằng backend có xác thực, kiểm tra CV thuộc ứng viên; lưu chính các bytes đó thành snapshot riêng.
- Snapshot không phụ thuộc vòng đời CV gốc; không có FK xuyên service hoặc cascade từ CV sang đơn.
- QR bên trong PDF vẫn đi đến `/p/{public_token}`. Trang đích tuân thủ trạng thái PUBLIC hiện tại, chỉ hiển thị dữ liệu được HS-04 cho phép. Lưu PDF không đóng băng quyền công khai của trang QR.

## 4. Ràng buộc từ DN-04

- DN-04 sở hữu tìm/lọc việc, sắp xếp, chi tiết JD, dữ liệu công ty và `job_views`; UT-01 cung cấp component ứng tuyển để gắn vào trang đó.
- Danh sách DN-04 chỉ hiện JD APPROVED, chưa hết hạn, công ty không bị khoá.
- UT-01 kiểm tra lại điều kiện tại lúc nộp; đề xuất áp dụng cả điều kiện công ty không bị khoá để thống nhất hai luồng.
- Không dùng GET chi tiết công khai có ghi view làm API kiểm tra nghiệp vụ; nộp đơn hoặc xem đơn không được tự tăng `job_views`.
- Điểm khớp DN-06 và các bộ lọc DN-04 không thuộc UT-01.

## 5. Đề xuất dùng để lập kế hoạch

Các mục dưới đây là lựa chọn đề xuất, cần được xem cùng kế hoạch trước triển khai:

1. **Nộp lại:** cho phép sau WITHDRAWN hoặc REJECTED; phần REJECTED đã được người dùng xác nhận ở mục 2. Sau HIRED vẫn chặn theo đề xuất hiện tại. Ràng buộc unique bỏ qua đơn WITHDRAWN/REJECTED, các trạng thái còn lại chặn thêm đơn cùng ứng viên/JD.
2. **Nộp lại tạo bản ghi mới:** giữ nguyên đơn, PDF và lịch sử cũ; đơn mới bắt đầu SUBMITTED, xuất PDF mới từ CV được chọn ở lần nộp này. Không đổi đơn cũ REJECTED/WITHDRAWN về SUBMITTED và không thêm transition mở lại vào lịch sử đơn cũ.
3. **Hạn nộp:** chỉ hợp lệ khi `deadline > now` với Instant UTC. Nếu DN-02 dùng ngày không kèm giờ, DN-04 phải chuẩn hoá thành thời điểm đóng nhận theo múi giờ nghiệp vụ; UT-01 không tự suy diễn.
4. **Luật chuyển bước:** tiến từng bước, không lùi/bỏ bước; terminal không đổi tiếp. UT-02 sẽ xác nhận dùng chung trước khi bổ sung API HR.
5. **Dữ liệu bổ sung:** lưu `company_id`, `job_title`, `company_name` lúc nộp để phân quyền HR và hiển thị đơn kể cả khi JD không còn công khai. Chúng là metadata của đơn, không sao chép toàn bộ JD.
6. **Giới hạn nhập:** thư giới thiệu không bắt buộc, tối đa 5.000 ký tự; lý do rút bắt buộc sau trim, tối đa 1.000 ký tự. Đây là giới hạn đề xuất, không phải giá trị trong đề bài.
7. **Thông báo:** thực hiện email cho HR đang hoạt động thuộc công ty, qua outbox nhỏ có retry. Không xây trung tâm thông báo hoặc UI HR trong UT-01. Địa chỉ nhận do company-service cung cấp qua hợp đồng nội bộ có xác thực.
8. **Quyền đọc lịch sử:** ứng viên xem mọi lần đổi trạng thái và lý do rút của mình; chỉ trả `note` khi `to_status = WITHDRAWN` và `actor_id = user_id` của đơn, các transition khác trả note null. Ghi chú tuyển dụng của HR mặc định không trả cho ứng viên, cần thống nhất với UT-02 nếu muốn công khai.
9. **Lưu file:** adapter filesystem riêng tư với volume bền vững cho dev/demo; đường đọc kiểm tra JWT và chủ đơn/công ty. Không public bucket hoặc đưa file vào `web/public`. Có thể thay adapter bằng object storage sau này.

## 6. Hiện trạng repo đã kiểm tra

- Backend: Java 21, Spring Boot 3.3.4, Spring Data JPA, PostgreSQL, JWT và lỗi dùng `common-core`; `recruitment-service` hiện chỉ có ping và test xác thực.
- Profile đã có HS-01 và Flyway schema `profile`; chưa thấy CV HS-04 hoặc JD DN-04 trong checkout tại thời điểm lập kế hoạch.
- Gateway hiện hỗ trợ `/api/v1/recruitment/**`, strip `/api/v1`; plan dùng prefix này thay vì thêm route mơ hồ `/api/v1/jobs/**` giữa hai service.
- FE: Next.js 16, React 19, TypeScript, Tailwind, Axios và auth store/guard có sẵn. Chưa có trang JD hoặc CV.
- CI hiện bắt `main`, `test`, dùng Node 20 và bỏ qua lỗi type-check; giữ nhánh đích `test` đã xác nhận và bổ sung kiểm tra thực sự khi bàn giao task này.

## 7. Phạm vi loại trừ

Không triển khai lại HS-04/DN-04/DN-06, không làm API chuyển bước HR của UT-02, không tự tạo tài khoản HR sản xuất, không merge/push/PR trong bước lập kế hoạch. Test double chỉ hỗ trợ phát triển độc lập, không thay thế nghiệm thu tích hợp với HS-04 và DN-04 thật.
