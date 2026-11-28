# Rikkei Jobs — Quy chuẩn thiết kế giao diện

**Phiên bản 1.1 · Cập nhật 25/11/2026 · Áp dụng cho toàn bộ frontend.**

Đây là tài liệu chuẩn để cả team và Codex thiết kế, triển khai và review UI. Phong cách chung lấy từ giao diện ứng viên đã được duyệt: đỏ trầm, nền sáng, chữ rõ, khoảng cách thoáng và thông tin tuyển dụng dễ quét. Các vai trò dùng chung tokens và component; bố cục điều hướng được chọn theo nhiệm vụ của từng vai trò.

## 1. Cách áp dụng

- Đọc tài liệu này trước khi tạo hoặc sửa UI. Màn mới tuân thủ chuẩn; màn cũ chuyển dần trong phạm vi đang chỉnh sửa.
- Tái sử dụng component và pattern đang có trước khi tạo mới. Bổ sung biến thể vào component chung khi nhiều màn cần cùng một cách trình bày.
- Thay đổi token, component hoặc pattern chung phải cập nhật tài liệu này và kiểm tra các màn sử dụng trong cùng PR. Nêu rõ lý do nếu cần ngoại lệ theo nghiệp vụ.
- Ảnh tham khảo giúp chọn hướng thẩm mỹ. Nghiệm thu theo tài liệu này, nghiệp vụ và thiết kế đã duyệt cho màn đó.

### Chuẩn đích và hiện trạng

Tài liệu quy định **chuẩn đích**, không khẳng định các màn đã đồng nhất. `globals.css` khai báo tokens chung `--brand`, `--brand-hover`, `--brand-soft`, `--heading`, `--body`, `--text-muted`, `--surface`, `--surface-border`, `--control-border`, `--page-background`. HR ánh xạ các tokens này sang semantic theme trong `HrShell`; ứng viên còn CSS tương thích riêng. Theme mặc định của vai trò khác và Button chung vẫn cần chuyển dần.

Khi chuẩn hoá code, tập trung tokens vào `web/src/app/globals.css` và các biến thể vào `web/src/app/components/ui/`. Scope ứng viên hiện có là lớp tương thích trong giai đoạn chuyển đổi; map các giá trị đó về theme chung khi chuyển module, thay vì sao chép một bộ màu mới cho mỗi vai trò. Thay đổi theme toàn cục cần kiểm tra cả component cũ dùng theme đó.

## 2. Nguyên tắc thị giác

1. **Rõ hành động chính:** mỗi khu vực tác vụ có một hành động chính nổi bật, nhãn mô tả rõ kết quả như “Gửi ứng tuyển”, “Lưu thay đổi”.
2. **Ưu tiên thông tin quyết định:** vị trí, doanh nghiệp, trạng thái, hạn nộp và hành động xuất hiện trước nội dung phụ.
3. **Màu có mục đích:** đỏ trầm dùng cho thương hiệu, điều hướng chọn và CTA; trạng thái nghiệp vụ dùng màu ngữ nghĩa.
4. **Nhất quán về nhịp:** dùng thang chữ, khoảng cách, bo góc và component trong tài liệu này; giữ các cạnh nội dung thẳng hàng.
5. **Dễ sử dụng ở mọi kích thước:** hỗ trợ chuột, bàn phím, cảm ứng, chữ tiếng Việt và nội dung dài.

## 3. Design tokens

### Màu sắc

| Vai trò | Giá trị | Quy tắc sử dụng |
| --- | --- | --- |
| Brand / primary | `#B4232D` | CTA chính, link, mục điều hướng đang chọn |
| Primary hover | `#941C25` | Hover CTA trên nền sáng |
| Primary soft | `#FFF1F2` | Nền nhấn nhẹ, avatar, mục menu chọn |
| Page background | `#F6F7FA` | Nền vùng làm việc |
| Surface | `#FFFFFF` | Card, form, table, popup |
| Heading | `#18233A` | Tiêu đề và thông tin quan trọng |
| Body | `#334155` | Nội dung và hướng dẫn |
| Muted | `#64748B` | Metadata trên nền trắng; kiểm tra tương phản khi dùng nền khác |
| Border | `#E2E8F0` | Đường phân vùng và viền card |
| Control border | `#64748B` | Viền input/select/textarea cần nhận diện rõ |
| Success | `#166534` trên `#F0FDF4` | Thành công hoặc đang nhận hồ sơ |
| Warning | `#92400E` trên `#FFFBEB` | Cần chú ý, gần hạn, đang chờ thao tác |
| Error / destructive | `#B91C1C` trên `#FEF2F2` | Lỗi hoặc thao tác phá huỷ |
| Info | `#1D4ED8` trên `#EFF6FF` | Hướng dẫn và thông tin hệ thống |

