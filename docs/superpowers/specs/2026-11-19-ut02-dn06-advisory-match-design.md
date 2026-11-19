# UT-02 — Tích hợp điểm DN-06 chỉ để tham khảo

Trạng thái: người dùng đã duyệt phương án B và thiết kế; điểm chỉ mang tính tham khảo. Đang lập kế hoạch, chưa triển khai mã sản phẩm.

## Mục tiêu và phạm vi

HR/STAFF xem điểm khớp DN-06 ngay trong chi tiết đơn UT-02, dùng component chung, biết khi nào điểm tạm không khả dụng và có thể thử lại. DN-06 lỗi không làm mất phần thông tin đơn, CV, vòng, lịch và offer vốn xem được theo quyền hiện tại.

Điểm0 hoặc belowMinimum=true không chặn ứng tuyển, chuyển bước, tạo/đặt lịch hoặc gửi offer. Điều kiện UT-01, UT-02, UT-06, UT-07 giữ nguyên. Không thêm ngưỡng tuyển dụng, xếp hạng hàng loạt, snapshot điểm trong recruitment DB, không đổi công thức DN-06 hoặc phạm vi quyền của DN-06.

## Hiện trạng đã kiểm tra

- HttpJobMatchClient gọi đúng GET /jobs/{jobId}/match/{userId}, chuyển tiếp bearer; DTO score/belowMinimum/met/missing khớp DN-06.
- HrApplicationReadService đang gộp match vào chi tiết; 404/5xx/network được client chuyển Optional.empty, lỗi hợp đồng hoặc upstream401/403 thành RecruitmentDependencyException502. Trường hợp sau hiện có thể làm mất toàn bộ chi tiết đơn.
- MatchCard riêng của UT-02 chưa dùng MatchScoreCard chung. Component chung đã có nhóm kỹ năng, required/optional, loading/error/retry và JD chưa khai báo kỹ năng.
- DN-06 chỉ cho HR/STAFF xem ứng viên; ADMIN không gọi DN-06. InternalApplicationController đã cung cấp kiểm tra đơn ứng tuyển cho DN-06, không cần viết lại.
- Workspace gốc đang có thay đổi mock/data.json và mock/test/jd-cv.test.js của người dùng; không sửa hoặc mang chúng vào thay đổi này.

## Phương án đã cân nhắc

A. Đổi component và bổ sung kiểm chứng, giữ nguyên match:null không phân biệt nguyên nhân. Ít thay đổi nhưng trạng thái lỗi còn mơ hồ.

B. Làm như A, bổ sung trạng thái lookup và retry độc lập để lỗi điểm không che khuất màn chi tiết. Cần thêm response metadata và kiểm thử tương thích. Người dùng chọn B.

## Các quyết định kỹ thuật đề xuất

Các chi tiết dưới đây là lựa chọn của người thiết kế, chờ duyệt cùng tài liệu.

### Dữ liệu và API

Không migration. Giữ nguyên trường match và cấu trúc MatchResponse; bổ sung matchStatus vào response chi tiết đơn:

| matchStatus | match | Nghĩa |
| --- | --- | --- |
| AVAILABLE | DTO điểm hợp lệ | Điểm lấy được từ DN-06, kể cả score=0 |
| UNAVAILABLE | null | Không lấy được kết quả hợp lệ ở lần gọi hiện tại |
| NOT_REQUESTED | null | ADMIN không gọi DN-06 theo ma trận quyền hiện hành |

JD không có kỹ năng vẫn AVAILABLE; met/missing rỗng và component hiển thị “JD chưa khai báo kỹ năng”. Không suy diễn thiếu dữ liệu xác minh từ score0 vì đó có thể là kết quả tính hợp lệ.

Thêm GET /recruitment/applications/{applicationId}/match (gateway /api/v1/recruitment/applications/{applicationId}/match) trả {matchStatus,match}, chỉ HR/STAFF. Đây là endpoint đọc bổ sung để nút thử lại không gọi lại identity/profile/CV và không thay dữ liệu hoặc form khác trên trang.

Endpoint mới kiểm quyền như chi tiết đơn: HR chỉ công ty mình, STAFF được đọc theo quyền UT-02; tài nguyên ngoài quyền404, thiếu/xấu phiên401, sai vai trò403. ADMIN403 trên endpoint retry nhưng vẫn đọc chi tiết đơn bình thường với NOT_REQUESTED.

Lần mở trang vẫn lấy điểm thông qua GET chi tiết như hiện tại, không phát thêm request match từ FE. Nút thử lại mới gọi endpoint match-only. Hai API dùng chung một lookup service để tránh hai cách xử lý kết quả.

### Lỗi và phân quyền

Ownership/role/account guards chạy trước lookup điểm, không bị catch hoặc hạ thành UNAVAILABLE. Identity, profile-contact, DB và các lỗi nghiệp vụ ngoài lookup điểm giữ hành vi hiện có; không nuốt lỗi bằng catch RuntimeException quanh toàn bộ detail.

Chỉ ở ranh giới gọi DN-06: Optional.empty, timeout/network, upstream404/5xx, lỗi payload/hợp đồng và upstream401/403 đã được adapter biểu diễn thành RecruitmentDependencyException đều cho UNAVAILABLE. Không thử lại bằng tài khoản/service credential khác, không thay đổi quyền DN-06. Upstream401/403/lỗi hợp đồng có log cảnh báo theo mã và ID kỹ thuật để phát hiện cấu hình sai, không log bearer hay body có dữ liệu cá nhân. Không đổi contract HttpJobMatchClient dùng ở nơi khác.

