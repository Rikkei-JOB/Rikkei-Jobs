# UT-06 — Kết quả kiểm chứng

Kiểm tra cục bộ ngày 19/11/2026, nhánh `feature/UT-06-interview-scheduling`, đích tích hợp `test`.

| Kiểm tra | Kết quả |
| --- | --- |
| Maven recruitment-service và dependency modules | 141 test, 0 failure/error/skip: recruitment127 + common-core14 |
| Mock Express | 21/21 |
| Playwright UT-02/UT-06/UT-07 trên Edge | 11/11 |
| TypeScript | Pass |
| ESLint | 0 lỗi; 1 warning có sẵn tại commitlint.config.mjs |
| Next.js production build | Pass |
| OpenAPI | Parse được 5 path, mọi schema reference có đích |
| Compose | Render hợp lệ; recruitment gọi mock:9000, key khớp mock, dependency healthcheck đúng |
| Script demo PowerShell | Parse không lỗi |

Backend chạy trên PostgreSQL16 riêng qua UT01_TEST_DB_URL. `InterviewLiveFlowTest` dùng server Spring cổng ngẫu nhiên, Express Meet thực sự của repo và SMTP TCP sink trong test: tạo phòng rồi cố ý lỗi persistence, thử lại cùng phòng, xác nhận2 bên, slot huỷ409, lịch HR đúng, mốc1h gửi2 người đúng một lượt dù chạy job lại, sau đổi lịch có2 mail huỷ +1 lời mời mới. Identity/job/profile ở ngoài phạm vi test được mock; đây không phải bằng chứng gửi tới hộp thư Internet hay kiểm thử toàn bộ SSO/gateway.

Ảnh và UI test dùng fixtures, bao gồm viewport390px và trình duyệt múi giờ America/New_York để kiểm tra giờ Việt Nam. Weekly test giữ response tuần cũ, chuyển tuần và nhận đủ101 buổi qua2 trang của tuần mới, sau đó trả response cũ; UI vẫn giữ tuần mới.

## Review độc lập và sửa lỗi

Một lượt review read-only đối với commit09e7297, sau đó một lượt sửa có regression RED→GREEN và chạy lại toàn suite:

- Huỷ lịch khi worker bắt đầu gửi: huỷ/khóa hàng pending trước khi chụp người nhận đã thử gửi, không bỏ sót ICS CANCEL.
- Lease hết hạn khi sender vẫn chạy: reclaim/FAILED phải lấy được send key; claim_token UUID mới cho mỗi lượt, mọi ghi kết quả có fencing.
- Giữ giờ dự kiến UT-02 và slot PICKED của lịch DONE khi đơn chuyển trạng thái; chỉ xoá projection của lịch đang huỷ hoặc khi chủ động đề xuất lại.
- Từ chối cả epoch JSON dạng số và chuỗi số thiếu ISO offset, không thay đổi Jackson chung.
- SENT và reminded_*_at cùng transaction để không mất timestamp sau khi gửi người cuối.
- Thêm foreign slot thật, hai offset cùng instant, pick đua reschedule và delayed-week assertions. Biên còn3600.5s dùng so sánh Instant, không cắt phần giây lẻ.

Regression ghi nhận trực tiếp các lỗi trước sửa: numeric time trả201 thay400; worker gửi3 thay2 email khi lease hết hạn; thiếu cancellation cho confirmation đã thử; legacy time thànhnull; DONE slot thànhCANCELLED; milestone lỗi vẫn lưu tất cả SENT. Sau sửa các test tương ứng đều pass.

## Quyết định và giới hạn

| Quyết định | Lý do và đánh đổi |
| --- | --- |
| Dùng lệnh Windows và ledger thay helper Unix | Phù hợp máy hiện tại; theo dõi tiến độ thủ công |
| Domain lưu status string có CHECK ở DB | Theo pattern outbox có sẵn; ít kiểm tra enum tại compile-time hơn |
| PostgreSQL test riêng thay Testcontainers ở máy này | Client Testcontainers không thương lượng được API Docker hiện tại; cần quản lý container test thủ công, CI vẫn có fallback |
| Giữ scheduledAt trong API UT-02 cũ | Tránh phá client cũ; thời gian legacy vẫn là dự kiến, không tự có phòng/nhắc |
| Cài node_modules riêng trong worktree | Turbopack không nhận junction ngoài root; tốn thêm dung lượng |
| Giới hạn Hikari chỉ trong test: max5/minIdle0 | Nhiều Spring context cache làm hết connection PostgreSQL; giảm dung lượng pool test, không đổi production |
| Không chặn request HR để đợi SMTP | Lần gửi được coi là bắt đầu ở bước kiểm tra hợp lệ cuối; nếu đã bắt đầu trước huỷ thì có CANCEL theo sau qua send lock |
| Giữ giới hạn SMTP đã duyệt | Không có provider idempotency: crash sau SMTP nhận nhưng trước DB commit vẫn có thể trùng hoặc lệch thứ tự mail |
| Giữ giới hạn Meet đã duyệt | Không có API revoke; URL cũ có thể vẫn dùng được, idempotency mock mất sau reset/restart |
| Bổ sung Compose/CI sau snapshot review | Render và live-boundary test kiểm chứng wiring; backend CI cài thêm Node/mock dependencies; hosted CI chưa được quan sát tại thời điểm lập báo cáo |

Một điểm minor để lại: chi tiết vòng UT-02 chưa có nhãn riêng “lịch dự kiến” cho giờ legacy, và trạng thái lịch ở khối form không hiện khi vòng không còn chỉnh sửa. Lịch tuần HR và thẻ ứng viên vẫn thể hiện đúng trạng thái. Không có finding important/critical còn mở sau lượt sửa và bộ kiểm chứng cục bộ; chưa có lượt review thứ hai.
