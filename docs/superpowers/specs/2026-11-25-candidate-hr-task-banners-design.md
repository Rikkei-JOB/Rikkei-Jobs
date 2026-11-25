# Banner ứng tuyển và phỏng vấn — ứng viên và HR

Ngày: 25/11/2026. Trạng thái: Người dùng duyệt đặc tả hướng A ngày 25/11/2026; đã triển khai và kiểm chứng local. Ảnh trước/sau: `docs/design/task-banners/README.md`.

## Mục tiêu và phạm vi

Cải tiến phần đầu bốn trang danh sách/lịch: `/candidate/applications`, `/candidate/interviews`, `/hr/recruitment`, `/hr/interviews`. Dùng phong cách minh hoạ công nghệ của banner tìm việc đã duyệt, giúp nhận diện nhiệm vụ và tạo điểm nhấn thương hiệu. Đây là thay đổi trình bày nhỏ, dùng chung một component banner với nội dung và minh hoạ theo trang.

Không mở rộng sang trang chi tiết, banner tìm việc, offer hoặc vai trò khác. Giữ nguyên nghiệp vụ, API, phân quyền, bộ lọc và điều hướng hiện có. Không thêm thống kê hoặc CTA không có chức năng thực tế.

## Các hướng cân nhắc

- **A — Được người dùng chọn:** gradient đỏ trầm, chữ trắng, SVG công nghệ màu kem/hồng nhạt. Đồng nhất với banner tìm việc; bản HR gọn hơn để dành chỗ cho dữ liệu.
- **B:** nền trắng pha hồng, chữ tối, minh hoạ nhỏ. Ít chiếm sự chú ý nhưng thiếu điểm nhấn thương hiệu so với A.

## Nội dung bốn banner

Mỗi trang giữ đúng một H1 bằng tên trang hiện có, dễ định hướng và hỗ trợ công nghệ trợ giúp. Thông điệp cảm hứng là dòng mô tả, không tạo thêm H1.

| Trang / H1 | Mô tả | Minh hoạ | Hành động / thông tin |
| --- | --- | --- | --- |
| Ứng tuyển của tôi | Mỗi hồ sơ, một cơ hội mới. Theo dõi phản hồi và bước tiếp theo trong hành trình ứng tuyển. | CV và các mốc tiến trình | Giữ link Tìm việc làm mới tới trang tìm việc hiện tại |
| Phỏng vấn của tôi | Sẵn sàng cho cuộc hẹn tiếp theo. Chọn khung giờ, theo dõi lịch hẹn và chuẩn bị phỏng vấn. | Lịch hẹn và màn hình cuộc gọi | Giờ Việt Nam · UTC+7 |
| Hồ sơ ứng tuyển | Kết nối đúng người, đúng cơ hội. Sàng lọc hồ sơ và theo dõi tiến trình tuyển dụng của công ty. | Hồ sơ ứng viên, kính lúp, dấu kiểm | Không thêm CTA |
| Lịch phỏng vấn | Chủ động lịch hẹn, kết nối nhân tài. Theo dõi lịch phỏng vấn và phối hợp cùng đội ngũ tuyển dụng. | Lịch tuần và các thành viên kết nối | Giờ Việt Nam · UTC+7; chuyển Tuần/Danh sách xuống thanh công cụ ngay dưới banner |

## Hình thức và responsive

- Tuân thủ `DESIGN.md`: gradient `#8F1D2B → #B4232D`, Inter, bo góc 16px, padding 24–32px; chữ trắng và mô tả trắng pha hồng đủ tương phản. Minh hoạ nằm bên phải trên desktop, chiếm khoảng 28–32% chiều rộng; không chứa dữ liệu thật hoặc văn bản nghiệp vụ cần đọc.
- Đề xuất của người triển khai: min-height 200px cho ứng viên, 160px cho HR ở desktop; không cố định chiều cao khi nội dung dài. Tablet giảm kích thước minh hoạ, cho chữ wrap; mobile padding 20px, min-height khoảng 160px và tự tăng theo nội dung, ẩn minh hoạ nếu thiếu chỗ. Không để minh hoạ đè lên chữ.
- H1 30/36px desktop, 24/30px mobile; mô tả 14/22px. CTA có vùng bấm tối thiểu 44px và focus trắng rõ trên nền đỏ.
- Dùng SVG viết trong code, `aria-hidden`, không lấy ảnh ngoài và không thêm dependency. Không animation liên tục. Giữ H1 phỏng vấn ứng viên có thể nhận focus khi cần khôi phục focus từ danh sách.
- Bộ lọc, tabs, thông báo kết quả thao tác và điều khiển lịch dùng surface sáng dưới banner. Không nhét control lịch/bộ lọc vào vùng trang trí.

## Triển khai và nghiệm thu

Tái sử dụng một component tại lớp UI chung với hai chủ đề minh hoạ (hồ sơ/phỏng vấn), có biến thể ứng viên/HR gọn; không tạo framework banner hay theme riêng theo vai trò. Chi tiết nét vẽ và spacing nhỏ do người triển khai quyết định trong các quy tắc trên. Cập nhật pattern banner trong `DESIGN.md` cùng thay đổi.

Chụp trước/sau bằng Playwright cho cả bốn trang ở 1440×1000, 834×1112, 390×844; kiểm tra không tràn ngang ở 360px, đọc chữ, bàn phím/focus, loading/rỗng/lỗi và tên trang duy nhất. Xem ảnh thực tế; kiểm tra CTA tìm việc, tabs ứng viên và chuyển Tuần/Danh sách HR vẫn hoạt động. Chạy type-check, lint, build và các kiểm thử hành vi liên quan; không thêm test chỉ kiểm tra class CSS hay hằng số màu.

Giữ nguyên các thay đổi local trước đó; không commit/push trong bước chuẩn bị thiết kế này.
