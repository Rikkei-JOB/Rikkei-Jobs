# HS-02 — Giai đoạn 3 và bàn giao

Hoàn thiện hồ sơ ứng viên gồm [thông tin/LMS](HS-02-personal-profile.md),
[học vấn/chứng chỉ/avatar/upload](HS-02-records-and-files.md) và kỹ năng tự đánh giá.
Trang `/candidate/profile` giữ khối Skill Profile HS-01 để phân biệt điểm tự khai
với điểm được xác minh. Không bổ sung cơ chế công khai hoặc duyệt hồ sơ trong task này.

## Kỹ năng tự khai

`GET /api/v1/skills` yêu cầu đăng nhập, trả mảng `{id,code,name,group}` từ danh mục
database, sắp theo nhóm/mã. Frontend không giữ bản sao danh mục cố định.

`PUT /api/v1/profiles/me/self-skills` yêu cầu CANDIDATE, body là mảng JSON:

```json
[
  {"skillId": "00000000-0000-0000-0001-000000000001", "selfScore": 75.5}
]
```

Lấy skillId thực tế từ GET /skills; ID trên chỉ minh hoạ cấu trúc request. Response
là toàn bộ SkillProfileView[] có cùng cấu trúc với `GET /profiles/me/skills` của
HS-01, không chỉ các kỹ năng vừa tự khai.

- PUT thay thế **toàn bộ danh sách tự khai** của người đăng nhập. Bỏ một skill khỏi
  mảng sẽ xoá phần tự khai của skill đó. Gửi `[]` để bỏ tất cả phần tự khai.
- skillId phải tồn tại, không được lặp; selfScore bắt buộc, là số 0–100, tối đa 6
  chữ số thập phân. Null, phần tử null, ID sai/trùng/không tồn tại hoặc điểm sai
  trả 400; không lưu một phần danh sách. Sai vai trò 403, thiếu/sai JWT 401.
- Chủ sở hữu luôn từ JWT. userId/source/verified/verifiedScore client gửi thêm
  không được dùng. Query userId không thể chọn hồ sơ người khác.
- Lưu một bằng chứng SELF cho mỗi skill, verified=false, weight=1, sourceRef=self,
  không gắn externalRecordId; đồng bộ `skill_scores.self_score` trong cùng transaction.
- Không thay bằng chứng LMS/EXAM/INTERNSHIP/MENTOR, verifiedScore hay level. Skill
  vừa có điểm xác minh vừa có điểm tự khai hiển thị hai phần riêng; điểm chính và
  xếp loại xác minh vẫn giữ nguyên. SELF không được tính vào verifiedScore.
- Lưu/rút lại SELF dùng chung khoá user với import/recompute HS-01 để tránh lệch
  giữa bằng chứng và bảng điểm khi có request đồng thời. Recompute giữ selfScore.
- Không cần migration mới: dùng các bảng HS-01 đã có. Seed demo SELF chỉ chạy
  khi user chưa có profile state, không phục hồi kỹ năng người dùng đã xoá khi restart.

Giao diện có thêm/sửa/bỏ hàng, báo điểm sai, chặn chọn trùng skill, giữ dữ liệu
khi lưu lỗi và cập nhật Skill Profile ngay sau khi lưu thành công. Trang xem hồ
sơ theo userId vẫn chỉ đọc, không có form sửa kỹ năng.

## Chạy và tự nghiệm thu

Dùng cấu hình/cách chạy của giai đoạn 1–2. Bật seed identity dev và mock LMS; đăng
nhập tab Qua LMS, chọn candidate1. Dữ liệu ứng viên trong mock là dữ liệu mẫu.
Không cần bật `PROFILE_DEMO_DATA_ENABLED` để tự thêm kỹ năng; danh mục skills được
Flyway seed. Nếu muốn xem điểm HS-01 mẫu thì bật biến này, demo chỉ dùng local.

```powershell
docker compose up -d --build profile-service api-gateway
# Frontend tại web/:
npm run dev
```

Mở http://localhost:3000/candidate/profile. Các bước nghiệm thu thủ công:

1. Kiểm tra hệ/lớp/khoá lấy từ LMS và không sửa được; lưu thông tin cá nhân, F5.
2. Thêm/sửa/xoá học vấn và chứng chỉ; kiểm tra ngày kết thúc/hết hạn không trước
   ngày bắt đầu/cấp. Upload PDF 4MB được, 6MB và EXE đổi đuôi PDF bị từ chối.
