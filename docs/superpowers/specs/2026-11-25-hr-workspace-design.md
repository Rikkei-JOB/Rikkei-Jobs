# Thiết kế không gian tuyển dụng HR dùng top bar

Ngày: 25/11/2026. Trạng thái: người dùng đã duyệt hướng A và bản thiết kế này để triển khai.

## Mục đích và phạm vi

HR cần tìm hồ sơ, đọc CV, xử lý các bước tuyển dụng và theo dõi lịch phỏng vấn nhanh hơn. Dùng top bar để dành chiều ngang cho dữ liệu, đồng bộ phong cách ứng viên theo `DESIGN.md`. Người dùng đã chọn phương án A; các kích thước và cách tổ chức màn dưới đây là đề xuất của Codex.

Phạm vi gồm khung HR, danh sách và chi tiết hồ sơ, tin tuyển dụng, lịch phỏng vấn, thông tin công ty. Thông báo HR là hạng mục nghiệp vụ riêng trong lộ trình này. Mục Thực tập sinh giữ đường dẫn hiện tại; cần xác minh màn thực tế trước khi chỉnh nội dung.

Không thay quy tắc chuyển trạng thái, quyền truy cập hay hợp đồng offer/phỏng vấn để phục vụ bố cục. Không thêm dashboard hoặc thống kê chưa có nguồn dữ liệu. Không thay khung Staff/Admin/Mentor. Tránh tăng số lần bấm cho tác vụ xử lý hồ sơ và tránh mất bộ lọc khi quay lại danh sách.

## Các phương án

| Phương án | Lợi ích | Đánh đổi | Quyết định |
| --- | --- | --- | --- |
| A: Top bar toàn bộ | Rộng chỗ cho bảng, đồng bộ với ứng viên, phù hợp năm mục HR | Cần menu thu gọn ở tablet | Người dùng đã chọn |
| B: Header và sidebar thu gọn | Thuận tiện khi số phân hệ tăng | Tốn chiều ngang và khác bố cục ứng viên | Không chọn |

## Hiện trạng và nền tảng

`AppShell` hiện dành riêng `CandidateShell` cho ứng viên; HR và các vai trò khác dùng header 48px, sidebar 224px. Thêm `HrShell` theo cùng cách phân nhánh, giữ nguyên các role khác. Tái sử dụng Button, menu/dialog primitive Base UI và mapping trạng thái; chỉ trích xuất phần chung của shell khi có hành vi thực sự giống nhau.

Tokens, Inter, Lucide, control 44px, focus, spacing và màu ngữ nghĩa theo `DESIGN.md`. Đỏ thương hiệu #B4232D dùng cho CTA và active state; nền #F6F7FA, bề mặt trắng. Không sao chép theme riêng cho HR. Nếu sửa token toàn cục, kiểm chứng các màn đang dùng token đó. Cập nhật mục layout HR trong `DESIGN.md` khi triển khai thiết kế đã duyệt.

## Điều hướng

Desktop từ 1200px: thanh trắng sticky cao 64px, logo trái; menu lần lượt Tin tuyển dụng, Hồ sơ ứng tuyển, Lịch phỏng vấn, Thực tập sinh; bên phải chuông và avatar tài khoản. Nội dung tối đa 1200px căn giữa. Không thêm banner lớn vào trang thao tác.

| Nhãn | Đích |
| --- | --- |
| Logo / Tin tuyển dụng | `/hr/jobs` |
| Hồ sơ ứng tuyển | `/hr/recruitment` |
| Lịch phỏng vấn | `/hr/interviews` |
| Thực tập sinh | `/hr/interns` hiện tại; không tạo mới tính năng trong đợt UI |
| Thông tin công ty trong menu tài khoản | `/hr/company` |
| Đổi mật khẩu trong menu tài khoản | `/change-password` |
| Đăng xuất | Dùng logout hiện có, xoá phiên và query cache |

Menu tài khoản hiển thị tên và vai trò HR. Trang công ty vẫn truy cập trực tiếp được; mục Thông tin công ty đánh dấu đang chọn khi ở trang con preview. Active state của menu chính khớp cả trang chi tiết. Logo dùng đích HR hiện có, không đổi chính sách redirect đăng nhập.