Theme chung là nơi hiện thực màu runtime: `--primary`, `--primary-foreground`, `--background`, `--foreground`, `--card`, `--muted-foreground`, `--border`, `--input`, `--ring`… Khi chuẩn hoá, bổ sung `--heading` cho màu tiêu đề và mapping `--color-heading: var(--heading)` trong `@theme inline` để dùng `text-heading`. Các token mới khác cũng cần mapping trước khi dùng utility tương ứng. Nút primary dùng chữ trắng. `--ring` theo primary trên nền sáng; link trên banner đỏ dùng focus trắng.

Màu quá trình ứng tuyển tiếp tục lấy từ `web/src/config/application-status.ts`: Đã gửi xanh dương, Sàng lọc vàng, Phỏng vấn tím, Offer cyan, Nhận việc xanh lá, Không đạt đỏ, Đã rút xám. Khi bổ sung trạng thái, cập nhật mapping chung; badge luôn có nhãn chữ.

### Typography

Font chung **Lexend** (font biến thiên, subset `latin`, `latin-ext`, `vietnamese`), lấy từ root layout; dùng chung `font-sans`, không ghi cứng tên font trong component (SVG dùng `var(--font-sans)`). Lexend rộng và đậm hơn Inter: tiêu đề dùng 600–700, hạn chế 800–900; ngoại lệ là tiêu đề hero trang công khai (800, 32–54px, phần nhấn mạnh tô chuyển màu). Kiểm tra dấu tiếng Việt, chữ đậm và số trên trình duyệt thực tế.

| Loại chữ | Desktop: size / line-height | Mobile | Weight |
| --- | --- | --- | --- |
| H1 trang | 30 / 40px | 24 / 32px | 700 |
| H1 banner | 30 / 36px | 24 / 30px | 700 |
| H2 khu vực | 20 / 28px | 20 / 28px | 600 |
| Tên việc / tiêu đề card | 18 / 26px | 18 / 26px | 600 |
| Nội dung và control | 14 / 22px | 14 / 22px | 400–500 |
| Nội dung đọc dài | 16 / 24px | 16 / 24px | 400 |
| Label | 14 / 22px | 14 / 22px | 600 |
| Metadata, trợ giúp | 12 / 18px | 12 / 18px | 400–500 |

Input/select/textarea trên mobile dùng font **16px** để tránh trình duyệt tự zoom. Eyebrow trang trí có thể dùng 10–11px, uppercase, letter-spacing nhẹ; thông tin quan trọng vẫn dùng cỡ chữ đọc được. Mỗi trang có một H1, các cấp tiêu đề phản ánh cấu trúc nội dung.

### Khoảng cách, hình dạng và hiệu ứng

| Thuộc tính | Chuẩn |
| --- | --- |
| Spacing scale | 4, 8, 12, 16, 24, 32, 40px |
| Khoảng giữa label và control | 8px |
| Khoảng giữa field | 20–24px |
| Khoảng giữa card | 16px; giữa khu vực lớn 24–32px |
| Padding card | 24px desktop, 16px mobile |
| Radius | Control 8px; card 12px; modal/banner 16px; avatar tròn |
| Shadow card | `0 2px 8px rgb(15 23 42 / 0.04)` |
| Shadow popup | `0 12px 40px rgb(15 23 42 / 0.12)` |
| Focus | Outline/ring 2px, offset tối thiểu 2px; tương phản rõ với nền |
| Motion | 150–200ms cho hover/open nhẹ; tôn trọng `prefers-reduced-motion` |

