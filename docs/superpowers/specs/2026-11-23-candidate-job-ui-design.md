# Thiết kế luồng ứng viên: tìm việc → chi tiết → ứng tuyển

Ngày: 23/11/2026. Trạng thái: người dùng đã duyệt; đã triển khai UI và nghiệm thu bằng Playwright/API fixtures. Xem [kết quả kiểm chứng](../../design/candidate-local/validation.md).

## Mục tiêu và hướng thiết kế

Giúp ứng viên tìm vị trí, đọc thông tin tuyển dụng và gửi CV trong khung ứng dụng Rikkei Jobs. Kế thừa cách phân cấp thông tin từ PTIT, điều chỉnh theo dữ liệu, phân quyền và luồng ứng tuyển hiện có của repo. Ưu tiên nội dung tuyển dụng thay cho banner quảng bá.

Người dùng đã chọn workflow Capture → Analyze → Specify → Implement → Validate và duyệt đặc tả này. Các giá trị màu, kích thước và bố cục bên dưới là thiết kế được duyệt cho vòng UI đầu, chưa phải hệ thống nhận diện toàn bộ sản phẩm.

### Các phương án

| Phương án | Lợi ích | Đánh đổi |
| --- | --- | --- |
| Sao sát bố cục PTIT, gồm banner và danh mục | Gần hình mẫu | Chiếm nhiều diện tích; repo chưa có dữ liệu danh mục và bộ lọc tương ứng |
| **Điều chỉnh PTIT vào khung ứng viên hiện có — đề xuất** | Tái sử dụng route, quyền, component và nghiệp vụ; đưa việc làm lên sớm | Khác PTIT ở menu, banner và bộ lọc |
| Tạo cổng việc làm công khai mới | Có thể tìm việc trước đăng nhập | Cần chốt API công khai và quyền; vượt phạm vi cải tiến UI hiện tại |

Phạm vi đề xuất: phương án thứ hai; chưa xây cổng công khai mới, chưa mở rộng backend. Đặc tả trình bày cả cấu trúc có thể mở rộng, nhưng vòng triển khai đầu chỉ dùng trường dữ liệu đang có.

## 01. Bằng chứng tham khảo

Nguồn đã mở bằng Playwright:

- Danh sách công khai: https://jobs.ptit.edu.vn/jobs
- Chi tiết đã quan sát: https://jobs.ptit.edu.vn/jobs/6ad6de815067945568f0d6ee
- URL người dùng cung cấp: https://jobs.ptit.edu.vn/jobs/all — hiện yêu cầu đăng nhập sinh viên.

Ảnh và số đo nằm tại [docs/design/ptit-reference](../../design/ptit-reference/). Chạy lại từ gốc repo bằng `node docs/design/ptit-reference/capture.cjs`, sử dụng Playwright đã có trong `web/node_modules`. File `observations.json` ghi URL, thời điểm, nội dung và computed styles; mỗi bước có ảnh viewport và toàn trang.

| Kích thước | Danh sách | Chi tiết | Sau khi bấm ứng tuyển |
| --- | --- | --- | --- |
| Desktop 1440 × 1000 | `desktop-01-jobs.png` | `desktop-02-detail.png` | `desktop-03-apply-login.png` |
| Tablet 834 × 1112 | `tablet-01-jobs.png` | `tablet-02-detail.png` | `tablet-03-apply-login.png` |
| Mobile 390 × 844 | `mobile-01-jobs.png` | `mobile-02-detail.png` | `mobile-03-apply-login.png` |

Ảnh bổ sung `desktop-04-all-login-required.png` ghi trạng thái chặn của `/jobs/all`. Không đăng nhập, không gửi hồ sơ lên PTIT. Chưa quan sát được chọn CV, form ứng tuyển hoặc màn thành công sau đăng nhập; các phần đó trong đặc tả dựa vào mã nguồn Rikkei Jobs.

## 02. Phân tích UI/UX

### Những gì đã quan sát ở PTIT