Dưới 1200px: logo, chuông, avatar và nút menu; điều hướng thu gọn đóng khi chọn trang hoặc Escape. Hỗ trợ focus, bàn phím và tên truy cập; không che nội dung vĩnh viễn. Popover tài khoản/thông báo không mở chồng nhau. Điều chỉnh sticky offset chi tiết HR theo header 64px; không áp dụng offset này cho vai trò khác.

## Danh sách hồ sơ ứng tuyển

Thứ tự: tiêu đề và mô tả ngắn → bộ lọc → số kết quả và bộ lọc đang áp dụng → bảng → phân trang.

- Thay ô UUID JD bằng chọn vị trí có nhãn tên tin; giá trị gửi vẫn là jobId. Dùng nguồn JD thuộc công ty, tải đủ các trang hoặc tìm kiếm phía server nếu API hỗ trợ; không giả định trang JD đầu là toàn bộ lựa chọn. Khi URL có jobId ngoài tập đang tải, vẫn giữ bộ lọc và tra tên theo API được cấp quyền; không âm thầm bỏ giá trị.
- Giữ lọc trạng thái, không thêm tìm tên/email hoặc khoảng ngày trước khi API hỗ trợ. Bộ lọc và page trên URL; đổi bộ lọc về trang 1, giữ tham số còn hợp lệ. Có Xoá bộ lọc.
- Bảng có header: Ứng viên, Vị trí, Ngày ứng tuyển, Trạng thái, Xem hồ sơ. Chỉ hiển thị trường có trong response; không thêm điểm khớp vào list khi chỉ detail có dữ liệu đó.
- Link chi tiết giữ đường dẫn quay lại chứa bộ lọc/page nội bộ hợp lệ. F5/Back/Forward khôi phục đúng trạng thái. Kiểm tra return URL thuộc `/hr/recruitment`, không nhận URL ngoài.
- Mobile dùng card, tên/vị trí được wrap. Skeleton khớp bố cục; lỗi có Thử lại; rỗng có Xoá bộ lọc khi phù hợp. Không cho dữ liệu cũ bị hiểu thành kết quả mới trong lúc tải/lỗi.

## Chi tiết hồ sơ

Breadcrumb quay lại danh sách đã lọc. Tóm tắt đầu trang gồm tên ứng viên là H1, vị trí, email/điện thoại nếu có, ngày nộp, trạng thái và tải CV snapshot đã nộp.

Desktop: nội dung chính và aside 300px khi đủ chỗ; dưới 1200px một cột. Aside hiển thị điểm phù hợp hiện tại, thời điểm tính, kỹ năng đạt/thiếu và hành động chuyển bước. Aside chỉ sticky khi chiều cao khả dụng đủ; nội dung dài vẫn tiếp cận được.

Nội dung chia tab Hồ sơ, Phỏng vấn, Offer, Lịch sử. Hồ sơ chứa thư giới thiệu và nút tải CV; không hứa preview PDF trong đợt này. Phỏng vấn dùng lại vòng và lịch sử sửa kết quả. Offer dùng nghiệp vụ riêng hiện có. Lịch sử giữ timeline đầy đủ. Có chỉ báo trong tab khi tồn tại dữ liệu liên quan; tab theo URL để reload vẫn giữ ngữ cảnh.

Hành động lấy từ allowedTransitions, nhãn cụ thể, busy chặn lặp, 409 tải lại dữ liệu và giải thích trạng thái đã đổi. Giữ ghi chú chuyển bước và quyền hiện có. Không dựng tiến trình tuyến tính khiến Không đạt/Đã rút trông như bước bắt buộc. Không trộn ghi chú nội bộ vào thông báo ứng viên. Tabs không làm mất form đang nhập khi chuyển tab; khi chuyển hồ sơ phải reset theo đúng application ID.

## Các màn liên quan