Giữ bề mặt form/list ổn định khi tải dữ liệu. Gradient dành cho banner thương hiệu, nội dung tác vụ dùng surface trắng.

## 4. Nền tảng component

Stack hiện có: **Next.js, React, TypeScript, Tailwind, shadcn với Base UI, Lucide**. Dùng Lexend và Lucide cho UI mới. Ưu tiên component hiện có; thêm dependency khi công cụ sẵn có không đáp ứng được nhu cầu cụ thể.

| Component | Quy tắc chung |
| --- | --- |
| Button | Primary/secondary/ghost/destructive có vai trò rõ; control chính cao ≥44px; nhãn hành động cụ thể; busy chặn gửi lặp |
| Input/select/textarea | Label luôn hiện; placeholder là ví dụ; lỗi cạnh field, liên kết bằng `aria-describedby`; đánh dấu field bắt buộc bằng chữ/thuộc tính phù hợp |
| Card | Nền trắng, radius 12px, border nhẹ; tiêu đề và hành động có cùng vị trí giữa các card cùng loại |
| Badge | Chữ + màu ngữ nghĩa; có thể thêm icon/dot; nhãn và màu trạng thái lấy từ mapping chung |
| Table | Header rõ, nội dung căn trái; số và tiền căn phải; thao tác hàng nhất quán; mobile cuộn trong vùng bảng có nhãn hoặc chuyển sang card |
| Modal | Base UI Dialog; title/description, focus trap, Esc khi không busy, trả focus sau đóng; body cuộn, footer hành động tiếp cận được trên màn hình thấp |
| Menu/popover | Base UI; trigger có tên, trạng thái mở rõ; hỗ trợ bàn phím, Esc và đóng khi chọn/chuyển trang |
| Notification | Badge số chưa đọc, loading/rỗng/lỗi, thời gian; cập nhật trạng thái từ server; phân biệt lỗi tải với không có thông báo |
| Tabs | Dùng component chung, hỗ trợ bàn phím; tab biểu thị các phần cùng ngữ cảnh |

Tái sử dụng trực tiếp các exports trong `web/src/app/components/ui/`: `Button`, `Input`, `Label`, `Card`, `Tabs`, `Alert`. Hiện chưa có wrapper chung cho mọi modal, table hoặc notification; dùng primitive Base UI và pattern đã có, rồi trích xuất khi thực sự có nhu cầu dùng chung. Component nghiệp vụ ở thư mục tính năng, token/layout nền tảng ở lớp dùng chung.

Ví dụ dùng component và semantic utility sau khi theme đã được chuẩn hoá:

```tsx
<Card className="rounded-xl border bg-card p-4 md:p-6">
  <h2 className="text-xl font-semibold text-heading">Thông tin ứng tuyển</h2>
  <p className="mt-2 text-sm text-muted-foreground">Chọn CV phù hợp với vị trí.</p>
  <Button className="mt-6 min-h-11 px-5 text-sm">Gửi ứng tuyển</Button>
</Card>
```

## 5. Layout và responsive

| Viewport | Bố cục |
| --- | --- |
| <768px | Một cột; padding ngang 16px; bộ lọc xếp dọc; thao tác chính dễ bấm |
| 768–1199px | Padding ngang 24px; cho phép wrap; chọn số cột theo chiều rộng thực còn lại sau điều hướng |
| ≥1200px | Khung nội dung căn giữa tối đa 1200px; khu vực việc làm hiện dùng 1120px; dùng hai cột khi đủ chỗ |

Breakpoint desktop của sản phẩm là **1200px**. Tailwind `xl` mặc định là 1280px; dùng `min-[1200px]` hoặc media query 1200px khi cần đúng ngưỡng này.

