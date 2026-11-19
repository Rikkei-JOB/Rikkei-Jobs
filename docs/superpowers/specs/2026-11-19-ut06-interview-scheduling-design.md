# UT-06 — Đặt lịch phỏng vấn

Trạng thái: bản thiết kế chờ duyệt. Chưa triển khai mã sản phẩm.

## Mục tiêu và phạm vi

HR đề xuất giờ cho vòng UT-02, ứng viên chọn một giờ, hệ thống tạo phòng Rikkei Meet, gửi lịch và nhắc lịch. HR xem lịch tuần toàn công ty và lọc theo người phỏng vấn. Bàn giao đủ migration, BE Spring Boot, FE Next.js, tài liệu API và kiểm chứng; PR vào `test`, tiêu đề bắt đầu `[UT-06]`.

Không mở rộng sang đặt lịch định kỳ, đồng bộ Google/Outlook hay giữ chỗ tạm cho khung OPEN. File ICS là phương tiện thêm/cập nhật/huỷ sự kiện trên lịch cá nhân.

## Các phương án đã cân nhắc

1. Gọi Meet và gửi email trực tiếp trong request: ít thành phần nhưng lỗi mail làm kết quả đặt lịch khó xác định.
2. Gọi Meet khi chọn giờ; lưu lịch và email outbox trong cùng transaction: tái sử dụng cơ chế outbox của recruitment, tách lỗi gửi mail khỏi kết quả đặt lịch. Người dùng đã chọn phương án này.

## Quy tắc đã thống nhất

- Mỗi lần đề xuất gồm 1–5 khung; mỗi khung dài ít nhất 30 phút, bắt đầu trong tương lai. Danh sách có lỗi bị từ chối toàn bộ với 400.
- Các khung trong cùng danh sách không được chồng lấn. Khoảng giờ dùng quy ước [start, end), cho phép hai buổi nối tiếp.
- OPEN chưa giữ chỗ. Khi chọn, kiểm tra xung đột với lịch SCHEDULED của cả ứng viên và người phỏng vấn, kể cả ở vòng/đơn/công ty khác mà hệ thống quản lý. Xung đột trả 409, không tiết lộ chi tiết lịch khác.
- HR gửi khung mới hợp lệ thì huỷ lịch cũ ngay; ứng viên phải chọn lại. Đề xuất mới không hợp lệ giữ nguyên lịch cũ.
- Xác nhận, huỷ lịch và nhắc gửi cho ứng viên cùng interviewerEmail; chuẩn hoá và loại trùng email. Lời mời chọn giờ chỉ gửi ứng viên.
- HR xem lịch toàn công ty, có lọc người phỏng vấn. Không lấy companyId từ client để quyết định quyền.
- Job mỗi 15 phút: mốc 24h áp dụng khi 1h < thời gian còn lại <= 24h; mốc 1h khi 0 < thời gian còn lại <= 1h. Chạy bù chỉ gửi mốc hiện tại, không gửi dồn hai mốc.
- Khi đặt lịch còn <= 1h, email xác nhận thay nhắc 1h. Lịch huỷ, DONE hoặc đã bắt đầu không nhận nhắc.

## Các quyết định kỹ thuật đề xuất

Các mục sau là lựa chọn của người thiết kế, chờ người dùng duyệt cùng tài liệu.

### DB và trạng thái

Thêm migration trong schema recruitment, giữ lịch sử thay vì xoá:

- interview_slots: id, round_id FK, start_at/end_at TIMESTAMPTZ, status OPEN/PICKED/CANCELLED, proposal_id, created_at. CHECK thời lượng >= 30 phút. Điều kiện tương lai kiểm tra ở service với Clock, không dùng CHECK phụ thuộc thời gian hiện tại.
- interview_schedules: id, round_id FK, slot_id FK UNIQUE, meet_url, status SCHEDULED/DONE/CANCELLED, reminded_24h_at/reminded_1h_at nullable, created_at/cancelled_at, reminder_1h_suppressed. Lưu snapshot candidate_id, candidate_email và interviewer_email phục vụ kiểm tra trùng và thông báo nhất quán.
- UNIQUE có điều kiện cho phép tối đa một lịch SCHEDULED mỗi round. Bảo đảm slot thuộc chính round của schedule bằng FK ghép hoặc ràng buộc tương đương.
- interview_notification_outbox: sự kiện, schedule_id hoặc proposal_id, recipient, payload snapshot, trạng thái PENDING/PROCESSING/SENT/FAILED/CANCELLED, attempts, next_attempt_at, lease_until, sent_at. Khoá duy nhất (event identity, loại thông báo, email chuẩn hoá).
- Index phục vụ slot theo round/status, lịch theo start/end thông qua slot, candidate_id/interviewer_email/status, outbox status/next_attempt_at.

Lịch SCHEDULED là nguồn chính; đồng bộ interview_rounds.scheduled_at khi chọn, xoá khi huỷ. Bỏ nhập giờ trực tiếp trên form tạo vòng mới. Dữ liệu scheduled_at cũ vẫn hiển thị là “lịch dự kiến”, không tự tạo phòng/nhắc hoặc coi là lịch UT-06 vì thiếu giờ kết thúc và sự xác nhận.

DONE không đồng nghĩa PASS/FAIL. Giữ luồng kết quả UT-02; khi ghi kết quả PASS/FAIL, chuyển lịch đang SCHEDULED thành DONE. Khi trả kết quả về PENDING, không tự mở lại lịch đã DONE; đặt lại qua reschedule nếu cần.

### Phân quyền và vòng đời

Tái sử dụng JWT, vai trò, company ownership và application lock UT-02. HR đề xuất/đổi lịch trong công ty mình; ứng viên chỉ xem/chọn lịch của đơn mình. Sai vai trò 403; tài nguyên ngoài phạm vi sở hữu 404. Chỉ đặt/đổi khi application INTERVIEW và round PENDING; trường hợp trạng thái không còn hợp lệ trả 409.

Khi đơn rời INTERVIEW, huỷ các khung OPEN và lịch SCHEDULED, dừng email nhắc chưa gửi và ghi thông báo huỷ. Thao tác này phải cùng cơ chế khoá với UT-06. Trở lại INTERVIEW không tự khôi phục lịch.

### API và tính đồng thời

Các đường dẫn dưới đây là đường dẫn nghiệp vụ, được đặt dưới prefix recruitment/gateway hiện hành:

| API | Hành vi |
| --- | --- |
| POST /rounds/{roundId}/slots | Body mảng {startAt,endAt}; tạo đề xuất đầu tiên. Nếu đã có đề xuất/lịch trước đó, 409 và dùng reschedule. Trả 201 với proposalId và slots. |
| GET /me/interviews | Phân trang theo quy ước repo; trả công ty, JD, vòng, người phỏng vấn, khung còn hiệu lực và lịch hiện tại. Khung hết giờ không cho chọn. |
| POST /me/interviews/{roundId}/pick | Body {slotId}; trả 200 với schedule, meetUrl và thời gian. Slot không thuộc vòng hoặc ngoài quyền trả 404; CANCELLED, hết giờ, vòng không hợp lệ hoặc trùng lịch trả 409. |
| POST /rounds/{roundId}/reschedule | Body mảng khung mới; áp dụng cả khi đang chờ chọn hoặc đã đặt. Kiểm tra toàn bộ trước khi huỷ lịch/khung cũ; huỷ và tạo đề xuất mới trong cùng transaction; trả 200. |
| GET /companies/me/interviews?from=&to= | Khoảng ISO có offset, from < to; lấy lịch giao với [from,to), phân trang theo quy ước repo; thêm bộ lọc interviewerEmail. Trả đủ candidate, job, round, giờ, status và meetUrl. |

