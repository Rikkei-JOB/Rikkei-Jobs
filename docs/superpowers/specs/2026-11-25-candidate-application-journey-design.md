# Giao diện ứng tuyển phỏng vấn và offer của ứng viên

Ngày: 25/11/2026. Trạng thái: Đã được người dùng duyệt ngày 25/11/2026.

## Mục tiêu và phạm vi

Ứng viên đã chọn hướng A: giữ ba trang riêng cho đơn ứng tuyển, phỏng vấn và offer, thống nhất phong cách và làm rõ hành động tiếp theo. Mục tiêu là nhận biết tình trạng hồ sơ, chọn giờ phỏng vấn và phản hồi offer dễ dàng trên desktop và điện thoại.

Áp dụng DESIGN.md, top bar ứng viên và các API hiện có. Giữ nguyên quyền truy cập, trạng thái nghiệp vụ, điều kiện rút hồ sơ, chọn giờ, nhận/từ chối offer. Không bổ sung dashboard, API, tính năng đặt lại lịch hoặc thông báo mới. Dialog nộp ứng tuyển đã có được giữ; chỉ kiểm tra tương thích khi thay đổi theme chung.

## Các hướng đã cân nhắc

| Hướng | Lợi ích | Đánh đổi |
| --- | --- | --- |
| A, được chọn | Ba trang đồng nhất, tận dụng routes và chức năng hiện có | Vẫn chuyển trang giữa các nhiệm vụ |
| B | Một trung tâm gom thông tin theo hồ sơ | Cần dữ liệu liên kết phỏng vấn với hồ sơ mà hợp đồng hiện tại chưa trả |

Các quyết định chi tiết bên dưới là đề xuất của Codex để người dùng duyệt.

## Nền tảng giao diện

- Giữ menu top bar hiện tại: nhóm Ứng tuyển chứa Đơn ứng tuyển, Phỏng vấn và Offer. Active state đúng trang con, chuông giữ nguyên.
- Nội dung tối đa 1200px, padding 24px trên tablet/desktop và 16px mobile. Header trang gồm một H1, mô tả ngắn và thao tác nếu có. Không dùng banner minh hoạ lớn cho các trang tác vụ.
- Dùng tokens chung trong globals.css theo DESIGN.md; ưu tiên ánh xạ trong vùng tính năng, tránh đổi theme các vai trò khác. Card trắng, radius 12px, padding 24/16px, gap 16–24px. Control tối thiểu 44px; input mobile 16px.
- Desktop từ 1200px: trang chi tiết có nội dung chính và aside 300px; tablet/mobile xếp một cột. Aside sticky dưới header; mobile dùng hành động trong luồng nội dung, không thêm thanh cố định.
- Trạng thái có chữ và màu; dùng application-status.ts cho trạng thái hồ sơ. Offer và lịch có nhãn riêng theo ý nghĩa thực tế.
- Ngày giờ theo Asia/Ho_Chi_Minh. Tên dài, email, nội dung dài wrap; không tràn ngang ở 360px. Một main landmark cho mỗi trang, tận dụng main của shell.

## Đơn ứng tuyển

### Danh sách

Thứ tự: tiêu đề “Ứng tuyển của tôi” → bộ lọc có label → số kết quả → card → phân trang. Giữ API lọc status và page, size 20; không thêm tìm kiếm hoặc các tổng thống kê chưa có API.

Card có tên vị trí, tên doanh nghiệp, badge trạng thái, ngày nộp và link “Xem tiến trình”. Tên vị trí cũng mở chi tiết. Chỉ hiện hướng dẫn bước tiếp theo theo trạng thái, chẳng hạn “Đang chờ nhà tuyển dụng xem xét”; không khẳng định có lịch hoặc offer dựa vào trạng thái hồ sơ.

URL giữ status/page. Link chi tiết mang returnTo là đường dẫn danh sách nội bộ đã kiểm tra; Back khôi phục bộ lọc. Phân trang chỉ có khi cần. Lỗi lọc hoặc đổi trang không hiển thị dữ liệu cũ như kết quả mới.

### Chi tiết

Breadcrumb → thẻ vị trí/doanh nghiệp/trạng thái/ngày nộp → tiến trình → hai cột nội dung và tài liệu.

Nội dung chính: tiến trình và giải thích trạng thái, thư giới thiệu nếu có, lịch sử trạng thái. Aside: tải CV đã nộp, lời giải thích bản snapshot, xem tin tuyển dụng và thao tác rút hồ sơ. Không lặp tiến trình ở hai nơi.

Tiến trình desktop nằm ngang; mobile chuyển dọc để nhãn dễ đọc. REJECTED/WITHDRAWN đánh dấu đã kết thúc tại bước từ lịch sử, không đánh dấu một bước đang chạy. HIRED hiển thị đã hoàn tất tiến trình hồ sơ; không diễn giải thành xác nhận ứng viên đã bắt đầu làm việc.