- Desktop có hai lớp header, hero giới thiệu, thanh tìm kiếm và banner tuyển dụng. Danh mục nằm trái; danh sách dùng thẻ có tên công ty, vị trí, hạn nộp, lương và nhãn loại việc.
- Thanh tìm kiếm gom địa điểm, ngành nghề, hình thức, mức lương, từ khóa và nút tìm kiếm. Đã thử chọn Hà Nội ở lượt khảo sát trước: URL chuyển thành `/jobs?location=H%C3%A0+N%E1%BB%99i`.
- Chi tiết desktop có cột nội dung lớn và cột thông tin công ty nhỏ. Tóm tắt vị trí, hạn nộp và CTA xuất hiện trước mô tả dài.
- Tablet 834px dùng header gọn có menu; danh mục thành hàng cuộn ngang; chi tiết là một cột. Đây là quan sát tại 834px, không phải breakpoint CSS gốc đã được xác minh.
- Mobile 390px xếp bộ lọc theo chiều dọc, CTA tìm kiếm toàn chiều ngang. Form lọc và banner đẩy danh sách xuống thấp.
- Bấm “Ứng tuyển ngay” khi chưa đăng nhập mở modal “Đăng nhập Sinh Viên”, có Huỷ/Đăng nhập.

Số đo computed styles từ lần chụp, không suy ra chỉ bằng mắt:

| Thành phần | Quan sát |
| --- | --- |
| Font | Be Vietnam Pro |
| Tiêu đề chi tiết desktop/tablet | 28px, weight 700, line-height 38.5px |
| Tiêu đề chi tiết mobile | 22px, weight 700, line-height 30.25px |
| Chữ tiêu đề chi tiết | RGB(5, 26, 83), tương đương #051A53 |
| Nút ứng tuyển | RGB(227, 0, 15), cao 51px, radius 8px, chữ 15px/700 |
| Nút tìm kiếm desktop | RGB(218, 37, 29), radius 12px, padding 14px 36px |

### Áp dụng vào Rikkei Jobs

Giữ: CTA đỏ rõ ràng, tóm tắt vị trí trước nội dung dài, thẻ nền trắng, tiêu đề có tương phản cao, bộ lọc có nhãn, chi tiết hai cột khi đủ rộng.

Điều chỉnh: tiêu đề giới thiệu ngắn, không dùng carousel quảng cáo; từ khóa đặt trước bộ lọc; danh sách là các thẻ theo hàng để đọc tên dài dễ hơn; không phụ thuộc hover để xem chi tiết; mobile có CTA cố định ở đáy khi modal chưa mở.

Không sao logo, mascot, banner, thông tin trường hoặc nội dung tuyển dụng PTIT vào sản phẩm. Không thêm số lượt xem, lượt ứng tuyển, nút yêu thích hoặc điểm phù hợp khi chưa có dữ liệu/chức năng tương ứng.

## Hiện trạng repo và ràng buộc dữ liệu

| Khu vực | Đã có | Hệ quả cho UI |
| --- | --- | --- |
| `candidate-jobs.tsx` | Một component cho cả list/detail; tiêu đề, công ty, deadline, CTA | Tách list và detail rõ ràng; tránh trang chi tiết chỉ là thẻ danh sách phóng lớn |
| `CandidateJob` | `jobId`, `companyId`, `jobTitle`, `companyName`, `deadline`, `acceptingApplications` | Vòng đầu chỉ tìm theo vị trí/công ty và trạng thái nhận hồ sơ |
| `candidateJobService` | List/get qua `NEXT_PUBLIC_JOB_API_PATH`; list trả mảng | Lọc trên tập dữ liệu đã tải; ghi rõ số kết quả của tập đó; không giả phân trang server |
| Route ứng viên | `/candidate/jobs`, `/candidate/jobs/[jobId]` yêu cầu cấu hình API; detail kiểm UUID | Giữ guard và kiểm ID; không ngầm chuyển sang dữ liệu fixture khi thiếu cấu hình |
| Link việc làm | `applicationLinks` mặc định `/jobs`, khác menu `/candidate/jobs` | Khi chạy luồng này cấu hình `NEXT_PUBLIC_JOB_PAGE_PATH=/candidate/jobs`; vẫn tôn trọng override hợp lệ |
| API JD chi tiết DN-02 | Có `Job` nhiều trường nhưng `GET /jobs/{id}` chỉ cho HR, JD thuộc công ty mình | Không dùng API HR để lấp dữ liệu thiếu cho ứng viên |
| Điểm phù hợp | Có `useMyJobMatch` và endpoint `/jobs/{id}/match/me` | Hoãn khỏi vòng đầu để không phụ thuộc thêm vào JD fixture hoặc tạo N+1 request cho list |
| `ApplyDialog` | CV bắt buộc; thư không bắt buộc tối đa 5000; snapshot PDF; chống gửi lặp; đối soát khi mất phản hồi | Tái sử dụng xử lý hiện tại, thay trình bày; không thêm bước xác nhận thứ hai |
| `AppShell` | Header 48px, sidebar 224px; menu mobile dưới 768px | Giữ điều hướng và bố cục dùng chung cho các vai trò |

