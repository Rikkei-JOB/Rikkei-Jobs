# UT-02 — HR xử lý đơn và vòng phỏng vấn

## Ý định

HR của doanh nghiệp sở hữu JD xử lý đơn theo các bước được phép, ghi nhận nhiều vòng phỏng vấn; STAFF xem đơn của mọi doanh nghiệp. Ứng viên nhận email cho mỗi lần đơn đổi trạng thái. UT-07 sở hữu thao tác gửi offer và xử lý bước sau OFFER.

## Mục tiêu và giới hạn

- Giao đủ migration, backend, frontend, OpenAPI, dữ liệu demo, ảnh màn hình và hướng dẫn chạy; unit test phủ mọi cặp chuyển trạng thái.
- HR công ty khác nhận 404 đối với đơn, CV, vòng phỏng vấn và thao tác ghi. STAFF chỉ xem trong UT-02, bao gồm mở/tải PDF CV đã nộp.
- Không tính lại công thức khớp trong UT-02, không thay DN-06 hoặc truy cập trực tiếp DB của identity/profile/company.
- Không đưa nút chuyển `INTERVIEW -> OFFER` hoặc `OFFER -> HIRED` vào API transition của UT-02. UT-07 gọi nghiệp vụ riêng.
- Không xoá vòng hoặc quay lại trạng thái trước; không sửa vòng sau khi đơn rời INTERVIEW.

## Ràng buộc của repo

- UT-01 đã có `recruitment.applications`, `application_status_history`, CV PDF snapshot, `ApplicationStatusPolicy`, `ApplicationTransitionService` khoá hàng và outbox email báo HR khi nộp đơn.
- Policy hiện cho phép các bước dành cho UT-07 và WITHDRAWN của ứng viên; UT-02 phải có tập bước HR riêng và không dùng policy chung như quyền gọi mọi transition.
- Outbox UT-01 unique theo `(application_id, recipient)`, không thể tái dùng nguyên trạng để gửi email theo từng lần đổi bước.
- Identity sở hữu tên/email ứng viên. Profile HS-01 sở hữu `verifiedScore`. DN-06 sở hữu điểm khớp JD–ứng viên và cần thống nhất quyền đọc kỹ năng với HS-01.
- `test` là nhánh tích hợp theo quy ước repo và xác nhận đã ghi trong tài liệu UT-01; đề bài ban đầu ghi `develop`. Trước khi tạo PR, đối chiếu lại với trưởng nhóm nếu quy ước nhánh đã thay đổi.

## Các cách đã cân nhắc

1. **Backend UT-02 tổng hợp dữ liệu — đã chọn.** Recruitment kiểm quyền đơn trước, rồi lấy tên/email ứng viên từ identity và điểm hiện tại từ DN-06; FE dùng một response chi tiết. Có thêm các lời gọi service, nhưng ranh giới 404 và dữ liệu hiển thị nhất quán.
2. FE gọi recruitment, identity và DN-06 riêng. Ít mã tổng hợp ở backend hơn, nhưng FE phải phối hợp quyền, lỗi và thời điểm tải của ba nguồn.

Điểm khớp **hiện tại** từ DN-06 đã được chọn. DN-06 tính từ `job_skills` và `verifiedScore`, không từ PDF CV. `match_scores(job_id, user_id)` là cache có thể đổi; UT-02 không diễn đạt nó như điểm tại lúc nộp. Nếu cần điểm lịch sử sau này, lưu snapshot riêng theo `application_id`.

## Dữ liệu và migration