Giữ WithdrawDialog và điều kiện canWithdraw từ server. Link ứng tuyển lại vẫn mở tin tuyển dụng để kiểm tra điều kiện hiện hành. “Xem lịch phỏng vấn” hoặc “Xem offer” chỉ là lối tắt sang danh sách tương ứng khi trạng thái phù hợp, không mở một đối tượng suy đoán và không báo rằng chắc chắn có đối tượng.

## Phỏng vấn

Header “Phỏng vấn của tôi”, mô tả nhiệm vụ và nhãn “Giờ Việt Nam · UTC+7”. Ba bộ lọc dạng tab: Cần xử lý, Sắp diễn ra, Lịch sử; mặc định Cần xử lý, URL view giữ lựa chọn. Hiển thị số mục vì mineAll đã tải toàn bộ các trang; lỗi một trang là lỗi tải toàn bộ, không dùng tổng chưa đầy đủ.

Phân nhóm theo thứ tự ưu tiên, mỗi lời mời chỉ xuất hiện một nhóm:

1. Có schedule SCHEDULED: giờ kết thúc còn ở tương lai thuộc Sắp diễn ra; giờ kết thúc đã qua thuộc Lịch sử với nhãn “Đã qua giờ hẹn”, không tự chuyển thành DONE.
2. Có schedule DONE: Lịch sử, nhãn “Đã hoàn thành”.
3. Không có schedule hoặc CANCELLED: Cần xử lý; có slot OPEN còn ở tương lai thì chọn giờ, không có slot thì hướng dẫn chờ HR. CANCELLED giữ thông tin lịch cũ và nhãn đã huỷ, không cho vào phòng cũ.

Sắp diễn ra xếp theo giờ bắt đầu tăng dần; Lịch sử theo thời gian giảm dần; Cần xử lý giữ thứ tự server. Mục hết giờ chuyển nhóm khi đồng hồ được cập nhật; giữ focus và không tự đổi tab đang xem.

Card hiển thị doanh nghiệp, vị trí, vòng, email người phỏng vấn và trạng thái. Lịch xác nhận có khối ngày, giờ bắt đầu/kết thúc và thời lượng. Nút “Vào phòng phỏng vấn” chỉ xuất hiện với SCHEDULED còn trong Sắp diễn ra và có meetUrl hợp lệ http/https; giữ mở tab mới an toàn. Nếu chưa có link, thông báo “Đường dẫn phòng phỏng vấn chưa được cập nhật”. Không tự thêm điều kiện chỉ được tham gia sát giờ.

Khung giờ đề xuất dùng radio card, nhóm theo ngày, có giờ và thời lượng. Chỉ đưa ra slot OPEN chưa bắt đầu. Chọn một khung giờ rồi “Xác nhận khung giờ”; busy khoá lựa chọn/gửi lặp. Thành công tải lại toàn bộ dữ liệu và hiển thị xác nhận cùng liên kết chuyển sang Sắp diễn ra, không tự đổi tab. Nếu 409, tải lại và xoá lựa chọn hết hiệu lực; giữ lỗi rõ ràng. Không thêm huỷ hoặc đề nghị đổi lịch vì chưa có API ứng viên.

InterviewInvitation chưa có applicationId; không tạo link chi tiết hồ sơ bằng cách đoán vị trí hay doanh nghiệp. Không thêm lịch tháng hoặc xuất lịch ở đợt này.

## Offer

### Danh sách

Header “Offer của tôi”, mô tả “Xem thư mời nhận việc và phản hồi trước hạn”. Mỗi card: vị trí, badge, lương gross/tháng, ngày bắt đầu, hạn trả lời và “Xem offer”. Dùng phân trang server hiện có, size 10; page ở URL và giữ khi quay lại từ chi tiết.

Giữ thứ tự server, không thêm tab lọc hoặc sắp xếp chỉ trên một trang nhưng diễn đạt như toàn bộ dữ liệu. SENT có nhãn “Chờ phản hồi”; nếu deadline đã qua, nhãn “Đã quá hạn phản hồi” và không cho thao tác, trong khi trạng thái chuẩn vẫn chờ server xác nhận. Hạn trong vòng 48 giờ nhấn warning và ghi rõ thời gian; hết hạn không dùng warning như còn có thể phản hồi.

Offer list không tải application detail cho từng hàng để bổ sung tên công ty. Không hiển thị doanh nghiệp giả hoặc thêm nhiều request cho từng card.

### Chi tiết

Breadcrumb về danh sách giữ page → vị trí/trạng thái → phần tóm tắt lương gross/tháng, ngày bắt đầu, địa điểm, hạn phản hồi → ghi chú giữ xuống dòng → khối phản hồi. Desktop khối phản hồi ở aside; mobile ngay sau tóm tắt và ghi chú để ứng viên đọc trước khi quyết định.