Trường mở rộng như lương, thành phố, mô tả, yêu cầu, quyền lợi, logo và tên kỹ năng cần hợp đồng đọc dành cho ứng viên. Chúng không nằm trong điều kiện nghiệm thu vòng UI đầu. Khi có API đúng quyền, hiển thị theo cấu trúc bên dưới; không suy đoán dữ liệu thiếu từ tên công ty hay tên vị trí.

## 03. Design specification đề xuất

### Tokens

Phạm vi màu đặt dưới vùng giao diện việc làm ứng viên; không đổi `:root` làm thay giao diện HR/staff/admin. Portal của hộp ứng tuyển phải được gắn cùng scope/theme class trên Popup, vì không kế thừa CSS variables từ wrapper trong cây React.

| Token | Giá trị đề xuất | Dùng cho |
| --- | --- | --- |
| `--career-primary` | #B4232D | CTA chính, trạng thái chọn |
| `--career-primary-hover` | #941C25 | Hover CTA |
| `--career-primary-soft` | #FFF1F2 | Điểm nhấn, chip đang chọn |
| `--career-heading` | #18233A | Tiêu đề |
| `--career-text` | #334155 | Nội dung |
| `--career-muted` | #64748B | Metadata, trợ giúp |
| `--career-background` | #F6F7FA | Nền vùng nội dung |
| `--career-surface` | #FFFFFF | Card, popup |
| `--career-border` | #E2E8F0 | Phân vùng trang trí |
| `--career-control-border` | #64748B | Viền control cần nhận diện rõ |
| `--career-success` | #166534 | Thành công/đang nhận, luôn kèm chữ |
| `--career-warning` | #92400E | Cảnh báo, luôn kèm chữ |

Giữ Inter hiện có và Lucide icons; kiểm tra dấu tiếng Việt khi render. Type scale: h1 desktop 30/40 weight 700, mobile 24/32; h2 20/28 weight 600; tên việc 18/26 weight 600; body/control 14/22 hoặc 16/24; metadata 12/18. Không dùng metadata nhỏ cho hướng dẫn quan trọng.

Spacing: 4, 8, 12, 16, 24, 32, 40px. Card radius 12px, popup 16px, control 8px; padding card 24px desktop và 16px mobile; khoảng cách giữa card 16px. Shadow nhẹ `0 2px 8px rgb(15 23 42 / 0.04)`. Control cao tối thiểu 44px; icon 20px trong vùng bấm tối thiểu 44 × 44px. Focus ring 2px và offset 2px. Kiểm tra contrast chữ thường ≥4.5:1 và control/focus ≥3:1 ở bước validation.

### Bố cục danh sách

1. Đầu trang: eyebrow “CƠ HỘI NGHỀ NGHIỆP”, h1 “Tìm công việc phù hợp”, mô tả một câu; link “Ứng tuyển của tôi”. Không thêm khối hero lớn.
2. Search form: nhãn “Vị trí hoặc công ty”, input, nút “Tìm kiếm”; trạng thái nhận hồ sơ bằng select. Dữ liệu hiện có không hỗ trợ lọc địa điểm/lương/ngành nên các control đó chưa xuất hiện.
3. Dòng kết quả: “N vị trí” (hoặc “N vị trí trong danh sách đã tải” nếu cần phân biệt nguồn); nút xoá bộ lọc khi có lọc.
4. JobCard: biểu tượng công ty dạng chữ cái từ tên công ty (không coi là logo thật), tên vị trí là link, công ty, deadline, nhãn “Đang nhận hồ sơ”/“Đã đóng”, link “Xem chi tiết”, ApplyButton.
5. Empty/error/loading thay phần kết quả; giữ search form để sửa tìm kiếm.

Tìm theo tên vị trí/công ty không phân biệt hoa thường và dấu; URL lưu `q` và `receiving=all|open|closed`, Enter hoặc nút Tìm kiếm áp dụng; reset xoá hai tham số này, giữ tham số không liên quan. Reload và Back/Forward khôi phục input cùng kết quả. Không truy vấn lại API theo từng phím khi chỉ lọc mảng đã tải.

### Bố cục chi tiết