- Thêm `recruitment.interview_rounds`: `id`, `application_id` FK, `round_no`, `name`, `interviewer_email`, `scheduled_at` nullable, `result` (`PENDING`, `PASS`, `FAIL`), `score` nullable, `note` nullable, `created_at`; unique `(application_id, round_no)` và index để lấy theo thứ tự.
- Thêm bảng lịch sử sửa vòng, liên kết `round_id` và `application_id`, lưu người sửa, thời điểm, giá trị cũ/mới của `result`, `score`, `note`. Lịch sử này không trộn với `application_status_history` vì đó là lịch sử bước của đơn. STAFF đọc được lịch sử sửa vòng.
- Thêm outbox riêng hoặc mở rộng outbox UT-01 theo khóa `status_history_id` để mỗi lần đổi trạng thái có một thông báo riêng. Tạo event trong cùng transaction với status và history; worker gửi với retry. Tránh gửi email trực tiếp trong transaction hoặc lặp email khi FE gọi lại.
- Không thêm `matchScore` vào bảng `applications`: điểm hiện tại lấy từ DN-06 theo `(job_id, user_id)`.

## Quy tắc nghiệp vụ

- Bảng/map chuyển bước HR: `SUBMITTED -> SCREENING | REJECTED`, `SCREENING -> INTERVIEW | REJECTED`, `INTERVIEW -> REJECTED`. Không cho quay lui, bỏ bước, tự chuyển sang OFFER/HIRED, hoặc chuyển từ terminal. Sai luật trả `409 INVALID_TRANSITION`.
- WITHDRAWN vẫn do API ứng viên UT-01 xử lý. UT-07 sở hữu `INTERVIEW -> OFFER` và `OFFER -> HIRED | REJECTED` qua nghiệp vụ riêng.
- Chỉ thêm vòng khi đơn đang INTERVIEW. `round_no` được cấp tuần tự dưới khoá của đơn để hai request đồng thời không trùng. Vòng mới bắt đầu PENDING; `scheduledAt` tùy chọn trong request tạo vòng.
- Chỉ sửa `result`, `score`, `note` của vòng khi đơn còn INTERVIEW. Cho phép sửa lại kết quả, kể cả đặt về PENDING, và ghi audit mỗi lần giá trị thực sự đổi. Không có API xoá vòng. Điểm vòng nếu được nhập phải ở 0–100; PENDING không giữ điểm kết luận.
- UT-07 chỉ được gửi offer khi có ít nhất một vòng, không còn vòng PENDING và vòng có `round_no` lớn nhất là PASS. Theo đề bài, vòng trước FAIL không tự chặn nếu vòng cuối PASS. Kiểm tra điều kiện và chuyển OFFER dưới cùng khoá đơn/transaction để không đua với thêm hoặc sửa vòng.
- Email ứng viên cho mỗi lần **chuyển** status được gắn với `application_status_history`, bao gồm WITHDRAWN của UT-01 và các bước của UT-07 khi các luồng dùng cùng cơ chế. Lần tạo đơn `null -> SUBMITTED` không tính là chuyển bước; UT-01 đã gửi email báo HR ở sự kiện đó. Nội dung offer của UT-07 phải dùng chung event hoặc được tính là email trạng thái duy nhất để tránh gửi trùng.

## API và quyền

Đường dẫn dưới gateway `/api/v1/recruitment`:

| API | HR cùng công ty | STAFF | Ghi chú |
| --- | --- | --- | --- |
| `GET /applications/{id}` | Đọc | Đọc | Đơn, ứng viên, CV snapshot, match hiện tại, vòng và lịch sử |
| `GET /hr/applications?jobId=&status=&page=&size=` | Đọc | Không | Danh sách đơn của công ty trong JWT, không nhận `companyId` từ client |
| `POST /applications/{id}/transition` | Ghi | Không | `{toStatus,note}`; 409 `INVALID_TRANSITION` |
| `POST /applications/{id}/rounds` | Ghi | Không | `{name,interviewerEmail,scheduledAt?}` |
| `PUT /applications/{id}/rounds/{roundId}` | Ghi | Không | `{result,score?,note?}`; round phải thuộc đơn |
| `GET /staff/applications?companyId=&jobId=&status=&page=&size=` | Không | Đọc | PageResponse theo quy ước nhóm |

Kiểm tra đơn tồn tại và `companyId` của HR trước khi gọi identity/DN-06. HR sai công ty nhận 404, kể cả khi đoán đúng ID; STAFF không có thao tác ghi. Mở rộng endpoint CV snapshot của UT-01 cho STAFF đọc PDF sau xác thực, và áp dụng cùng cách trả 404 cho HR sai công ty (hiện trả 403). Response PDF vẫn `private, no-store`.

