# UT-03 — Kết quả kiểm chứng ngày 2026-11-26

Phạm vi: DB, BE, FE của UT-03; không triển khai DN-03. Nhánh đích PR: test. Tự triển khai và tự review, không dùng subagent theo yêu cầu người dùng.

## Kết quả

| Kiểm tra | Kết quả |
| --- | --- |
| Maven clean test: common-core, common-observability, gateway, profile, company, recruitment | 600 test, 599 pass, 1 skip; 0 failure/error; BUILD SUCCESS |
| Playwright toàn bộ trên nền origin/test đã cập nhật | 134 pass, 18 skip theo điều kiện có sẵn; 0 failure |
| Các test UT-03 trong Playwright | 19 pass, gồm HR 4, STAFF/ứng viên 3, visual 12 |
| TypeScript type-check | Pass |
| ESLint toàn bộ | 0 error; 1 warning có sẵn tại web/commitlint.config.mjs |
| Next.js production build | Pass |
| OpenAPI | Parse YAML thành công, 8 paths |
| PowerShell demo | Parse syntax thành công; chưa chạy script bằng token môi trường bên ngoài |
| Visual | 18 ảnh ở desktop 1440, tablet 768, mobile 390; đã mở kiểm tra các màn và dialog đại diện |
| Git diff check | Pass |

Backend theo module: common-core 14, gateway 24, profile 166 (1 skip), company 219, recruitment 177. Các test live/visual được skip do điều kiện môi trường của suite cũ không được coi là đã chạy.

## Những hành vi đã chứng minh

- HR chưa có quyền nhận 403 CONTACT_LOCKED; HR công ty khác không lấy được contact.
- WITHDRAWN/REJECTED vẫn cấp quyền theo công ty; HR khác cùng công ty dùng được quyền đã duyệt.
- PENDING/REJECTED không cấp quyền; APPROVED cấp quyền; khi có cả hai căn cứ ưu tiên APPLICATION.
- Gửi đồng thời chỉ tạo một PENDING; duyệt đồng thời chỉ một lần thành công.
- Lý do trim trước kiểm tra độ dài; lý do từ chối lưu riêng, không ghi đè yêu cầu gốc.
- JSON chi tiết/danh sách HR không có email/phone thật; xem trạng thái không sinh log.
- Hai lần lấy contact thành công sinh đúng hai dòng audit; phone thật null vẫn hợp lệ.
- Dependency thất bại và lỗi PostgreSQL tại thời điểm commit không trả contact; HR bị khóa vẫn bị guard chặn.
- PENDING/REJECTED của hồ sơ được nhận diện đúng; hồ sơ không PUBLIC không cho xin quyền mới, quyền cũ không tự mất.
- Ứng viên chỉ xem log của chính mình; STAFF lọc công ty/ứng viên/thời gian với khoảng UTC nửa mở.
- UI không tự lấy contact; mỗi lần xem lại là hành động rõ ràng; không persist contact, không hiện response cũ khi đổi ứng viên.
- HTTP thật với JWT/RestClient/PostgreSQL qua ContactLiveFlowTest: xin mở → STAFF duyệt → xem hai lần → lịch sử ứng viên.
- Fixture SQL chạy trên migration PostgreSQL thật của identity/profile/company; chạy lại giữ quyết định STAFF.

## Quyết định trong triển khai

- Dùng worktree riêng và chỉ rebase các commit UT-03 lên origin/test mới nhất. Loại commit UT-01 chưa được publish khỏi PR này; checkout gốc không bị chỉnh sửa. Giá phải trả: cần kiểm chứng lại trên nền mới, đã thực hiện.
- Testcontainers không phát hiện được Docker trong môi trường này; dùng PostgreSQL 16 riêng qua UT01_TEST_DB_URL đã được repo hỗ trợ. Không dùng DB ứng dụng.
- Cho cấu hình PLAYWRIGHT_PORT, mặc định vẫn 3101; dùng 3113 để tránh server của checkout khác. Hành vi ứng dụng không đổi.
- CompanyHr không có tên HR; directory tái sử dụng IdentityClient.lookupUsers. Log vẫn giữ nguyên khi thiếu tên hoặc service metadata lỗi, hiển thị ID thay thế.
- Dùng ContactAccessLogs chung cho STAFF/ứng viên thay vì hai implementation lặp lại. Phân quyền vẫn ở BE, đã kiểm bằng role tests.
- Các flag Maven có dấu chấm được quote khi gọi từ PowerShell; không sửa build config.
- Thay việc mở thêm nhiều service nền bằng các test tự động dùng HTTP thật, upstream HTTP fixture và migration SQL thật. Không tuyên bố đã triển khai/chạy nguyên stack bên ngoài môi trường test.
- Theo yêu cầu không dùng subagent, review cuối là tự review. Hai lỗi quan trọng phát hiện khi review (trạng thái hồ sơ PENDING/REJECTED và thứ tự trim/validation) có regression test RED→GREEN; bộ backend sạch đã pass sau sửa. Không có lỗi nhỏ của UT-03 được để lại.

## Giới hạn đã thống nhất

UT-03 bảo vệ email/phone có cấu trúc trong JSON. Không tự sửa nội dung CV/PDF/cover letter do ứng viên viết. Log xác nhận server đã phục vụ contact, không xác nhận người dùng đã đọc được response.