Offer có applicationId: tải thêm application detail để hiển thị doanh nghiệp và link “Xem hồ sơ ứng tuyển”. Đây là phần bổ sung độc lập: tải/lỗi không chặn nội dung offer hoặc phản hồi; nếu lỗi thì bỏ doanh nghiệp và có link hồ sơ từ applicationId, không suy đoán dữ liệu.

Hạn hiển thị ngày giờ tuyệt đối và thời gian còn lại theo ngày/giờ/phút, không đếm từng giây. Cập nhật mỗi phút và hẹn cập nhật chính xác tại deadline. Đến hạn khoá thao tác ngay, kể cả dialog đang mở, tải lại trạng thái server một lần; server quyết định kết quả cuối cùng.

- “Nhận offer”: nút primary mở dialog xác nhận, nhắc vị trí, lương, ngày bắt đầu. CTA cuối “Xác nhận nhận offer”. Chỉ gửi request sau xác nhận.
- “Từ chối offer”: nút secondary mở dialog riêng, textarea lý do bắt buộc, tối đa 2000 ký tự, đếm ký tự; nút cuối “Xác nhận từ chối”. Giữ lý do khi lỗi và khi đóng/mở lại trên cùng offer; reset khi đổi offer.
- Khi busy: khoá gửi lặp và đóng dialog. 409 tải lại trạng thái; nếu không còn phản hồi được, đóng dialog và hiển thị trạng thái mới. Lỗi khác giữ nội dung, hiển thị lỗi tại dialog.
- Khi lỗi mạng chưa biết kết quả: giữ dữ liệu, khoá gửi lại và cho “Kiểm tra kết quả” qua detail; chỉ mở lại thao tác sau khi đối soát thành công và offer vẫn còn SENT/chưa hết hạn. Không tự gửi lại mutation.
- ACCEPTED/DECLINED/EXPIRED thay thao tác bằng thông báo kết quả; dùng respondedAt nếu server có, không tự điền thời điểm. DECLINED hiện lý do. ACCEPTED giữ link xem kinh nghiệm trên hồ sơ.

Dialog dùng Base UI, title/description, focus trap, Esc khi không busy, trả focus về trigger nếu còn tồn tại hoặc về tiêu đề khối kết quả khi trigger đã bị loại bỏ. Body cuộn trên màn thấp; footer tiếp cận được. Kiểm tra deadline lại ngay trước khi gửi.

## Trạng thái và dữ liệu

Mỗi màn có skeleton/role status, rỗng có hành động phù hợp, lỗi có thử lại, busy và thành công. Rỗng do bộ lọc có xoá bộ lọc; chưa có đơn có khám phá việc làm; chưa có lịch giải thích chờ lời mời; chưa có offer giải thích offer xuất hiện khi doanh nghiệp gửi.

Không để dữ liệu người dùng cũ hoặc đối tượng cũ xuất hiện khi đổi phiên/id; query key hoặc vòng đời dữ liệu theo user/token/id/params. Tải lỗi không cho thực hiện hành động trên bản dữ liệu đã mất hiệu lực. Thao tác tải CV lỗi hiển thị riêng, không xoá nội dung hồ sơ.

Giữ cơ chế chống gửi lặp, xử lý conflict và đối soát nộp ứng tuyển hiện có. Dữ liệu thật thiếu thì bỏ trường hoặc giải thích. Giữ thông báo và các đường dẫn từ thông báo hiện tại.

## Triển khai và nghiệm thu

Tái sử dụng Button/Card/Tabs, badge, WithdrawDialog, formatter và Base UI. Chỉ trích xuất component có phần dùng chung thực tế: header trang, vùng trạng thái tải/lỗi và khối metadata; builder quyết định cách chia file/CSS và thư viện dữ liệu theo pattern repo, không thêm dependency nếu chưa cần.

Nghiệm thu hành vi: lọc/phân trang và Back; hồ sơ kết thúc; tải CV/rút hồ sơ; chọn giờ thành công và 409; lịch huỷ/chưa link/đã qua giờ; nhận/từ chối offer, giữ lý do khi lỗi, deadline trong dialog, 409 và đối soát lỗi mạng; không gửi mutation lặp; links thông báo vẫn mở đúng trang.

Chụp bằng Playwright 1440×1000, 834×1112 và 390×844; kiểm tra thêm 360px. Bộ ảnh gồm danh sách/chi tiết đơn, phỏng vấn cần chọn giờ/đã xác nhận, danh sách/chi tiết offer và dialog phản hồi. Gắn nhãn fixture trong tài liệu ảnh. Kiểm tra bàn phím, focus, nội dung dài và reduced motion. Chạy type-check/lint/build và E2E liên quan; xác minh cả màn nộp ứng tuyển và thông báo nếu dùng chung theme/component.

Không viết product code hoặc thay đổi API trong bước đặc tả này. Sau khi thiết kế được duyệt sẽ lập kế hoạch triển khai và xác minh.