- **Ứng viên:** top bar sticky 64px; logo, menu, chuông, tài khoản. Dưới 1200px dùng menu thu gọn, chuông luôn tiếp cận được.
- **Offer ứng viên:** phản hồi tại trang chi tiết mở từ email/thông báo, bắt buộc đăng nhập đúng tài khoản; không có menu hoặc danh sách Offer riêng. Trang chi tiết quay lại hồ sơ ứng tuyển. Đổi tài khoản phải đồng bộ các tab và xoá cache của phiên trước.
- **HR:** top bar sticky 64px, menu Tổng quan (chỉ sáng khi đúng `/hr`) / Tin tuyển dụng / Hồ sơ ứng tuyển / Lịch phỏng vấn / Thực tập sinh; menu tài khoản chứa công ty, đổi mật khẩu, đăng xuất. Dưới 1200px thu gọn menu. Khung nội dung tối đa 1200px (padding ngoài 24px). Thông báo HR chờ API riêng; không dùng API thông báo ứng viên.
- **Bố cục theo vai trò (cùng nền sáng, cùng token, chỉ khác điều hướng; `layout/role-shells.tsx` + `role-shells.css`, tiền tố `rs-`):** HR giữ thanh ngang (`HrShell`). Mentor (`MentorShell`): sidebar trắng 248px, mục đang mở là viên đỏ nhạt, thẻ tài khoản ở đáy, không có thanh trên trên desktop. Cán bộ (`StaffShell`): thanh trên toàn chiều ngang + rail icon 92px (nhãn dưới biểu tượng, ngăn cách theo `ROLE_NAV_GROUPS.STAFF`). Quản trị (`AdminShell`): sidebar xám nhạt `#f8fafc` có tiêu đề nhóm (`ROLE_NAV_GROUPS.ADMIN`), mục đang mở là thẻ trắng có vạch đỏ trái, thanh trên có breadcrumb. Dưới 1024px mọi khung ẩn sidebar/rail, dùng thanh trên với nút "Mở menu". Nội dung trang vẫn nằm trong `.hr-main` (Mentor, Quản trị) hoặc `.manager-main` (Cán bộ); không dùng sidebar tối.
- **Trang Mentor (`components/mentor/`):** Tổng quan `/mentor` (lời chào, 4 thẻ số liệu, lịch sắp tới, cần chấm, đánh giá gần đây), Lịch `/mentor/schedule` (dải 7 ngày + danh sách theo ngày), Đánh giá `/mentor/reviews` (tab theo trạng thái, khung chấm điểm, mở thẳng một vòng bằng `?round=`), Hồ sơ `/mentor/profile` (mở từ menu tài khoản; họ tên, email từ LMS chỉ xem; chức danh, đơn vị, kinh nghiệm, liên hệ, chuyên môn, giới thiệu lưu qua `/profiles/work/me`, component chung `components/work-profile/`). Ba trang dùng chung cache `useMentorRounds`; tiền tố CSS `mtr-`.
- **Trang công khai (trang chủ, `/jobs`, `/companies`):** `PublicHeader` + `SiteFooter`, hero tối đỏ-navy có ô tìm kiếm lớn. Bảng việc làm (`components/job-board`) ở `/jobs`: bộ lọc nằm trên URL, danh sách trái + xem nhanh phải từ 1024px, dưới 1024px bấm thẻ mở trang chi tiết. Dữ liệu chỉ lấy từ API `/public/**` của company-service, không có dữ liệu mẫu ở frontend.
- **Minh hoạ trang ứng viên (`components/job-board/illustrations.tsx`):** SVG tự vẽ, `aria-hidden`, màu theo bảng màu job-board, chữ trong hình dùng `var(--font-sans)`. Không dùng ảnh chụp hay hình tải từ nguồn ngoài. Hình hero chỉ hiện từ 1024px; trạng thái trống dùng `EmptySearchArt` thay icon.
- **Ô chọn thả xuống:** dùng `SelectMenu` (`components/ui/select-menu.tsx`, dựa trên Select của Base UI), không dùng `<select>` của trình duyệt vì danh sách mặc định không đổi kiểu được. Trong form react-hook-form, nối qua `Controller`; trường rỗng có `placeholder`.
- **Khu Admin (`components/admin/`, khung `AdminShell`):** menu Tổng quan · Tài khoản · Duyệt JD · Đơn ứng tuyển · Kỹ năng; hồ sơ ở `/admin/profile`. Tài khoản: tab theo vai trò kèm số đếm, khoá/mở khoá ngay trong dòng (bắt buộc lý do; không tự khoá mình, không khoá quản trị), API `/admin/users/**` (identity). Kỹ năng: lọc theo nhóm, thêm/sửa tên và nhóm, mã không đổi và không xoá, API `/admin/skills/**` (profile). Duyệt JD dùng lại component của Staff với `base="/admin/jobs"`. Tiền tố CSS `adm-`.
- **Khung Staff (`StaffShell`):** logo kèm nhãn "CÁN BỘ TUYỂN DỤNG", menu tài khoản có "Hồ sơ cá nhân" (`/staff/profile`, cùng component hồ sơ nghề nghiệp với Mentor), footer `SiteFooter compact` ở đáy. Mọi trang Staff mở đầu bằng `TaskBanner` compact như HR/Mentor; nút hành động đặt trong banner (`data-primary` cho nút chính nền trắng).
- **Staff Tổng quan `/staff` và Duyệt JD `/staff/jobs` (DN-07, `components/staff-jobs/`):** Tổng quan gom số liệu từ API sẵn có (JD, hồ sơ chờ duyệt, đơn ứng tuyển, doanh nghiệp, lần đồng bộ). Duyệt JD: thẻ trạng thái kèm số đếm, bảng (thẻ trên mobile), trang chi tiết có danh sách tự kiểm tra, Duyệt (xác nhận 2 bước) hoặc Trả lại kèm lý do chọn nhanh, sau khi xử lý mở JD chờ duyệt tiếp theo. API `/staff/jobs/**` (company-service). Tiền tố CSS `sjr-`, `sov-`.
- **Khu HR:** `/hr` là bảng điều khiển (`components/hr-dashboard/`, tiền tố CSS `hrd-`): banner chào kèm tóm tắt, 4 thẻ số liệu dẫn tới danh sách đã lọc, dải 5 bước quy trình ứng tuyển, hai cột "Phỏng vấn sắp tới" (14 ngày) và "Tin cần chú ý" (bị trả lại, còn ≤7 ngày hạn nộp), thẻ hồ sơ công ty. Mọi số liệu lấy từ API sẵn có, không ghi số mẫu. `/hr/jobs`, `/hr/company`, `/hr/interns` mở đầu bằng `TaskBanner` có minh hoạ riêng; liên kết chính trong banner dùng `data-primary`. `/hr/interns` liệt kê hồ sơ HIRED thật; bước theo dõi/nghiệm thu ghi "Sắp ra mắt" cho tới khi có backend thực tập.
- **Đăng nhập (`/login`):** khung `(auth)/layout.tsx` gắn `.auth-theme` (token đỏ như `.admin-theme`), header chỉ có liên kết thật (Việc làm IT, Công ty, Trang chủ), không đặt "pill" vai trò không bấm được. Từ 1024px chia 2 cột: `LoginShowcase` (minh hoạ, ai đăng nhập bằng cách nào) và `AuthCard` 440px. Tab mở theo `?tab=hr` (liên kết "Đăng nhập doanh nghiệp" dùng đường dẫn này). Chưa cấu hình `NEXT_PUBLIC_LMS_LOGIN_URL` mà bật mock thì danh sách tài khoản mẫu là lối chính, không hiện nút bị vô hiệu. Tab dùng chung: Base UI 1.9 đánh dấu tab đang chọn bằng `aria-selected`, CSS phải bắt cả `[aria-selected=true]`.
- **Footer (`SiteFooter`):** bản đầy đủ nền tối `#0b1024` với vạch màu thương hiệu, logo `BrandLogo inverse`, các cột "Dành cho ứng viên", "Nhà tuyển dụng", "Liên hệ" (trang công khai, đăng nhập, ứng viên). Bản `compact` là một hàng sáng (logo, liên kết nhanh, bản quyền) cho khung HR/Mentor/Admin.
- **Ứng viên dùng chung header và footer với trang công khai:** `CandidateShell` chỉ gồm `PublicHeader` + nội dung + `SiteFooter`. Khi ứng viên đăng nhập, `PublicHeader` hiện menu Việc làm IT · Công ty · Ứng tuyển ▾ · Hồ sơ ▾ · Phát triển ▾, chuông thông báo, menu tài khoản; dưới 1024px gom vào nút ☰. Trang chủ của ứng viên là `/` (`ROLE_LANDING`); `/candidate` và `/candidate/jobs[/id]` chuyển hướng ở `next.config.ts` về `/` và `/jobs[/id]`, nên chỉ có một trang việc làm.
- **Staff:** dùng `StaffShell` (thanh trên + rail icon, thay `ManagerShell` của DN-05); áp dụng tokens/component chung khi sửa màn.
- **Trang chi tiết:** nội dung chính và aside 280px khi đủ rộng; tablet về một cột. Sticky aside lấy offset theo header, dùng `--app-sticky-offset` hiện có.
- **CTA đáy mobile:** chừa padding nội dung cùng safe-area; modal mở thì CTA nền được ẩn; sau đóng trả focus khi control đã hiện lại.
- **Lớp hiển thị:** nội dung thông thường; CTA đáy 15; header ứng viên 30; menu/popover 40; modal/backdrop 50. Kiểm tra stacking context trước khi tăng z-index.
- **Banner tìm việc:** gradient `#8F1D2B → #B4232D`, minh hoạ SVG công nghệ; cao 220px desktop/tablet và 180px mobile. Banner đặt trên thanh tìm kiếm.
- **Banner ứng tuyển/phỏng vấn:** dùng `ui/task-banner.tsx` và CSS module, gradient cùng phong cách banner tìm việc, SVG chỉ trang trí (`illustration`: applications, interviews, company, jobs, dashboard, interns; cùng bảng màu giấy `#FFF5F2`, đỏ `#B4232D`, điểm nhấn vàng `#F2CB87`). Liên kết hoặc nút chính trong banner đặt `data-primary` (nền trắng, chữ đỏ). Giữ tên trang làm H1 duy nhất; mô tả và CTA/thông tin múi giờ bên trái. Ứng viên min-height 200px, HR compact min-height 160px; chiều cao tự tăng khi nội dung dài. Mobile padding 20px, H1 24/30px, ẩn minh hoạ để ưu tiên chữ. Bộ lọc, tabs và chuyển Tuần/Danh sách đặt dưới banner trên surface sáng. Ảnh trước/sau tại `docs/design/task-banners/`.