Breadcrumb “Việc làm / [Tên vị trí]”. JobSummary gồm tên vị trí, tên công ty, deadline, trạng thái nhận và CTA. Vòng dữ liệu tối thiểu có thêm một khối “Chuẩn bị ứng tuyển”: chọn CV phù hợp; thư giới thiệu không bắt buộc; CV đã gửi được lưu thành bản chụp PDF. Không giả phần mô tả công việc.

ActionAside ở desktop: tên công ty (không giả quy mô/địa chỉ), hạn nộp, nút ứng tuyển và link “Ứng tuyển của tôi”. Khi hợp đồng ứng viên có mô tả/yêu cầu/quyền lợi, JobContentSections hiển thị phía trái và aside giữ bên phải. Trường trống bỏ section; không hiển thị chuỗi placeholder như nội dung thật. Text dài wrap; nội dung chi tiết không bị line-clamp.

CTA desktop nằm trong aside sticky bên dưới header; mobile ở thanh đáy có safe-area padding, phần nội dung chừa khoảng cao tương ứng. Không có CTA fixed khi modal mở. Nút disabled và nhãn rõ khi ngừng nhận hồ sơ; quyết định cuối về điều kiện nộp vẫn do API.

### Hộp ứng tuyển và kết quả

Giữ modal một bước của repo: tiêu đề “Ứng tuyển vị trí này”, thông tin vị trí nếu đã có ở trang gọi, chọn CV bắt buộc, thư giới thiệu optional, bộ đếm 5000 ký tự, ghi chú snapshot và Huỷ/Gửi ứng tuyển.

CV dùng select có label, hiển thị name + targetPosition khi có. Không thêm upload hoặc xem trước PDF vì adapter hiện chỉ trả metadata CV. Link quản lý/tạo CV lấy từ `applicationLinks.cvs`; nếu route thực chưa được cấu hình, phải ghi thành phụ thuộc tích hợp, không tạo trang quản lý CV giả.

| Trạng thái | Hành vi |
| --- | --- |
| Tải CV | Thông báo live, khoá gửi |
| Không có CV | Hướng dẫn tạo CV, link theo config; khoá gửi |
| Lỗi tải CV | Inline alert + Tải lại CV; giữ thư đã nhập |
| Chưa chọn CV | Khoá gửi; label và hướng dẫn dễ hiểu |
| Đang gửi | “Đang xử lý…”, chặn double submit, khoá đóng và input theo xử lý hiện có |
| Validation/409/503 | Hiện thông báo từ API, giữ CV/thư; có đường xem đơn đã gửi |
| Mất phản hồi, chưa rõ có ghi đơn | Giữ cơ chế “Kiểm tra kết quả”, không cho gửi lặp đến khi đối soát |
| Thành công 201 | Điều hướng tới `/candidate/applications/{id}` như hiện tại; trạng thái “Đã gửi” là xác nhận |

Base UI Dialog quản lý focus trap, trả focus về trigger, title/description; Esc đóng khi không busy. Mobile popup có chiều rộng `calc(100% - 32px)`, max-height theo dvh và overflow nội bộ; phần nút gửi vẫn tiếp cận được khi mở bàn phím. Không tắt zoom input, không đặt autofocus làm trang nhảy cuộn ngoài ý muốn.

### Component tree

```text
AppShell (hiện có, giữ điều hướng)
└─ CandidateCareerScope
   ├─ CandidateJobsList
   │  ├─ CareerPageHeader
   │  ├─ JobSearchForm
   │  ├─ ResultsSummary
   │  └─ JobCard[] | LoadingState | EmptyState | ErrorState
   └─ CandidateJobDetail
      ├─ Breadcrumb
      ├─ JobSummary
      ├─ ApplicationPreparation
      ├─ JobActionAside / MobileApplyBar
      └─ ApplyButton → ApplyDialog (tái sử dụng logic)
         ├─ CV select / CV loading / empty / retry
         ├─ Cover letter + counter
         ├─ Submission error / uncertain-result recovery
         └─ Huỷ / Gửi ứng tuyển
```

Tên component là định hướng, builder có thể gộp các khối quá nhỏ. Không tạo abstraction hoặc dependency mới chỉ để khớp cây này. `JobContentSections` là phần mở rộng sau khi có hợp đồng dữ liệu, chưa render trong vòng đầu.

### Responsive rules

Ngưỡng bên dưới là thiết kế Rikkei đề xuất theo viewport và chiều rộng còn lại sau AppShell, không phải breakpoint gốc PTIT.