API ứng dụng trả200 với UNAVAILABLE vì người dùng đã có quyền xem đơn; điều này khác với lỗi401/403 của chính API ứng dụng, vẫn trả đúng401/403. Không trả lại điểm cache cũ dưới nhãn điểm hiện tại khi lookup thất bại. Retry thủ công một lượt, không retry loop vô hạn.

Đối chiếu khi lập kế hoạch: RecruitmentHttpSupport đang dùng read timeout30s, lớn hơn timeout Axios15s. Quyết định kỹ thuật: chỉ HttpJobMatchClient dùng connect timeout2000ms/read timeout5000ms có cấu hình; không đổi client chung của các dependency khác. Đánh đổi: lookup chậm hơn5s sẽ hiện UNAVAILABLE và HR có thể thử lại, thay vì đợi đến khi cả request FE hết hạn.

### Giao diện

UT-02 dùng wrapper mỏng giữ tiêu đề “Điểm khớp với JD”, dòng “Điểm phù hợp chỉ mang tính tham khảo” và MatchScoreCard chung cho dữ liệu/nhóm kỹ năng. Không sao chép công thức vào FE và không sửa shared component theo cách phá DN-06 preview.

- AVAILABLE: hiển thị điểm và kỹ năng bằng component chung. belowMinimum chỉ là cảnh báo trong thẻ.
- UNAVAILABLE: “Tạm thời không lấy được điểm phù hợp. Bạn vẫn có thể tiếp tục xử lý đơn.” cùng nút “Thử lại”.
- Đang thử lại: loading chỉ trong khối điểm, chặn gửi lặp; các phần khác trên trang vẫn dùng được.
- Thử lại thành công cập nhật riêng match/matchStatus; thất bại giữ trạng thái không khả dụng, không gán0 thay lỗi và không hiển thị điểm cũ như mới.
- ADMIN: không hiển thị khối điểm hoặc nút retry, không phát request DN-06.

Khi chuyển ứng viên/đơn hoặc đổi phiên trong lúc retry, bỏ response cũ bằng request identity hoặc query key gắn viewerId/applicationId. Không làm mất ghi chú chuyển bước, dữ liệu form vòng hoặc offer chưa lưu. Không vô hiệu hoá action tuyển dụng theo matchStatus, score hoặc belowMinimum.

Frontend mới coi matchStatus là nguồn chính. Để tương thích fixture/response cũ, khi chưa có metadata thì suy ra AVAILABLE nếu match có giá trị, UNAVAILABLE nếu null đối với HR/STAFF; ADMIN vẫn ẩn. Cập nhật fixture/test mới gửi metadata đúng contract.

## Kiểm chứng nghiệm thu

1. HR cùng công ty và STAFF xem đúng điểm từ DN-06; HR khác công ty404 trước mọi lời gọi điểm. ADMIN đọc được đơn nhưng không gọi DN-06 hoặc thấy retry; candidate/mentor không gọi được endpoint nội bộ UT-02 mới.
2. AVAILABLE với score0 vẫn hiện0%, không hiện lỗi; AVAILABLE với hai danh sách rỗng hiện JD chưa có kỹ năng; belowMinimum=true hiện cảnh báo, các action giữ nguyên.
3. DN-06 trả503/timeout/404/payload sai/upstream401 hoặc403: phần chi tiết đơn vẫn trả200, matchStatus UNAVAILABLE, match null. Lỗi quyền của chính API UT-02 vẫn401/403/404; lỗi identity không bị nuốt.
4. Retry lỗi rồi thành công: chỉ khối điểm đổi, không mất input chưa lưu; hai click lúc pending chỉ một request; response đơn A về muộn không ghi đè đơn B hoặc phiên khác.
5. Mock HTTP boundary kiểm đúng URL, bearer và DTO DN-06 có candidateScore thập phân. Khi đủ môi trường, smoke test với các service thật: ứng viên đã nộp JD6, HR xem điểm, đổi đầu vào kỹ năng theo API owner rồi retry nhận điểm hiện hành. Phân biệt bằng chứng HTTP mock với smoke xuyên service thực tế trong báo cáo.
6. Các test UT-01/02/06/07 liên quan vẫn qua, nhất là offer gate theo kết quả vòng, đặt lịch và chuyển bước khi điểm0/belowMinimum/UNAVAILABLE. Không cập nhật các policy này để gọi matching.
7. Cập nhật OpenAPI UT-02, request mẫu, hướng dẫn test UI và ảnh cần thiết. Type-check/lint và backend/browser checks theo phạm vi thay đổi. Nhánh/PR mới có tiêu đề [UT-02], base test; không sửa PR UT-06 cũ.

## Giao cho người triển khai

Chọn tên enum/DTO/lookup service và cách giữ state/retry theo pattern hiện có, đảm bảo một nguồn xử lý availability. Tận dụng test infrastructure và shared MatchScoreCard. Không thêm thư viện hoặc table mới. Khi thực thi tạo workspace cô lập từ test mới nhất và giữ nguyên thay đổi người dùng ở workspace gốc.