Trang không tràn ngang ở 360px. Tên vị trí/công ty và nội dung dài được wrap; phần chi tiết hiển thị đầy đủ. Giới hạn cuộn ngang trong vùng bảng khi thật sự cần, giữ các hành động chính có thể truy cập.

## 6. Mẫu màn hình và trạng thái

| Mẫu | Thứ tự thông tin |
| --- | --- |
| Danh sách | Tiêu đề hoặc banner → tìm kiếm/bộ lọc → số kết quả → list/table → phân trang |
| Chi tiết | Breadcrumb → tóm tắt/trạng thái → nội dung → hành động chính; desktop có thể dùng aside |
| Form | Tiêu đề/mục đích → các nhóm field → hướng dẫn/lỗi → Huỷ và Lưu/Gửi |
| Dashboard | Chỉ số có dữ liệu thật → công việc cần xử lý → danh sách/biểu đồ hỗ trợ quyết định |

Mỗi màn có đủ các trạng thái phù hợp:

- **Loading:** skeleton theo bố cục hoặc thông báo `role="status"`; khoá hành động phụ thuộc dữ liệu đang tải.
- **Rỗng:** giải thích nguyên nhân và hành động tiếp theo như xoá bộ lọc, tạo hồ sơ hoặc quay lại sau.
- **Lỗi:** thông báo cụ thể, `role="alert"` khi phù hợp và cách thử lại; giữ dữ liệu người dùng đã nhập.
- **Thành công:** xác nhận kết quả bằng thông tin thật từ server, điều hướng đến đối tượng vừa tạo khi phù hợp.
- **Busy/disabled:** nhãn “Đang xử lý…” và chặn gửi lặp; nêu lý do khi nghiệp vụ không cho thao tác.
- **Chưa rõ kết quả ghi:** kiểm tra kết quả trước khi gửi lại; giữ cơ chế đối soát hiện có.