Lặp pick cùng slot đã PICKED của lịch hiện tại trả lại cùng lịch, không gọi Meet hoặc gửi email mới. Slot PICKED của lịch cũ đã huỷ trả 409.

Trong transaction: lấy application lock theo chuẩn UT-02, round lock, rồi khoá theo tài nguyên ứng viên và email người phỏng vấn bằng PostgreSQL transaction advisory locks với thứ tự ổn định. Kiểm tra overlap sau khi có khoá; chỉ khoá round không đủ chống trùng ở hai vòng khác nhau. Mọi đường ghi lịch phải tuân cùng quy tắc khoá; builder kiểm tra thứ tự với đường cập nhật kết quả/chuyển trạng thái để tránh deadlock.

Giữ khoá trong lúc gọi Meet với timeout ngắn có cấu hình; không tự retry không giới hạn. Mock hiện trả roomUrl; adapter chuyển thành meetUrl/meet_url. Bổ sung Idempotency-Key theo slotId: cùng key/payload trả cùng phòng, cùng key khác payload trả 409; dữ liệu idempotency mock tồn tại theo vòng đời tiến trình như fixture phòng hiện tại. Lỗi/timeout Meet trả 503, transaction rollback và các khung còn nguyên. Nếu đã tạo phòng nhưng DB rollback, lần chọn lại slot tái sử dụng phòng. Không thêm API xoá phòng vì hợp đồng mock chưa có; phòng thừa không được công bố cho ứng viên.

Huỷ lịch ở UT-06 chỉ thu hồi khả năng truy cập qua giao diện ứng dụng và huỷ lời mời; không cam kết vô hiệu hoá URL Meet đã gửi nếu nhà cung cấp chưa hỗ trợ.

### Email, ICS và nhắc lịch

Outbox tạo cùng transaction với dữ liệu nghiệp vụ, một bản ghi mỗi người nhận. Worker tái sử dụng cách claim/lease/retry của repo; trước gửi nhắc kiểm tra lịch còn hợp lệ và mốc chưa quá hạn. Huỷ các reminder chưa gửi khi reschedule; email xác nhận cũ còn chờ cũng bị huỷ. Gửi email huỷ cho người có xác nhận SENT hoặc đã từng được worker bắt đầu gửi, kể cả trường hợp chưa biết SMTP đã nhận hay chưa; không chỉ dựa vào SENT vì worker có thể dừng sau khi SMTP nhận mail. Worker và luồng huỷ phối hợp để không bắt đầu gửi xác nhận cũ sau khi huỷ; trong vận hành bình thường, hoàn tất lượt gửi xác nhận đang chạy trước lượt gửi huỷ. SMTP không bảo đảm thứ tự tới hộp thư qua sự cố tiến trình; email đã được SMTP nhận không thể thu hồi. ICS CANCEL với cùng UID/SEQUENCE tăng giúp ứng dụng lịch nhận biết bản cập nhật mới hơn.

ICS xác nhận dùng METHOD:REQUEST; huỷ dùng METHOD:CANCEL với cùng UID sự kiện và SEQUENCE tăng. Mỗi schedule mới có UID riêng, UTC DTSTART/DTEND, ORGANIZER người phỏng vấn, ATTENDEE hai bên và URL Meet. Escape/fold nội dung theo chuẩn ICS.

Job tạo reminder outbox bằng unique key, an toàn khi nhiều instance chạy. Hai trường reminded_*_at chỉ ghi khi toàn bộ người nhận của mốc đã gửi thành công; thành công từng người lưu ở outbox, không gửi lại người đã SENT. Khi xác nhận thay nhắc 1h, lưu reminder_1h_suppressed riêng, không giả lập thời điểm gửi nhắc. Retry có giới hạn, FAILED có log để xử lý lại bằng cách vận hành hiện có.

