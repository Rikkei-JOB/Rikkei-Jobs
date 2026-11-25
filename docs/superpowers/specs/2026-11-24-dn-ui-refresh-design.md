# Làm lại giao diện khu quản trị (HR, cán bộ) theo thương hiệu Rikkei — Thiết kế

Người phụ trách: người làm DN-01/02/05/06. Làm **trước DN-07**. PR vào **`test`**.

Ký hiệu: **(chốt)** người phụ trách đã quyết trong buổi brainstorm; **(người thiết kế chọn)** quyết định khi viết thiết kế, người làm có thể nêu lại.

## 1. Mục đích

Trên `test`, phía ứng viên vừa có giao diện mới (UT-01 #33, DN-04 #32): thanh trên trắng, đỏ Rikkei `#b4232d` làm điểm nhấn, nền `#f6f7fa`, CSS riêng (`candidate-shell.css`, `career.css`). Các trang HR và cán bộ vẫn ở khung cũ (`AppShell`: nút đen, phong cách shadcn trung tính, nội dung bó hẹp `max-w-5xl` ở giữa), nên hai phía trông như hai sản phẩm khác nhau và trang quản trị thừa nhiều khoảng trống.

Mục tiêu: trang HR và cán bộ dùng cùng thương hiệu và cùng kiểu **thanh menu ngang** với phía ứng viên (nhóm đã chốt dùng thanh ngang), bố cục gọn, ít khoảng trống, lấy cảm hứng từ cách các trang tuyển dụng (ITviec, TopCV) trình bày trang công ty, bảng tin nhà tuyển dụng và form đăng tin.

### Mục tiêu

1. Khung mới **thanh ngang** cho **HR và cán bộ** (chốt), màu đỏ Rikkei, giống thanh của ứng viên.
2. Bốn nhóm trang của người phụ trách làm lại theo 4 ý tưởng đã chọn (chốt; mockup ở `.superpowers/brainstorm/1466-1795516134/content/` và `.superpowers/brainstorm/415-1795569365/content/`).

### Ngoài phạm vi

- Phía ứng viên (`CandidateShell`, `career.css`, trang của UT-01/DN-04): không đụng.
- Luồng tài khoản HR: **giữ nguyên** như hiện tại (HR tạo trong công ty) (chốt; ý tưởng "cán bộ quản lý HR độc lập rồi gán vào công ty" đã bỏ).
- Tab "JD của công ty" ở trang chi tiết công ty của cán bộ: **dời sang DN-07** (chốt) vì cần API cho cán bộ xem JD theo công ty, là phần lõi của DN-07.
- **Mentor và admin**: không đảm nhận (chốt) — giữ nguyên khung menu trái và màu cũ.
- Các trang cán bộ không thuộc DN (Duyệt hồ sơ, Đơn ứng tuyển, Đồng bộ…) và trang HR không thuộc DN (Tuyển dụng, Lịch phỏng vấn, Thực tập sinh): chỉ nằm trong khung mới và đổi màu theo token, không sửa bố cục trang.
- File của UT-01 (`candidate-shell.tsx`, `candidate-shell.css`, `candidate-notifications.tsx`): không sửa (người thiết kế chọn) — một bạn trong nhóm đang làm tiếp phần thanh ngang, tránh sửa chồng.
- Chế độ tối: không làm (người thiết kế chọn); lớp token mới chỉ khai báo bản sáng.
- `/staff/match-preview` và `MatchScoreCard`: chỉ đổi màu theo khung.

### Không được (anti-goals)

- Không làm đổi giao diện trang của người khác ngoài việc đổi màu qua khung chung (không sửa token trong `:root` của `globals.css`).
- Không đổi hành vi nghiệp vụ hiện có (quy tắc lưu/gửi duyệt JD, khoá HR, tạo công ty…); chỉ đổi trình bày, trừ hai API bổ sung ở §6.
- Không thêm thư viện.

## 2. Cách làm đã chọn

| Câu hỏi | Các cách đã xét | Chọn |
| --- | --- | --- |
| Kiểu khung | menu trái kiểu B1 (trắng + đỏ); **thanh ngang như ứng viên** | **Thanh ngang** (chốt, nhóm đã thống nhất; ban đầu chọn B1 menu trái, đổi sau khi nhóm chốt) |
| Vai trò áp dụng | mọi vai trò không phải ứng viên; **chỉ HR và cán bộ** | **HR và cán bộ** (chốt): mentor, admin không thuộc người phụ trách |
| Cách áp màu | **A** lớp `admin-theme` khai báo lại token shadcn trong khung mới; B đổi token `:root` chung; C CSS riêng như phía ứng viên | **A** (chốt): B làm đổi nút trang đơn ứng tuyển UT-01 (đang dùng `bg-primary`) và trang mentor/admin; C tạo hai cách viết giao diện song song trong cùng trang HR |
| Code khung | sửa `candidate-shell` thành khung chung; **component riêng `ManagerShell`** | **Component riêng** (người thiết kế chọn): không sửa file UT-01 đang có người làm tiếp; chấp nhận lặp ~70 dòng, sau này nhóm có thể gộp |
| Icon | lucide-react; hugeicons (khai báo trong `components.json`) | **lucide-react** (người thiết kế chọn): `menu.ts` và thanh của ứng viên đã dùng, 25 file dùng lucide, chưa file nào dùng hugeicons |
| Đầu trang công ty | A ảnh bìa, logo đè mép; **B** không bìa, vạch đỏ 4px mép trên, logo + tên một hàng | **B** (chốt): chữ luôn trên nền trắng, tốn ít chiều cao |
| Ý tưởng trang | 1 hồ sơ công ty, 2 danh sách JD kiểu bảng tin, 3 form JD có xem trước, 4 quản lý doanh nghiệp | **Cả 4** (chốt), trừ tab JD của ý 4 (DN-07) |

## 3. Khung thanh ngang cho HR và cán bộ (`ManagerShell`)

Files: `web/src/app/components/layout/app-shell.tsx` (rẽ nhánh), mới `manager-shell.tsx` + `manager-shell.css` cùng thư mục, `web/src/config/menu.ts` (thêm nhóm menu), `web/src/app/globals.css` (khối `.admin-theme`).

- **`AppShell` rẽ ba nhánh**: `CANDIDATE` → `CandidateShell` (giữ nguyên); `HR`, `STAFF` → `ManagerShell` (mới); `MENTOR`, `ADMIN` → khung menu trái hiện tại, **không đổi gì** (cả màu).
- Phần tử gốc của `ManagerShell` có lớp **`admin-theme`**. Trong `globals.css` thêm khối `.admin-theme { … }` khai báo lại token shadcn (người thiết kế chọn giá trị, người làm chuyển sang `oklch` cho khớp file):
  - `--primary` = `#b4232d`, `--primary-foreground` = trắng, `--ring` = `#b4232d`;
  - `--background` (nền vùng nội dung) = `#f6f7fa`; thẻ/khung vẫn trắng (`--card`, `--popover` trắng);
  - chữ chính `#334155`, tiêu đề `#18233a`, viền `#e2e8f0` (khớp `candidate-shell.css`).
  - Không sửa `:root`.
- **Thanh ngang** — cùng kích thước và hành vi với thanh ứng viên (`candidate-shell.css`: cao 64px, nền trắng, viền dưới, dính đầu trang, nội dung thanh rộng tối đa 1440px):
  - trái: logo chữ "**Rikkei** Jobs" ("Rikkei" đỏ, "Jobs" xanh than, chấm đỏ), link về `ROLE_HOME`;
  - giữa: các mục menu; mục đang chọn chữ đỏ đậm + gạch đỏ 2px dưới; nhóm có ▾ mở menu thả xuống (`@base-ui/react/menu`), nhóm có mục đang chọn cũng đỏ;
  - phải: avatar tròn (2 chữ cái cuối tên, nền `#fff1f2`, chữ đỏ, viền `#fecdd3`) + họ tên + nhãn vai trò (`ROLE_LABEL`), bấm mở menu tài khoản: "Đổi mật khẩu" (chỉ HR, thay nút chìa khoá hiện tại) và "Đăng xuất";
  - **không có chuông thông báo** (người thiết kế chọn: thông báo hiện chỉ có cho ứng viên).
- **Nhóm menu** (khai báo trong `menu.ts`, ví dụ `ROLE_NAV_GROUPS`, dùng lại `MenuItem` và icon lucide có sẵn):
  - HR: 5 mục phẳng — Công ty · JD · Tuyển dụng · Lịch phỏng vấn · Thực tập sinh.
  - Cán bộ: **Duyệt ▾** (Duyệt hồ sơ, Duyệt JD) · Đơn ứng tuyển · Doanh nghiệp · Ghép thực tập · **Hệ thống ▾** (Đồng bộ dữ liệu, Báo cáo).
  - `ROLE_MENU` giữ nguyên (khung mentor/admin và route guard còn dùng).
- **Điện thoại (≤ 768px)**: thanh còn logo + avatar + nút ☰; bấm ☰ mở bảng menu theo nhóm (như `MobileNavigation` của ứng viên).
- **Vùng nội dung**: nền `#f6f7fa`, padding 32px 24px, rộng tối đa **1200px**, căn giữa (giống `candidate-main`); các trang DN **bỏ `max-w-5xl`/`max-w-3xl`** riêng của mình (nguồn chính của khoảng trống thừa), trừ form ngắn đặt hẳn trong thẻ.
- Tiêu đề trang thống nhất: `text-xl font-semibold text-[#18233a]` (người thiết kế chọn), nút hành động chính bên phải cùng hàng.

## 4. Ý 1 — Hồ sơ công ty (`/hr/company`, `/hr/company/preview`)

Component mới dùng chung **`CompanyHeader`** (`web/src/app/components/company/company-header.tsx`), cũng dùng cho trang chi tiết công ty của cán bộ (§7):

- Thẻ trắng, **vạch đỏ 4px mép trên**, logo 56px (ảnh `logoUrl`, không có thì ô đỏ chữ cái đầu), tên công ty (18px đậm), một dòng thông tin ngăn bằng "•": ngành • quy mô • thành phố • website (bỏ mục trống). Bên phải là vùng nút (prop `actions`).
- Hàng tab nằm trong thẻ, dưới phần đầu (prop `tabs`, tab đang chọn chữ đỏ + gạch đỏ dưới).

**`/hr/company`** (`company-profile-form.tsx`, `company-summary-card.tsx`, `page.tsx`):

- Đầu trang: `CompanyHeader`, nút "Xem như ứng viên". Mã số thuế và trạng thái hiển thị ở dòng phụ dưới thông tin nhanh ("MST 0101234567 · Đang hoạt động"), kèm chú thích "Liên hệ cán bộ để đổi" như hiện tại.
- Tab **Giới thiệu**: logo (giữ luồng xem trước → "Dùng ảnh này" / "Huỷ", ≤ 2MB JPG/PNG), tên, ngành, quy mô, giới thiệu.
- Tab **Thông tin liên hệ**: địa chỉ, thành phố, website.
- Tab **Việc làm đang tuyển (n)**: danh sách chỉ đọc JD `APPROVED` còn hạn (§6.1), mỗi dòng: tiêu đề, cấp độ, hạn nộp; bấm mở `/hr/jobs/[id]`. Không có JD: "Chưa có vị trí đang tuyển" + link "Tạo JD".
- **Một form chung** cho hai tab đầu (đổi tab không mất dữ liệu đang gõ); **thanh lưu dính đáy** vùng nội dung: trạng thái ("Đã lưu lúc hh:mm" / "Có thay đổi chưa lưu" / lỗi) + nút "Lưu thay đổi". Ẩn thanh lưu ở tab Việc làm. Quy tắc kiểm tra, thông báo lỗi giữ nguyên.
- `company-summary-card.tsx` không còn dùng: người làm xoá nếu không nơi nào khác dùng.

**`/hr/company/preview`** (`company-public-view.tsx`): `CompanyHeader` (nút "Quay lại chỉnh sửa", không có tab), banner "Bạn đang xem trang công ty như ứng viên thấy" giữ nguyên; các khối Giới thiệu, Thông tin liên hệ, **Việc làm đang tuyển (n)** dạng thẻ JD giống thẻ ở trang Việc làm của ứng viên (logo chữ cái, tên JD, cấp độ, hạn nộp) — không bấm được (người thiết kế chọn: trang chỉ để xem trước).

## 5. Ý 2 — Danh sách JD (`/hr/jobs`)

Files: `hr/jobs/page.tsx`, `components/job/job-table.tsx`, `job-status-badge.tsx`.

- **Hàng thẻ số liệu** thay cho dải tab trạng thái: 6 thẻ **Tất cả · Nháp · Chờ duyệt · Đang tuyển · Bị trả lại · Đã đóng** (người thiết kế chọn: giữ đủ 6 bộ lọc hiện có; mockup 4 thẻ thiếu Nháp/Đã đóng), mỗi thẻ số lớn + nhãn; thẻ đang lọc viền đỏ. Bấm thẻ = lọc như tab hiện tại (`?status=` trên URL như trước). Số lấy từ API tổng hợp §6.2. Thẻ "Đang tuyển" có thêm dòng nhỏ màu đỏ "N sắp hết hạn" khi N > 0.
- **Bảng**: Tiêu đề | Cấp độ | Hạn nộp | Trạng thái | Cập nhật | ⋯
  - Dòng phụ dưới tiêu đề: JD `REJECTED` → "Lý do trả lại: …" (đỏ, cắt 1 dòng, `title` đủ nội dung); còn lại → "N kỹ năng bắt buộc" (0 → "Chưa có kỹ năng bắt buộc").
  - Hạn nộp: ngày + " · còn N ngày" (N ≤ 7 → chữ đỏ; hôm nay → "hết hạn hôm nay"; đã qua → "đã quá hạn"; không có → "—"). Tính theo giờ Việt Nam như `JobRules.today`.
  - Menu ⋯ (dùng `@base-ui/react/menu`): DRAFT/REJECTED: "Sửa", "Gửi duyệt"; APPROVED: "Sửa", "Đóng JD"; PENDING/CLOSED: "Xem". "Gửi duyệt" và "Đóng JD" gọi đúng API hiện có, dùng lại `confirm-dialog.tsx` cho "Đóng JD"; gửi duyệt lỗi `VALIDATION_ERROR` → thông báo "JD chưa đủ điều kiện gửi duyệt" kèm link "Mở để sửa". Bấm cả dòng vẫn mở JD như hiện tại.
- Công ty bị khoá: banner và ẩn "+ Tạo JD" như hiện tại; menu ⋯ chỉ còn "Xem".

## 6. API bổ sung (company-service)

Hai thay đổi, đều chỉ **thêm**, không đổi trường cũ (người thiết kế chọn):

### 6.1 Ý 1 dùng API có sẵn

`GET /companies/me/jobs?status=APPROVED&size=100` rồi lọc bỏ JD có `deadline` < hôm nay (giờ VN) ở giao diện. Không đổi backend (người thiết kế chọn: không dùng API JD công khai vì API đó không lọc theo công ty). Công ty đang `LOCKED` thì tab và trang xem trước hiện thêm dòng "Công ty đang bị khoá, ứng viên hiện không thấy việc làm" để khớp những gì ứng viên thực sự thấy.

### 6.2 `GET /companies/me/jobs/summary` (mới, `MyCompanyJobController`)

- `@PreAuthorize(Roles.HR)`, công ty lấy như `listMine` (company_hr theo userId; công ty LOCKED vẫn đọc được; HR chưa liên kết → 404).
- Response: `{ "total": 11, "byStatus": { "DRAFT": 3, "PENDING": 5, "APPROVED": 2, "REJECTED": 1, "CLOSED": 0 }, "expiringSoon": 1 }` — `byStatus` luôn đủ 5 khoá; `expiringSoon` = JD `APPROVED` có `deadline` từ hôm nay đến hôm nay + 7 ngày (giờ VN, `JobRules.today(clock)`).
- Một truy vấn đếm theo nhóm trạng thái + một truy vấn đếm sắp hết hạn; springdoc như các endpoint khác.

### 6.3 Mở rộng `JobSummary` (danh sách JD của HR)

- Thêm `requiredSkillCount` (int) và `rejectReason` (String, null trừ JD `REJECTED`).
- Đếm kỹ năng bắt buộc **một truy vấn gom nhóm cho cả trang** (không N+1).

### 6.4 Mở rộng `StaffCompanyItem` (danh sách công ty của cán bộ)

- Thêm `logoUrl`, `city`, `size` (đều có thể null), lấy từ bảng `companies` (không truy vấn thêm).

`docs/api/company-openapi.yaml` cập nhật 6.2–6.4.

**Trùng file với DN-07.** Nhánh `feature/DN-07-job-review` (đang làm, worktree `.worktrees/DN-07-job-review`) cũng sửa `MyCompanyJobController` (thêm `GET /companies/me/jobs/{id}`, chuyển từ `GET /jobs/{id}`), `JobRepository`, `JobMapper`, `CompanySecurityConfig`, `web/src/services/job.service.ts`. Đợt này tạo nhánh từ `origin/test` sạch, chỉ **thêm** method mới (không sửa method DN-07 đổi) để PR nào merge sau chỉ phải gộp tay vài chỗ. Giao diện gọi chi tiết JD qua `jobService` hiện có, không tự đổi đường dẫn.

## 7. Ý 3 — Form JD có khung xem trước (`/hr/jobs/new`, `/hr/jobs/[id]`)

Files: `components/job/job-form.tsx` (426 dòng — người làm được tách nhỏ nếu cần), `job-general-step.tsx`, `job-skills-step.tsx`, component mới `job-preview-card.tsx`.

- Từ **≥ 1280px**: hai cột — form trái (~60%), **thẻ xem trước** phải (~40%), dính khi cuộn (`sticky`, cách thanh trên). Dưới 1280px: thẻ xem trước nằm trong khối thu gọn "Xem trước như ứng viên" ngay trên thanh nút, mặc định đóng (người thiết kế chọn).
- **`JobPreviewCard`**: nhãn nhỏ "XEM TRƯỚC NHƯ ỨNG VIÊN", logo + tên công ty, tiêu đề, dòng "lương · thành phố · hình thức · hạn nộp" (thiếu thì bỏ mục), các chip kỹ năng (bắt buộc trước, tên lấy từ danh mục kỹ năng `use-skill-catalog`), đoạn đầu mô tả (cắt 4 dòng). Cập nhật theo dữ liệu đang gõ (`watch` của react-hook-form), không gọi API. Phong cách giống thẻ JD phía ứng viên (người làm đối chiếu `career.css`), viết bằng Tailwind.
- **Thanh nút dính đáy** vùng nội dung: trái là trạng thái ("Đã lưu nháp lúc hh:mm" / "Có thay đổi chưa lưu"), phải là các nút hiện có theo trạng thái JD (Lưu nháp, Gửi duyệt / Lưu và gửi duyệt lại kèm hộp xác nhận / Đóng JD). Hai nút bước "1. Thông tin chung" / "2. Kỹ năng yêu cầu" giữ ở đầu form, kèm dấu lỗi như hiện tại.
- Chế độ chỉ xem (PENDING/CLOSED), banner lý do trả lại, quy tắc kiểm tra và luồng lưu: giữ nguyên.

## 8. Ý 4 — Quản lý doanh nghiệp (`/staff/companies`, `/staff/companies/[id]`)

Files: `staff/companies/page.tsx`, `[id]/page.tsx`, `components/staff-company/company-table.tsx`, `hr-account-table.tsx`.

- **Danh sách**: tiêu đề + "+ Tạo công ty" một hàng; ô tìm + bộ lọc trạng thái + "N công ty" một hàng. Bảng: Công ty (logo 24px hoặc ô chữ cái màu suy ra từ tên, tên đậm, dòng phụ "thành phố · quy mô") | Mã số thuế | HR | JD | Trạng thái; bấm cả dòng mở chi tiết; phân trang giữ nguyên.
- **Chi tiết**: `CompanyHeader` (§4) chỉ đọc, dòng phụ "MST … · trạng thái", vùng nút: "+ Thêm HR". Bên dưới là khối "Tài khoản HR (n)" (bảng hiện tại, làm gọn theo khung mới; khoá/mở khoá/thông báo giữ nguyên). **Chưa có tab** — DN-07 thêm tab "JD của công ty" (chốt).
- Hộp thoại tạo công ty, thêm HR, khoá/mở khoá: chỉ đổi màu theo token, không đổi hành vi.

## 9. Kiểm thử

| Phần | Cách |
| --- | --- |
| `GET /companies/me/jobs/summary`: đếm đúng từng trạng thái, `expiringSoon` biên (hôm nay, +7 ngày tính, +8 không tính, đã quá hạn không tính, không phải APPROVED không tính), công ty khác không lẫn, HR chưa liên kết 404, vai trò khác 403 | company-service, MockMvc (H2), `Clock` cố định |
| `JobSummary.requiredSkillCount`, `rejectReason` đúng; trang nhiều JD không N+1 (một truy vấn đếm) | company-service |
| `StaffCompanyItem` có `logoUrl`, `city`, `size` | company-service |
| Giao diện | `npm run type-check`, `npm run lint`, `npm run build`; kiểm tay từng trang ở 1440px và 375px (thanh ngang, menu thả xuống, bảng ☰, menu tài khoản); trang mentor/admin và trang ứng viên **không đổi** |

## 10. Tài liệu bàn giao

- `docs/api/DN-02-jobs.md`, `docs/api/DN-05-company-hr-ui.md`: cập nhật mục giao diện và API mới; ảnh minh chứng cũ đánh dấu là giao diện trước khi làm lại, người phụ trách chụp lại sau khi chạy thật.
- `docs/api/company-openapi.yaml`: §6.2–6.4.
- PR ghi rõ: `AppShell` (khung chung của NT-02) rẽ nhánh cho HR và cán bộ sang thanh ngang mới; mentor, admin, ứng viên không đổi.

## 11. Để người làm tự quyết

- Giá trị `oklch` chính xác của token, cường độ bóng/viền, kích thước chữ ngoài các mốc đã ghi.
- Cách tách `job-form.tsx`, tên component phụ.
- Màu ô chữ cái cho công ty không có logo (bảng màu cố định, suy ra từ tên).
