# UT-07 — Offer, phản hồi và ghi nhận việc làm

## Ý định

HR gửi đề nghị làm việc sau khi ứng viên vượt qua vòng phỏng vấn cuối. Ứng viên xem đủ điều kiện và hạn trả lời, rồi nhận hoặc từ chối. Khi nhận, đơn thành `HIRED` và công việc xuất hiện ngay trong “Hồ sơ của tôi”; HR xác nhận ứng viên đã đi làm sau đó.

## Mục tiêu, giới hạn và điều cần tránh

- Giao đủ migration, backend, frontend, OpenAPI, dữ liệu mẫu, hướng dẫn chạy, ảnh màn hình và PR `[UT-07] ...` vào **`test`** theo xác nhận của người dùng và quy ước repo.
- Không gửi offer khi đơn chưa `INTERVIEW`, còn vòng `PENDING`, không có vòng nào, hoặc vòng có `round_no` lớn nhất không `PASS`: trả `409`.
- Ứng viên khác không xem hoặc thao tác offer: trả `404` như offer không tồn tại. HR công ty khác cũng nhận `404`.
- Không tự thay đổi các đơn khác khi một offer được nhận. Ứng viên tự rút đơn ở các bước trước `OFFER`; với đơn đã `OFFER`, ứng viên dùng “Từ chối offer” và nhập lý do.
- Không sửa hoặc gửi lại offer trong phạm vi này; một đơn có tối đa một offer. Không đưa lương lên mục kinh nghiệm.
- Nội dung và lương offer chỉ hiển thị cho ứng viên sở hữu hoặc HR đúng công ty; quyền đọc đơn của STAFF/ADMIN từ UT-02 không tự mở quyền đọc offer.
- Không mở rộng hồ sơ công khai `/profile/[userId]` trong UT-07; mục kinh nghiệm mới xuất hiện trên `/candidate/profile`.

## Ràng buộc hiện có

- UT-02 đã có `OfferEligibilityPolicy`, vòng phỏng vấn, khóa hàng ứng tuyển, lịch sử trạng thái và outbox một email cho mỗi lần chuyển trạng thái. Endpoint chuyển trạng thái HR chỉ cho các bước UT-02; UT-07 dùng endpoint nghiệp vụ riêng.
- `recruitment-service` sở hữu đơn và trạng thái; `profile-service` sở hữu thông tin cá nhân. Trang “Hồ sơ của tôi” hiện ghép nhiều thành phần, nên có thể đọc thêm kinh nghiệm từ recruitment mà không ghi chéo hai database.
- Lịch sử và outbox trạng thái được tạo cùng transaction. Email offer phải dùng chính event `INTERVIEW -> OFFER`, không tạo thêm email trạng thái thứ hai.
- API đi qua gateway `/api/v1/recruitment`; vai trò và công ty lấy từ JWT, không lấy `companyId` hoặc `userId` từ request.

## Phương án đã cân nhắc

1. **Đã chọn: recruitment sở hữu `offers` và `employment_records`; FE ghép dữ liệu hồ sơ.** Nhận offer cập nhật hai bảng và đơn trong cùng transaction. Trang hồ sơ gọi thêm API recruitment để hiển thị kinh nghiệm. Dễ giữ nhất quán, ít phụ thuộc giữa service.
2. Profile sở hữu `employment_records`, recruitment gửi sự kiện hoặc HTTP sau khi nhận. Hồ sơ đọc tập trung hơn nhưng cần đồng bộ, retry và xử lý khoảng thời gian đơn đã `HIRED` mà kinh nghiệm chưa xuất hiện.

Đối với đơn khác đã `OFFER`, đã chọn hành động từ chối thay vì mở thêm `OFFER -> WITHDRAWN`: offer và đơn có trạng thái kết thúc nhất quán, đồng thời HR nhận được lý do. Với lương, đã chọn một số nguyên VND/tháng, **gross**, thay vì thêm loại lương và kỳ trả. UI, API và email phải ghi rõ đơn vị và gross.

## Dữ liệu