Giới hạn: outbox bảo đảm không tạo trùng yêu cầu và không gửi lại hàng đã SENT khi job chạy lại bình thường. SMTP không có khoá idempotency nên vẫn có cửa sổ gửi trùng khi SMTP nhận mail nhưng worker dừng trước khi ghi SENT; Message-ID ổn định không bảo đảm hộp thư khử trùng. Yêu cầu tuyệt đối một lần qua mọi sự cố cần nhà cung cấp mail hỗ trợ idempotency, ngoài hạ tầng hiện tại. Test nghiệm thu xác minh một email/người/mốc khi job lặp và chạy đồng thời.

### FE

- HR: trong khối vòng UT-02 có danh sách 1–5 dòng bắt đầu/kết thúc, thêm/xoá dòng, validation tại chỗ, gửi đề xuất hoặc đổi lịch. Đổi lịch báo rõ lịch cũ sẽ bị huỷ ngay. Hiển thị đang chờ chọn, lịch đã xác nhận, đã hoàn thành/huỷ và tình trạng gửi thông báo nếu có dữ liệu.
- HR lịch tuần: mặc định tuần hiện tại từ thứ Hai, nút tuần trước/sau/hôm nay, lọc interviewerEmail, lịch SCHEDULED và DONE; trạng thái huỷ qua bộ lọc. Hiện ứng viên, vị trí, vòng và người phỏng vấn. Có loading, empty, lỗi và thử lại.
- Ứng viên: thẻ lời mời theo vòng, giờ và người phỏng vấn; chọn một khung, xác nhận, khoá nút khi gửi. Khi 409 tải lại dữ liệu và giải thích khung không còn khả dụng. Chỉ hiện nút vào phòng cho lịch SCHEDULED hiện tại, mở link an toàn ở tab mới.
- API lưu UTC, bắt buộc offset ở input; UI hiển thị Asia/Ho_Chi_Minh và ghi rõ múi giờ. FE phải lấy đủ các trang dữ liệu của tuần, không bỏ sót lịch vì phân trang.

## Kiểm chứng và bàn giao

- DB/API: 0/6 khung, quá khứ, <30 phút, thiếu offset, trùng/chồng khung trả 400; đúng 30 phút và các buổi nối nhau hợp lệ.
- Quyền: ứng viên khác, HR khác công ty, sai role không xem/chọn/đổi được.
- Pick thành công có meetUrl, một lịch active, các khung khác CANCELLED; email hai bên có ICS. Pick khung huỷ 409, pick lặp cùng slot không tạo thêm phòng/mail.
- Test PostgreSQL cho hai pick cùng vòng, hai vòng chung ứng viên/người phỏng vấn, pick đua reschedule; chỉ kết quả hợp lệ được commit.
- Meet timeout/503, tạo phòng xong DB rollback, retry cùng slot; không công bố lịch chưa commit.
- Reschedule hợp lệ huỷ lịch cũ ngay; input lỗi giữ nguyên lịch; không nhắc lịch cũ, ICS huỷ đúng UID.
- Clock cố định kiểm tra biên 24h/1h/0h, đặt dưới 1h, job chạy lại/nhiều instance, retry mail từng người, loại trùng email và lịch huỷ/DONE.
- Lịch HR đúng công ty, người phỏng vấn, tuần và buổi đi qua ranh giới from/to; FE đổi tuần, chọn giờ và vào phòng phản ánh API thật.
- Chạy kiểm tra backend/frontend theo repo; cập nhật OpenAPI, request mẫu, hướng dẫn chạy/mock/mail và dữ liệu demo. PR vào test với tiêu đề `[UT-06] Đặt lịch và nhắc phỏng vấn`.

## Phần giao cho builder

Chọn tên lớp/component, migration version tiếp theo, tham số batch/retry/timeout, bố trí UI theo component hiện có và cách biểu diễn response phù hợp envelope repo. Không thêm thư viện lịch hoặc mail provider mới nếu chưa cần. Sau khi thiết kế được duyệt mới lập kế hoạch triển khai.
