# Phản hồi offer từ email sau khi đăng nhập

Ngày 25/11/2026. Trạng thái: Đã được người dùng duyệt ngày 25/11/2026. Thay thế phần điều hướng/danh sách offer ứng viên trong đặc tả candidate-application-journey; phần đơn ứng tuyển và phỏng vấn giữ nguyên.

## Quyết định và mục tiêu

Người dùng chọn phản hồi offer bằng link trong email và bắt buộc đăng nhập để xác nhận. Bỏ mục Offer khỏi menu và danh sách offer riêng của ứng viên; vẫn cần trang chi tiết để đọc và gửi quyết định. HR giữ nghiệp vụ tạo/theo dõi offer và xác nhận đã đi làm.

Luồng: HR gửi offer → email chứa link → đăng nhập nếu chưa có phiên → trở lại đúng offer → đọc nội dung → chọn nhận/từ chối → xác nhận → kết quả được ghi nhận trong hệ thống.

## Các hướng đã cân nhắc

- HR ghi nhận phản hồi sau trao đổi: ít thao tác ứng viên nhưng cần HR nhập lại.
- Link email không cần đăng nhập: cần token giao dịch, vòng đời token và xử lý thu hồi.
- Link email cần đăng nhập, được chọn: tái sử dụng API kiểm tra chủ sở hữu và nghiệp vụ hiện tại; không thêm token email hay cơ sở dữ liệu mới.

## Trang phản hồi và khả năng truy cập lại

Giữ URL `/candidate/offers/[id]` để email và thông báo cũ tiếp tục hoạt động. Đây là trang phản hồi riêng, dùng top bar ứng viên; không có menu Offer hoặc đường dẫn tới danh sách offer. Nút quay lại dẫn về hồ sơ ứng tuyển tương ứng khi có applicationId, fallback danh sách đơn.

Giữ bố cục/tokens và các dialog vừa triển khai: vị trí, doanh nghiệp nếu tải được, lương gross/tháng, ngày bắt đầu, địa điểm, nội dung và hạn trả lời giờ Việt Nam. Nhận/từ chối đều có xác nhận cuối. Lý do từ chối tối đa **1.000 ký tự** theo OfferCommandService hiện tại; đồng bộ UI/test với backend (đặc tả trước ghi 2.000).

Trang đơn chỉ có hướng dẫn “Bạn có thư mời nhận việc. Vui lòng kiểm tra email hoặc thông báo để xem và phản hồi.” khi trạng thái OFFER. Không thêm nút phản hồi trong trang đơn hoặc suy đoán offerId từ applicationId. Chuông vẫn mở đúng chi tiết offer; đây là lối truy cập lại nếu người dùng mất email. Điều hướng tới chi tiết vẫn bắt buộc đăng nhập.

URL `/candidate/offers` cũ chuyển sang `/candidate/applications` thay vì 404. Loại bỏ CandidateOfferList nếu không còn caller và cập nhật ROLE_MENU/shell group; không xoá API đọc danh sách vì có thể có consumer khác. Link cũ có returnTo về danh sách offer được bỏ qua trên trang phản hồi mới.

## Đăng nhập và đúng tài khoản

- Giữ guard cấp layout; chưa đăng nhập nhớ pathname rồi chuyển login. Sau đăng nhập thường/SSO và đổi mật khẩu bắt buộc, quay lại chính xác `/candidate/offers/[id]`.
- Tái sử dụng helper next-path nhưng kiểm chứng whitelist nội bộ: chặn URL ngoài origin, protocol-relative, backslash và đường dẫn không thuộc role. Không mở redirect tuỳ ý từ email.
- Phiên hết hạn trong khi tải hoặc gửi phản hồi: nhớ pathname offer trước khi chuyển login; sau đăng nhập đọc lại trạng thái. Không tự phát lại POST, không giả định request trước thất bại.
- Đăng nhập vai trò khác: giữ 403 hiện có, không hiển thị dữ liệu offer. Đăng nhập ứng viên khác: API trả 404, UI ghi “Không tìm thấy offer hoặc tài khoản hiện tại không có quyền xem.” Không tiết lộ tên/email chủ sở hữu.
- Trang sai quyền có nút “Đăng nhập tài khoản khác”: nhớ đúng pathname offer, xoá phiên/cache rồi login. Sau đăng nhập chỉ cho quay lại nếu đường dẫn thuộc vai trò mới. Không thay đổi ý nghĩa nút đăng xuất chung.
- Không tự đăng nhập từ link hoặc xem offer bằng offerId khi chưa xác thực. OfferId chỉ định tài nguyên; backend kiểm tra chủ sở hữu khi đọc lẫn khi ghi.