Recruitment gọi DN-06 `GET /jobs/{jobId}/match/{userId}` bằng JWT của HR/STAFF sau khi đã kiểm quyền đơn; DN-06 vẫn kiểm quyền JD. Chốt URL gateway thực và OpenAPI với người làm DN-06. Response chi tiết dùng `match: {score,belowMinimum,met,missing,computedAt} | null`; DN-06 lỗi/tạm chưa có thì vẫn trả chi tiết đơn với `match: null`, FE báo “Chưa có điểm khớp”. Identity cần API nội bộ đọc user theo ID cho tên và email; nếu identity không sẵn sàng, chi tiết đơn trả lỗi phụ thuộc thay vì hiển thị sai danh tính. Không đưa JWT hay service key vào browser ngoài Bearer của người dùng.

## Giao diện

- Trang HR tại `/hr/recruitment` liệt kê đơn của công ty và dẫn đến chi tiết từng đơn; STAFF có bảng tại `/staff/applications` với lọc công ty, JD, trạng thái và phân trang, mở cùng kiểu chi tiết chỉ đọc. Thêm API danh sách HR vì UT-01 chưa cung cấp nguồn cho trang này; đây là quyết định thiết kế của UT-02 để HR có đường vào chi tiết đơn.
- Chi tiết: tên/email ứng viên, JD, CV snapshot, thẻ điểm DN-06, trạng thái và timeline, các vòng theo `roundNo`, lịch sử sửa kết quả. Nút chuyển chỉ có các đích hợp lệ từ map HR; FE vẫn xử lý 409 do trạng thái có thể đổi ở request khác.
- Form thêm vòng có tên, email người phỏng vấn, lịch hẹn tùy chọn. Form kết quả cho sửa trong INTERVIEW; sau đó chỉ đọc.
- Dùng lại thành phần thẻ điểm của DN-06 nếu export phù hợp; nếu không, dùng response chung và giữ cùng cách trình bày, không nhân bản công thức.

## Kiểm thử và bàn giao

- Unit test dạng ma trận toàn bộ cặp trạng thái cho map HR và policy dùng chung; kiểm tra riêng UT-07 không đi qua endpoint transition của UT-02.
- Integration tests: HR công ty B đọc/sửa đơn A hoặc tải CV -> 404; STAFF đọc/lọc/tải CV được và ghi -> 403; SUBMITTED -> INTERVIEW -> 409; tạo/sửa vòng ngoài INTERVIEW bị chặn; cấp roundNo đồng thời; vòng PENDING hoặc vòng cuối FAIL chặn offer; vòng cuối PASS và không còn PENDING cho phép offer; sửa kết quả tạo audit.
- Kiểm tra transaction status/history/outbox và worker gửi đủ mỗi lần đổi; test retry và không tạo email trùng khi request thất bại. Test chi tiết khi DN-06 trả điểm và khi tạm lỗi.
- Cập nhật OpenAPI, hướng dẫn chạy, fixture đơn ở nhiều công ty/trạng thái/vòng, ảnh HR và STAFF; PR `[UT-02] ...` vào nhánh tích hợp hiện hành, một thành viên review trước mentor duyệt.

## Việc còn cần đối chiếu với owner khác

- DN-06: URL và response chính xác, `computedAt`, cách phân biệt “chưa tính” và service lỗi, quyền dùng Bearer HR/STAFF; DN-06 cần giải quyết việc HS-01 hiện chặn người khác đọc kỹ năng khi chưa consent/approve.
- Identity: API nội bộ tra user theo ID và cách lấy địa chỉ email hiện hành cho worker thông báo.
- UT-07: cùng khoá đơn, hàm kiểm điều kiện offer, nội dung email offer và tránh hai email cho một transition.
- Trưởng nhóm: nhánh PR cuối cùng (`test` theo repo hiện tại, `develop` theo mô tả task).