Tìm kiếm/bộ lọc/phân trang của danh sách có thể chia sẻ phải phản ánh trên URL và khôi phục sau F5/Back/Forward. Danh sách chỉ lọc mảng đã tải phải diễn đạt đúng phạm vi kết quả.

## 7. Nội dung và khả năng tiếp cận

- Tiếng Việt thống nhất, câu ngắn; nhãn theo ngữ cảnh nghiệp vụ. Ngày giờ hiển thị theo `Asia/Ho_Chi_Minh`; tiền có đơn vị và chu kỳ nếu cần.
- Dữ liệu thiếu được bỏ trường hoặc giải thích phù hợp. Ảnh nghiệm thu dùng fixture phải gắn nhãn; dữ liệu mẫu nằm trong môi trường kiểm thử/demo được xác định rõ.
- Ảnh/biểu đồ có ý nghĩa có mô tả; SVG minh hoạ thuần trang trí dùng `aria-hidden="true"`. Dùng chữ cái đại diện công ty khi chưa có logo thật.
- Vùng bấm ≥44×44px; icon thao tác thường 20px, icon trong text 16px. Icon button có accessible name.
- Contrast: chữ thường ≥4.5:1, chữ lớn ≥3:1, control/focus cần nhận diện ≥3:1. Muted trên nền trắng đạt chuẩn; đo lại khi đổi nền hoặc opacity.
- Tab theo thứ tự đọc; có focus rõ; popup trả focus về trigger; menu đóng bằng Esc. Ưu tiên primitive đã hỗ trợ accessibility.
- Chuẩn hiện tại là **light theme**. Dark theme còn trong code mặc định; cần bộ token và kiểm chứng riêng trước khi coi là đã hỗ trợ đầy đủ.