## Backend và email

Giữ các API authenticated đọc/accept/decline và transaction hiện có: nhận offer chuyển ACCEPTED/HIRED và tạo EmploymentRecord; từ chối chuyển DECLINED/REJECTED. Confirm-onboard vẫn là bước HR riêng. Không dùng endpoint HR để giả danh ứng viên.

Email hiện tại đã gửi `/candidate/offers/{id}`; giữ URL này, bổ sung câu “Vui lòng đăng nhập bằng tài khoản đã ứng tuyển để xem và xác nhận”. Chuẩn hoá hạn trả lời sang giờ Việt Nam thay vì Instant mặc định, giữ thông tin offer. Không đưa thông tin xác thực vào URL. Dùng web-base-url cấu hình của hệ thống. Mở GET hoặc email link không thay đổi quyết định; chỉ POST sau xác nhận mới thay đổi trạng thái.

Không thêm gửi lại email, email xác nhận sau phản hồi hoặc thông báo HR trong đợt này vì đó là khả năng bổ sung cần hợp đồng riêng. HR xem kết quả qua hồ sơ/offer hiện có. UI không khẳng định email đã gửi thành công chỉ dựa vào trạng thái OFFER; lỗi gửi được xử lý bởi cơ chế mail hiện tại.

## Tính đúng đắn và trạng thái

Giữ guard hết hạn tại server và client, chặn gửi lặp, đối soát khi lỗi mạng, huỷ GET cũ trước khi công bố kết quả POST, deadline cập nhật khi GET tải muộn và khi quay lại tab. Đã nhận/từ chối/hết hạn chỉ hiển thị kết quả, không cho quyết định mới. Không tự xoá dữ liệu offer khi bỏ danh sách UI.

Giữ accessibility dialog, focus, draft lý do khi lỗi, responsive 360px và các hành vi từ đặc tả trước. Không mở rộng thay đổi theme, luồng phỏng vấn hoặc quy trình tuyển dụng khác.

## Nghiệm thu

- Email deep link khi chưa login → login/SSO → đúng offer; trường hợp phải đổi mật khẩu vẫn giữ đích.
- Tài khoản sai role hoặc sai ứng viên không thấy dữ liệu; đổi tài khoản quay lại đúng offer, không lộ cache của người trước.
- Phiên hết hạn khi đọc/ghi → đăng nhập → tải lại kết quả, không tự gửi lại mutation.
- Không có menu/danh sách Offer; URL danh sách cũ redirect; email/thông báo cũ mở đúng chi tiết; quay lại đơn đúng applicationId.
- Nhận/từ chối, tối đa 1.000 ký tự, conflict, hết hạn và network reconciliation vẫn chạy qua test cũ cập nhật và regression mới.
- Redirect không nhận đích ngoài website hoặc sai role; email giờ Việt Nam.
- Type-check/lint/build, tests backend liên quan email, E2E auth/notifications/journey và ảnh desktop/tablet/mobile với fixture.

## Trạng thái triển khai

Đã triển khai local theo kế hoạch `docs/superpowers/plans/2026-11-25-email-offer-authenticated.md`. Các thay đổi ứng tuyển/phỏng vấn được giữ; luồng offer trước đó được thay bằng trang phản hồi sau đăng nhập. Chưa commit/push. Kết quả kiểm chứng và các quyết định kỹ thuật được ghi trong kế hoạch.