| Viewport | Quy tắc |
| --- | --- |
| <768px, kiểm tra 390px và mép 360px | Menu mobile hiện có; padding vùng nội dung 16px; search xếp dọc; card một cột; nút quan trọng đủ rộng; chi tiết một cột; thanh ứng tuyển đáy |
| 768–1199px, kiểm tra 834px | Sidebar hiện có 224px; vùng còn lại dùng một cột; search được wrap; detail aside trở thành card trong flow; không ép hai cột trong khoảng rộng hẹp |
| ≥1200px, kiểm tra 1440px | Giới hạn nội dung 1120px trong vùng còn lại; list dùng hàng ngang trong từng card; detail grid `minmax(0,1fr) 280px`, gap 24px |

Không tạo sidebar lọc thứ hai bên cạnh sidebar ứng dụng. Dòng title/company không tràn ngang, metadata wrap tự nhiên; dialog không vượt viewport. Không đặt width cố định cho cột chính. Giữ menu hiện có đóng khi đổi route.

### Route, dữ liệu và giới hạn triển khai

- Giữ route hiện tại, authentication và role CANDIDATE; không đổi sang public route chỉ vì nguồn tham khảo là public.
- Giữ `CandidateJob` và endpoint cấu hình; cấu hình thử nghiệm `NEXT_PUBLIC_JOB_PAGE_PATH=/candidate/jobs`. Thiếu `NEXT_PUBLIC_JOB_API_PATH` vẫn là trạng thái chưa tích hợp, không thay bằng dữ liệu mẫu trong ứng dụng.
- Chỉ dùng Playwright route interception/fixture cho preview và nghiệm thu khi producer chưa có; ảnh local phải ghi rõ “API fixture”. Không gọi kết quả đó là tích hợp backend thật.
- Không thay API HR, không gọi `/internal/**` từ browser, không đưa internal key ra frontend.
- Không sửa snapshot CV, transition đơn, permission, hoặc trạng thái backend trong nhiệm vụ UI.
- Dark theme chưa nằm trong vòng nghiệm thu đầu; scoped career surface dùng bảng sáng rõ ràng, không gây đổi theme các trang ngoài phạm vi.

## 04–05. Phạm vi triển khai và tiêu chí nghiệm thu sau khi duyệt

Sau khi duyệt tài liệu, lập implementation plan rồi sửa UI danh sách, chi tiết và ApplyDialog. Tái sử dụng Next.js/React/Tailwind/Base UI, Inter và Lucide sẵn có. Đọc docs Next.js local theo `web/AGENTS.md` trước khi code.

1. Danh sách → chi tiết → mở modal → chọn CV → gửi → trang đơn hoạt động với fixture kiểm thử và route thật.
2. Các bộ lọc có dữ liệu thực tương ứng; F5/Back/Forward khôi phục URL; đóng nhận hồ sơ không gửi được.
3. Xác minh loading, rỗng, lỗi/retry, chưa có CV, thư dài, tên vị trí/công ty dài, lỗi khi submit và mất phản hồi.
4. Playwright chụp lại 1440 × 1000, 834 × 1112, 390 × 844 và kiểm tra tràn tại 360px. Chụp list/detail/modal và trạng thái bất thường quan trọng; lưu riêng ảnh PTIT, ảnh local và ảnh sau sửa.
5. So sánh với đặc tả đã duyệt về hierarchy, spacing, typography, màu, vị trí CTA; ảnh PTIT dùng làm tham khảo, không dùng pixel-diff làm điều kiện bắt buộc vì layout và font chủ đích khác nhau.
6. Keyboard: Tab/Shift+Tab trong modal, Esc khi không busy, focus trở về trigger; form có label, alert/live status, không chỉ dùng màu để diễn đạt trạng thái.
7. Chạy type-check, lint và các Playwright tests liên quan UT-01/auth/navigation; bổ sung checks hành vi tìm kiếm và responsive nơi cần. Production build theo quy trình repo; không chạy backend suite nếu không đổi backend.
8. Không đụng các thay đổi đang có của người dùng: `mock/data.json`, `mock/test/jd-cv.test.js`, root package files/node_modules và mọi thay đổi ngoài phạm vi.

## Các quyết định cần duyệt

Đề xuất triển khai vòng đầu với màu đỏ #B4232D, giữ AppShell hiện có, UI tìm kiếm theo dữ liệu tối thiểu, modal CV một bước và CTA mobile cố định. Các trường JD mở rộng, bộ lọc lương/địa điểm/ngành và tích hợp CV producer được giữ thành phụ thuộc riêng. Duyệt bản này xác nhận phạm vi UI đầu; không xác nhận các API chưa tồn tại.