## 8. Nơi hiện thực và mẫu tham khảo

Các đường dẫn tính từ gốc repo:

| Nơi | Trách nhiệm |
| --- | --- |
| `web/src/app/globals.css` | Theme runtime và mapping Tailwind; nơi tập trung tokens khi chuẩn hoá |
| `web/src/app/layout.tsx` | Font chung và ngôn ngữ trang |
| `web/src/app/components/ui/` | Component nền tảng dùng chung |
| `web/src/app/components/layout/app-shell.tsx` | Chọn layout theo vai trò |
| `web/src/app/components/layout/candidate-shell.tsx` | Top bar và điều hướng ứng viên |
| `web/src/app/components/layout/hr-shell.tsx` | Top bar và điều hướng HR |
| `web/src/app/components/layout/role-shells.tsx` | Khung Mentor (sidebar), Cán bộ (thanh trên + rail), Quản trị (sidebar nhóm + breadcrumb) |
| `web/src/app/components/layout/public-header.tsx` | Header các trang công khai |
| `web/src/app/components/job-board/` | Bảng việc làm, chi tiết tin, trang công ty (`job-board.css`) |
| `web/src/app/components/layout/hr-shell.css` | Mapping token chung vào HR và responsive trong giai đoạn chuyển đổi |
| `web/src/app/components/applications/career.css` | Theme ứng viên hiện có, cần hợp nhất dần với theme chung |
| `web/src/app/components/applications/candidate-journey.css` | Mapping token chung và responsive cho đơn ứng tuyển, phỏng vấn, offer ứng viên |
| `web/src/app/components/applications/career-banner.tsx` | Banner SVG theo phong cách được duyệt |
| `web/src/config/application-status.ts` | Nhãn và màu trạng thái ứng tuyển |
| `docs/design/candidate-banner/` | Ảnh tham khảo banner desktop/tablet/mobile |
| `docs/design/candidate-topbar/` | Ảnh tham khảo top bar và thông báo |
| `docs/design/candidate-journey/` | Ảnh Playwright đơn ứng tuyển, phỏng vấn và offer với fixture |