| Màn | Đề xuất |
| --- | --- |
| Tin tuyển dụng | H1 rõ ràng, CTA Tạo tin tuyển dụng, tab trạng thái, bảng tên tin/trạng thái/hạn nộp theo dữ liệu thật; giữ điều kiện công ty khoá và thao tác theo quyền |
| Form tin tuyển dụng | Chia nhóm nội dung, yêu cầu, quyền lợi và thời hạn; lỗi cạnh field; giữ dữ liệu khi lỗi và hợp đồng gửi duyệt |
| Lịch phỏng vấn | Ưu tiên lịch hôm nay, điều hướng ngày/tuần và danh sách phù hợp mobile; chỉ thêm chế độ khi dữ liệu hiện có hỗ trợ đủ phạm vi thời gian |
| Công ty | Nhóm thông tin cơ bản, logo, liên hệ; nút Xem trang công ty; giữ quyền chỉ xem/chỉnh sửa hiện có |
| Thực tập sinh | Giữ menu/route hiện tại, chưa thiết kế lại nghiệp vụ trong phạm vi này |

## Thông báo HR

API `/me/notifications` hiện có chỉ dành cho ứng viên; không dùng nó cho HR. Chuông chỉ xuất hiện trong bản bàn giao hoạt động khi backend HR hoàn thành. Nếu triển khai UI trước, tạm chưa hiện chuông; không hiển thị số 0 hoặc danh sách rỗng giả.

Đề xuất sự kiện HR: đơn mới, ứng viên rút đơn, ứng viên phản hồi lịch/phỏng vấn, phản hồi offer và kết quả duyệt tin. Trước triển khai phải chốt owner sự kiện, người nhận là HR nào của công ty và quy tắc khi công ty/tài khoản bị khoá. Đây là những quyết định chưa được người dùng duyệt; không tự gửi thông báo cho toàn bộ HR.

Chức năng đích: số chưa đọc (99+), danh sách phân trang, thời gian, mở đối tượng liên quan, đánh dấu từng mục/tất cả đã đọc, loading/rỗng/lỗi và thử lại. Server sở hữu trạng thái đọc, lấy người nhận từ phiên, kiểm quyền đối tượng đích, deduplicate sự kiện. Tái sử dụng phần trình bày ứng viên nếu tách được an toàn, không gắn hook HR vào API ứng viên. Không thêm browser push trong thiết kế này. API/sự kiện HR cần đặc tả và kiểm thử riêng trước khi triển khai.

## Responsive và nghiệm thu

- 1440×1000: menu desktop, bảng đầy đủ, detail hai cột; 834×1112: menu thu gọn, detail một cột; 390×844 và 360px: card, control dễ bấm, không tràn ngang trang.
- Tuân thủ z-index trong DESIGN.md: header 30, popover 40, dialog 50. Popup có title, focus trap, Escape khi không busy và trả focus khi đóng; nội dung vừa màn hình thấp.
- Kiểm tra tiếng Việt, tên dài, thiếu logo/điện thoại/điểm phù hợp, dữ liệu rỗng, lỗi mạng, busy, 409 và công ty khoá. Chỉ dùng số đếm từ dữ liệu server đúng phạm vi.
- Chụp trước/sau bằng Playwright ở ba viewport, xem ảnh thực tế và thử bàn phím. Kiểm thử điều hướng HR, lọc/Back/return, chuyển bước, vòng phỏng vấn, offer, logout và quyền; hồi quy các role dùng component chung. Chạy type-check, lint, build và các E2E liên quan.

## Thứ tự triển khai đề xuất

1. HrShell, top bar và tài khoản; cập nhật DESIGN.md và kiểm tra role khác.
2. Danh sách hồ sơ, chọn vị trí, bộ lọc và đường quay lại.
3. Chi tiết hồ sơ, tabs và vùng xử lý; giữ nghiệp vụ đang chạy.
4. Tin tuyển dụng, công ty và lịch phỏng vấn theo cùng nền tảng.
5. Đặc tả riêng, backend và UI thông báo HR sau khi chốt người nhận/sự kiện.

Mỗi đợt bàn giao có ảnh và hành vi kiểm chứng được; không để đợt thông báo chặn cải thiện các màn HR hiện có. Tài liệu này chưa thay đổi runtime của dự án.