3. Thay/bỏ avatar JPG/PNG; PDF không được gắn làm avatar.
4. Chọn kỹ năng, nhập điểm 0–100 và lưu; thấy nhãn Tự khai ở Skill Profile. Đổi
   điểm/bỏ kỹ năng, lưu và F5; điểm xác minh và bằng chứng khác không bị mất.
5. Dùng ứng viên khác để kiểm tra không thấy học vấn/chứng chỉ riêng tư, không
   tải được file của chủ cũ. Không thử sửa trực tiếp dữ liệu của người dùng thật.

File bỏ đính kèm không tự xoá vật lý; backup database cùng volume profile_files.

## Kiểm chứng tự động

```powershell
mvn -f server/pom.xml test
npm --prefix mock test
npm --prefix web run type-check
npm --prefix web run lint
npm --prefix web run build
# Trong web/ trên Windows có Edge:
$env:UT01_BROWSER_CHANNEL='msedge'
npm run test:e2e -- --workers=2
```

Recruitment tests mặc định cần Docker/Testcontainers. Có thể dùng database kiểm
thử riêng qua `UT01_TEST_DB_URL/USER/PASSWORD` như docs UT-01; không trỏ vào DB
ứng dụng vì các test recruitment xoá bảng giữa các case. Các nhóm test profile hỗ
trợ HS01_TEST_DB_*, HS02_TEST_DB_*, HS02_PHASE2_TEST_DB_* và
HS02_PHASE3_TEST_DB_* để chạy trên PostgreSQL riêng. Đặt HS01_TEST_DB_URL thành
PostgreSQL cũng bật bài test read consistency thường bị skip trên H2.

Browser tests dùng API doubles, backend tests dùng DB/service thật trong phạm vi
test với nguồn LMS giả lập. Không coi đây là nghiệm thu luồng LMS thật hay mentor
duyệt PR.

### Kết quả xác minh 2026-11-14

- `mvn -o -f server/pom.xml test`: 12 module SUCCESS, **519 tests, 0 failures,
  0 errors, 0 skipped**. Recruitment dùng database riêng
  `hs02_ut01_verify_20261114`; các nhóm HS-01/HS-02 hỗ trợ PostgreSQL dùng
  `hs02_full_verify_20261114` (PostgreSQL 18). Không dùng DB ứng dụng để test.
- 8 bài SelfSkillsApiTest qua, gồm validation nguyên tử, quyền sở hữu, giữ điểm
  xác minh, xoá SELF cũ, thay danh sách đồng thời và lưu cùng lúc recompute.
  Test snapshot HS-01 cũng được chạy, không skip như khi dùng H2 mặc định.
- Test demo seed xác minh xoá kỹ năng SELF rồi chạy seed lại không phục hồi nó.
- Mock 18/18 qua. Playwright Edge **23/23 qua**, gồm 6 bài giai đoạn 3 và 17 bài
  hồi quy. Có regression RED→GREEN cho kỹ năng vừa có điểm xác minh vừa tự khai.
- Type-check và production build qua; lint 0 lỗi, 1 warning có sẵn ở
  `web/commitlint.config.mjs`. Desktop 1440px/mobile 390px đã xem ảnh, không tràn ngang.
- Review độc lập không phát hiện lỗi chặn; chưa thực hiện thao tác hồ sơ ứng viên
  thực tế, chưa nghiệm thu LMS thật hay có thành viên nhóm/mentor duyệt PR.
- Đã cập nhật profile-service/gateway đang chạy và xác nhận healthy; frontend
  cổng 3000 đã trả 200. Không sửa dữ liệu hồ sơ thực tế để tự nghiệm thu. Giai đoạn
  3 không thêm migration mới và giữ nguyên volume của giai đoạn 1–2.

## PR đề xuất

Tiêu đề: `[HS-02] Hoàn thiện hồ sơ ứng viên và kỹ năng tự khai`.
Base branch theo quy ước repo: `test` (file task gốc ghi develop là thông tin cũ).
Mô tả: hồ sơ lấy sẵn từ LMS, CRUD học vấn/chứng chỉ, upload riêng tư và kỹ năng
tự khai; dẫn các hướng dẫn giai đoạn 1–3, kết quả test và ảnh màn hình. Ảnh QA dùng
dữ liệu API doubles, không phải bằng chứng luồng live; bản để review nằm trong
`docs/api/screenshots/HS-02/`.
Thành viên nhóm review và mentor duyệt là bước thủ công, chưa được thực hiện bởi
agent. Chưa commit/push hoặc tạo PR thay người dùng.

Ảnh [desktop](screenshots/HS-02/profile-desktop.png) và
[mobile](screenshots/HS-02/profile-mobile.png).