Các bản đặc tả riêng trong `docs/superpowers/specs/` giải thích từng tính năng. Quy định nền tảng dùng tài liệu này; khi một đặc tả cũ khác chuẩn chung, ghi rõ phần đang chuyển đổi trong PR.

## 9. Quy trình cho team và Codex

1. Đọc `DESIGN.md`, `web/AGENTS.md`, component/pattern liên quan và hợp đồng dữ liệu của màn.
2. Chọn pattern có sẵn; xác định dữ liệu, trạng thái và hành động chính trước khi thêm UI.
3. Dùng semantic tokens và component chung. Token/biến thể chưa có được bổ sung tại lớp chung trong phạm vi cần thiết và kiểm tra ảnh hưởng.
4. Chạy lint/type-check/build theo mức thay đổi; kiểm thử hành vi quan trọng. Thay đổi trình bày dùng render/screenshot để kiểm tra, tránh test chỉ lặp lại class CSS hoặc hằng số.
5. Chụp 1440×1000, 834×1112, 390×844; kiểm tra thêm tràn ngang ở 360px. Xem ảnh thực tế, thử bàn phím và trạng thái bất thường liên quan.
6. PR nêu điều đã đổi, lý do, kiểm chứng và giới hạn; thêm ảnh trước/sau khi thay đổi bố cục. Cập nhật tài liệu này nếu chuẩn chung thay đổi.

### Kiểm tra trước khi bàn giao UI

- [ ] Màu, chữ, spacing và component theo chuẩn; ngoại lệ có lý do.
- [ ] Hành động chính, active state và nhãn nghiệp vụ rõ ràng.
- [ ] Có loading/rỗng/lỗi/busy phù hợp; dữ liệu thiếu không bị thay bằng nội dung giả.
- [ ] Desktop/tablet/mobile và nội dung dài đã được kiểm tra.
- [ ] Bàn phím, focus, label và vùng bấm hoạt động đúng.
- [ ] Kiểm tra cả màn dùng chung component/token vừa sửa; báo đúng phạm vi đã kiểm chứng.

## 10. Lịch sử thay đổi

| Phiên bản | Ngày | Nội dung |
| --- | --- | --- |
| 1.0 | 24/11/2026 | Thống nhất phong cách đỏ trầm từ giao diện ứng viên làm nền tảng toàn team; ghi rõ hiện trạng và hướng chuyển đổi theme/component. |
| 1.1 | 25/11/2026 | Bổ sung tokens nền tảng chung, khung HR dùng top bar và quy tắc chuyển đổi theo vai trò. |
| 1.2 | 26/11/2026 | Bảng điều khiển HR, minh hoạ banner cho tin tuyển dụng/công ty/tổng quan/thực tập sinh, `data-primary` cho liên kết trong banner. |
| 1.3 | 26/11/2026 | Mỗi vai trò làm việc một bố cục điều hướng riêng trên nền sáng (Mentor sidebar, Cán bộ rail icon, Quản trị sidebar nhóm + breadcrumb); bỏ `WorkspaceShell`/`ManagerShell`. |
| 1.4 | 26/11/2026 | Làm lại trang đăng nhập (2 cột, `?tab=hr`, nút đỏ qua `.auth-theme`); sửa trạng thái tab đang chọn theo `aria-selected`. |
| 1.5 | 26/11/2026 | Gộp `test` (HS-04, UT-03): màn liên hệ UT-03 mở đầu bằng `TaskBanner`, dropdown dùng `SelectMenu`; mục "Mở liên hệ"/"Nhật ký liên hệ" vào rail cán bộ, "Lịch sử xem liên hệ" vào nhóm Hồ sơ của ứng viên. |
| 1.6 | 26/11/2026 | Hồ sơ `/candidate/profile` và CV `/candidate/cvs` mở đầu bằng `TaskBanner`; lớp `.candidate-brand` (qua `body:has`) đổi `--primary` sang đỏ cho Button mặc định và dialog của hai trang; thanh bậc kỹ năng theo thang đỏ; card Kinh nghiệm theo chuẩn card hồ sơ, rỗng có hướng dẫn và liên kết tìm việc. |