- Migration mới trong schema `recruitment` tạo `offers`: `id`, `application_id` FK và UNIQUE, `position`, `salary BIGINT CHECK (salary > 0)`, `start_date DATE`, `work_location`, `note` nullable, `response_deadline TIMESTAMPTZ`, `status` (`SENT`, `ACCEPTED`, `DECLINED`, `EXPIRED`), `responded_at` nullable, `decline_reason` nullable, `created_at`. Lương gửi/nhận qua JSON là số nguyên VND/tháng gross. Không tự sao chép lương từ JD.
- Tạo `employment_records`: `id`, `application_id` FK và UNIQUE để truy nguyên và chống tạo trùng, `user_id`, `company_id`, `job_id`, `position`, `start_date`, `confirmed_at` nullable. `company_name` cho API hồ sơ lấy từ snapshot trong `applications` qua `application_id`.
- Index cho danh sách offer theo `applications.user_id`, cho job quét `offers(status, response_deadline)` và danh sách kinh nghiệm theo `employment_records.user_id`.
- `responded_at` chỉ ghi khi ứng viên nhận hoặc từ chối; offer quá hạn không có phản hồi của ứng viên. Lý do từ chối chỉ tồn tại khi `DECLINED`. `confirmed_at` chỉ được ghi sau khi offer `ACCEPTED` và đơn `HIRED`.

## Luồng trạng thái và giao dịch

1. **Gửi:** HR sở hữu công ty của đơn, khóa hàng `applications`, xác nhận `INTERVIEW` và điều kiện vòng cuối từ UT-02. Kiểm tra `responseDeadline` là thời điểm tương lai có offset thời gian, `startDate` là ngày hợp lệ, các chuỗi không rỗng, lương nguyên dương. Tạo offer `SENT`, chuyển đơn sang `OFFER`, ghi lịch sử và outbox trong cùng transaction. Trùng offer hoặc trạng thái đổi đồng thời trả `409`.
2. **Nhận:** API kiểm tra chủ offer trước, khóa đơn rồi đọc/khóa offer. Chỉ `SENT` và `now < response_deadline` được nhận; hạn đã tới hoặc trạng thái khác trả `409`. Cùng transaction: `ACCEPTED`, `responded_at`, đơn `HIRED`, lịch sử/outbox và đúng một `employment_record`. Khóa và UNIQUE chặn hai lần nhận tạo bản ghi trùng.
3. **Từ chối:** lý do bắt buộc, sau khi trim dài 1–1.000 ký tự. Cùng điều kiện chủ sở hữu, trạng thái và thời hạn như nhận. Cùng transaction: offer `DECLINED`, `responded_at`, `decline_reason`, đơn `REJECTED`, lịch sử/outbox. HR đọc lý do trong chi tiết đơn.
4. **Quá hạn:** job mỗi giờ quét offer `SENT` có `response_deadline <= now`, xử lý theo lô với khóa để nhiều instance không xử lý trùng. Cùng transaction: offer `EXPIRED`, đơn `REJECTED`, lịch sử/outbox. API nhận/từ chối tự kiểm tra hạn theo đồng hồ server nên không có khoảng một giờ cho phép trả lời muộn, kể cả khi job chưa chạy. Đồng hồ và instant được dùng nhất quán; đếm ngược FE chỉ để hiển thị.
5. **Xác nhận đi làm:** HR đúng công ty, offer `ACCEPTED` và đơn `HIRED` mới ghi `confirmed_at` theo đồng hồ server. Đây là thời điểm HR xác nhận, không phải ngày đi làm thực tế; `start_date` vẫn là ngày dự kiến trong offer. API không nhận ngày từ HR. Lần gọi lặp trả bản ghi đã xác nhận, không đổi timestamp. Không tạo thêm `employment_record` hoặc chuyển trạng thái đơn lần nữa.

## API và quyền

Tất cả đường dẫn dưới `/api/v1/recruitment`:

| API | Quyền | Kết quả chính |
| --- | --- | --- |
| `POST /applications/{id}/offer` | HR đúng công ty | Nhận `{position,salary,startDate,workLocation,responseDeadline,note?}`; tạo offer và chuyển đơn sang `OFFER`; sai điều kiện `409`. |
| `GET /applications/{id}` | Quyền hiện có của UT-02 | Chi tiết của HR đúng công ty bổ sung offer, trạng thái, hạn và lý do từ chối; STAFF/ADMIN tiếp tục đọc dữ liệu đơn hiện có nhưng không nhận nội dung offer. HR sai công ty vẫn `404`. |
| `POST /applications/{id}/offer/confirm-onboard` | HR đúng công ty | Ghi và trả `confirmedAt`; chưa nhận offer `409`, sai công ty `404`. |
| `GET /me/offers` | Ứng viên | Các offer của chính mình, gồm trạng thái kết thúc, sắp mới nhất trước. |
| `GET /me/offers/{id}` | Ứng viên | Chi tiết một offer cho trang trực tiếp; ID của người khác `404`. |
| `POST /me/offers/{id}/accept` | Ứng viên | Nhận trước hạn; sau hạn hoặc đã phản hồi `409`; người khác `404`. |
| `POST /me/offers/{id}/decline` | Ứng viên | `{reason}` bắt buộc; lý do thiếu/rỗng `400`; sau hạn hoặc đã phản hồi `409`; người khác `404`. |
| `GET /me/employment-records` | Ứng viên | Kinh nghiệm của chính mình, kèm `companyName` từ snapshot đơn; không có lương. |

Các response dùng lỗi `{code,message}` theo quy ước chung. `responseDeadline` xuất JSON ISO 8601 có offset hoặc UTC `Z`; backend so sánh theo instant. Không nhận user/company từ client. OpenAPI cập nhật schema, bảo mật, lỗi và ví dụ cho tất cả endpoint mới.

## Email và giao diện

- Outbox trạng thái UT-02 tiếp tục là nguồn thông báo duy nhất. Với event sang `OFFER`, worker đọc offer để gửi email gồm vị trí, lương gross VND/tháng, ngày bắt đầu, địa điểm, hạn trả lời và đường dẫn đăng nhập xem offer. Retry dùng ID event hiện có; không gửi trực tiếp trong request.
- Trang chi tiết đơn HR có form gửi offer chỉ khi trạng thái và vòng phỏng vấn hợp lệ; backend vẫn kiểm tra lại. Sau khi gửi, hiển thị đầy đủ nội dung, trạng thái, thời điểm phản hồi và lý do từ chối. Nút “Xác nhận đã đi làm” chỉ xuất hiện khi `ACCEPTED` và chưa xác nhận.
- Trang `/candidate/offers` liệt kê offer; chi tiết `/candidate/offers/[id]` hiển thị nội dung, đếm ngược, nút “Nhận” và “Từ chối”. Form từ chối yêu cầu lý do. Khi hết hạn theo giờ client, vô hiệu hóa nút và tải lại trạng thái; backend quyết định cuối cùng và xử lý `409` nếu có race.
- Trang `/candidate/profile` có mục “Kinh nghiệm”, hiển thị “<vị trí> tại <công ty>” ngay sau khi nhận. Có thể hiện ngày bắt đầu và trạng thái xác nhận, nhưng không công bố lương. Dữ liệu đến từ `GET /me/employment-records`.

## Kiểm chứng và bàn giao

- Test service/API: vòng cuối chưa PASS, còn PENDING, sai trạng thái, HR sai công ty; ứng viên khác xem hoặc phản hồi `404`; nhận/từ chối trước và đúng/sau deadline; lý do từ chối; hai request đồng thời; job chạy lại; nhận tạo đúng một employment record; xác nhận đi làm lặp không đổi thời gian; các đơn khác giữ nguyên.
- Test email: gửi offer có đầy đủ nội dung và một event; retry không tạo event thứ hai. Test FE cho form HR, trang offer, lý do bắt buộc, đếm ngược, kinh nghiệm sau khi nhận và ảnh màn hình.
- PR `[UT-07] ...` base `test` có cách chạy, migration, dữ liệu mẫu cho offer `SENT`, `ACCEPTED`, `DECLINED`, `EXPIRED`, ảnh HR/ứng viên và liên kết OpenAPI cập nhật.

## Quyết định giao người triển khai

- Chọn giới hạn độ dài phù hợp cho vị trí, địa điểm và ghi chú, nhất quán giữa DB, validation và OpenAPI.
- Chọn mã lỗi cụ thể bên trong HTTP `400`/`404`/`409`, theo quy ước common-core.
- Chọn cấu trúc DTO và tên component FE phù hợp codebase; không đổi hành vi nêu trên.
