# Mock JD/CV dùng chung

## Mục tiêu và phạm vi

Theo cấu trúc đã trao đổi, mở rộng Express trong `mock/` để frontend và recruitment-service gọi thử hợp đồng DN-04/HS-04 qua HTTP. Giữ nguyên 11 endpoint hệ thống ngoài và các công cụ `/mock/*`. Mock dùng dữ liệu thử, không phải implementation production hoặc bằng chứng owner đã duyệt hợp đồng.

## Phương án

- Mở rộng Express hiện có: giữ một lệnh chạy, hỗ trợ route nghiệp vụ, xác thực và PDF. Đây là phương án được triển khai theo yêu cầu tiếp tục của người dùng.
- JSON Server: phù hợp CRUD JSON nhưng cần thêm xử lý cho các route nội bộ, JWT và PDF; không thêm một server khác trong thay đổi này.
- Mock chỉ trong trình duyệt: bộ test hiện có vẫn giữ, nhưng không đáp ứng request từ recruitment-service.

## Hợp đồng

- `GET /internal/jobs/{jobId}/application-context`: yêu cầu `X-Internal-Key`, trả `{jobId, companyId, jobTitle, companyName, status, deadline, companyLocked, hrEmails}`. Các fixture gồm APPROVED, chưa duyệt, hết hạn, công ty khoá; quyết định có được ứng tuyển vẫn thuộc recruitment-service.
- `GET /profile/cvs`: JWT CANDIDATE hợp lệ, chỉ trả mảng `{id,name,targetPosition}` của `sub` trong token.
- `GET /profile/cvs/{cvId}/pdf`: JWT CANDIDATE và ownership, trả file PDF thật với `application/pdf`; sai chủ sở hữu 403, không có CV 404.
- Alias `/api/v1/profile/cvs` và `/api/v1/profile/cvs/{cvId}/pdf` phục vụ môi trường dev gọi trực tiếp mock. Internal endpoint không có alias gateway.
- JWT xác minh chữ ký HMAC HS256/384/512 theo `JWT_SECRET`, kiểm tra `exp`, `nbf` nếu có, UUID subject, role và `mustChangePassword`. Thiếu cấu hình JWT trả 503; không chấp nhận token tuỳ ý.
- `RECRUITMENT_COMPANY_SERVICE_KEY` ưu tiên `INTERNAL_SERVICE_KEY`, mặc định dev là `dev-internal-key`. Khoá này độc lập `X-Service-Key` của hệ thống ngoài.
- CORS chỉ trên route CV cho origin `MOCK_WEB_ORIGIN` (mặc định `http://localhost:3000`). Không đưa internal key vào frontend.
- Lỗi giữ `{code,message}`. Fixtures CV có owner UUID cố định; khi dùng JWT từ identity thật, cập nhật `ownerId` rồi reload vì UUID seed ứng viên không cố định.

## Cấu trúc và xác minh

Route và xác thực nằm trong `mock/jd-cv.js`; `mock/server.js` mount trước guard legacy. `mock/data.json` thêm `jobs`/`cvs`, PDF riêng trong `mock/fixtures/cvs/sample.pdf`, script `mock/candidate-token.js` tạo token thử local. Tài liệu hợp đồng ở `docs/api/UT-01-applications.md`; request mẫu, README và CI kiểm tra mock đi cùng thay đổi.

Node test runner gọi HTTP thật để kiểm tra các nhánh auth, ownership, lỗi, PDF, CORS và regression đủ 11 endpoint cũ. Render và đọc lại PDF fixture. Không thay đổi code ứng dụng backend/frontend, cấu hình production, hoặc tự push thay đổi.
