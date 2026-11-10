# HS-01: phần backend độc lập trước NT-01

Phạm vi đợt này: xây dựng và kiểm thử lõi dữ liệu Skill Profile trong `profile-service`. Chưa có endpoint HTTP, đăng nhập hay giao diện HS-01; những điểm chưa thống nhất được để lại cho bước tích hợp NT-01. Không lấy code NT-01 vào nhánh này.

## Đã triển khai

- Entity/repository cho `skills`, `skill_mappings`, `skill_evidences`, `skill_scores`.
- SQL tạo bảng và seed 20 mã kỹ năng theo `docs/conventions.md`; 15 mapping mẫu nằm trong DB.
- `SkillProfileService.importVerifiedRecords(userId, records)`: ánh xạ kết quả nội bộ đã xác thực thành bằng chứng rồi tính lại điểm. Nguồn `PM` được lưu thành bằng chứng `INTERNSHIP`.
- `recomputeSkills(userId)`: tính lại điểm đã xác thực từ bằng chứng đã lưu; giữ riêng `self_score`; xóa điểm cũ nếu không còn bằng chứng và điểm tự khai.
- `getSkills(userId)`: trả DTO kỹ năng, hai loại điểm, bậc và danh sách bằng chứng; sắp xếp theo nhóm/mã kỹ năng và ngày bằng chứng.
- Bộ dữ liệu kiểm thử cho ba ứng viên và các kiểm thử tính điểm, lưu trữ, cập nhật, cách ly ứng viên, dữ liệu sai và seed lặp lại.

`user_id` là UUID tài khoản nội bộ. `lms-sv-001` là ID hệ thống ngoài, không dùng thay UUID. UUID trong fixture chỉ dành cho kiểm thử, không giả định trùng tài khoản NT-01.

## Hợp đồng nội bộ để ghép tiếp

```java
List<SkillProfileView> importVerifiedRecords(UUID userId, List<VerifiedSkillRecord> records);
List<SkillProfileView> recomputeSkills(UUID userId);
List<SkillProfileView> getSkills(UUID userId);
```

`VerifiedSkillRecord(source, sourceCode, sourceRef, title, score, recordedAt)` chỉ nhận các nguồn `LMS`, `EXAM`, `PM`. Đây là đầu vào **nội bộ đã được kiểm chứng**, không phải request body công khai. Adapter NT-05 phải kiểm tra điểm đã chốt/chính thức hoặc task đã nghiệm thu trước khi gọi; chuẩn hóa thang 10 về 100. `attendanceRate`, `homeworkRate` và `rpoint` không tự được coi là điểm môn học.

- `sourceCode` tra `skill_mappings`, ví dụ `JAVA01`. Kết quả chưa có mapping được bỏ qua, không tự sinh kỹ năng mới.
- `sourceRef` định danh ổn định cho từng kết quả/lần thi/task. Khóa bằng chứng là `(user_id, skill_id, source, source_ref)`.
- Import là cập nhật từng phần: kết quả cùng khóa cập nhật thay vì nhân đôi; kết quả vắng trong batch vẫn được giữ. Thu hồi kết quả/đổi danh sách mapping cần chính sách đồng bộ riêng ở bước tích hợp.
- `recordedAt` là ngày gốc, bắt buộc có timezone và độ chính xác tối đa microsecond; không thay bằng ngày import. Thời gian có phần nhỏ hơn microsecond bị từ chối để DB không làm tròn sang phía khác của mốc một năm. Fixture LMS hiện chưa có ngày nên dữ liệu test dùng ngày mẫu được ghi rõ.
- Điểm nhận vào thuộc `0–100`, tối đa 12 chữ số thập phân để không bị DB làm tròn âm thầm. Trọng số được lưu cùng bằng chứng để các lần recompute giữ nguyên đầu vào; import lại một kết quả áp dụng mapping hiện hành.
- Không sửa dữ liệu SELF/MENTOR trong luồng import. Chưa quyết định công thức nhiều bằng chứng SELF; recompute giữ nguyên `self_score` do luồng tự đánh giá cung cấp, không tự suy ra hay cộng vào điểm xác thực.
- Các method trên không kiểm tra quyền truy cập. Controller tương lai phải kiểm tra JWT, chủ sở hữu và trạng thái công khai/duyệt trước khi gọi.

DTO đầu ra dùng cấu trúc của task; route cụ thể để chốt cùng gateway NT-01:

```json
[
  {
    "skill": {"id":"00000000-0000-0000-0001-000000000001","code":"JAVA","name":"Java","group":"Backend"},
    "verifiedScore":82.058824,
    "selfScore":null,
    "level":"GOOD",
    "evidences":[
      {"source":"LMS","title":"Lập trình Java (mẫu đã chốt)","score":85,"recordedAt":"2026-10-01T08:00:00Z","verified":true}
    ]
  }
]
```

Ví dụ JSON minh họa cấu trúc; danh sách bằng chứng rút gọn. Điểm 82.058824 trong fixture được tính từ cả LMS và EXAM như phần dưới.

## Công thức và kiểm tra tính tay

```text
hệ số thời gian = 1 nếu ngày bằng chứng >= thời điểm tính trừ 1 năm lịch (UTC)
                 0.5 nếu cũ hơn
verified_score = Σ(score × weight × hệ số) / Σ(weight × hệ số)
```

Mốc đúng một năm vẫn dùng hệ số 1. Chỉ lấy bằng chứng `verified=true`; SELF luôn bị loại khỏi phép tính xác thực. Không có bằng chứng xác thực thì `verified_score=null`, `level=null`.

- 80 điểm mới, trọng số 0.7; 60 điểm cũ, trọng số 0.3: `(80×0.7 + 60×0.3×0.5) / (0.7 + 0.3×0.5) = 76.470588`, bậc `GOOD`.
- Java của candidate1: LMS 85, trọng số 0.7; EXAM 80, trọng số 1; đều còn mới tại ngày kiểm thử 2026-11-11: `(85×0.7 + 80×1) / (0.7+1) = 82.058824`, bậc `GOOD`.
- Thêm SQL01 60 điểm vào người đã có JAVA01 80 điểm: SQL đổi từ 80 thành `(80×0.3 + 60×1) / 1.3 = 64.615385`.

Bậc được tính trước khi làm tròn: `<40 BASIC`, `<70 FAIR`, `<85 GOOD`, còn lại `EXCELLENT`. Điểm trả ra/lưu tổng hợp có sáu chữ số thập phân; do đó điểm hiển thị đã làm tròn có thể chạm ngưỡng trong khi bậc vẫn phản ánh giá trị chính xác chưa làm tròn.

## Chạy kiểm thử

Từ gốc repo, Java 21 và Maven:

```sh
mvn -f server/pom.xml -pl services/profile-service -am clean test
```

Máy hiện tại có cache Maven truy cập được tại `C:/Users/Admin/.m2/repository`; có thể chạy offline:

```powershell
mvn -f server/pom.xml '-Dmaven.repo.local=C:/Users/Admin/.m2/repository' -o -pl services/profile-service -am clean test
```

H2 chỉ nằm ở dependency `test`. Kiểm thử tự tạo database trong bộ nhớ ở chế độ PostgreSQL, chạy SQL thật và JPA `ddl-auto=validate`; không đọc/ghi PostgreSQL của nhóm, không cần mock server/NT-01. Đồng hồ kiểm thử cố định nên không lệ thuộc ngày chạy.

Fixture: `server/services/profile-service/src/test/resources/hs01/candidates.json`. Đổi/thêm record rồi chạy lại test để thử recompute; đây là kết quả đã chuẩn hóa kèm ngày giả lập, không phải schema `external_records` production hay dữ liệu trả trực tiếp từ mock hiện tại. Candidate1 có 8 kỹ năng; candidate2 có 7; candidate3 có 4. SELF-only được kiểm tra bằng fixture riêng trong integration test.

## SQL và chạy service

`src/main/resources/db/hs01/schema.sql` và `catalog.sql` là script chuẩn bị, **không nằm trong `db/migration` và không tự chạy**. Seed có thể chạy lại mà không tạo trùng; nhãn nhóm và các mapping là dữ liệu mẫu để chỉnh trong DB. Script không tạo users hay external_records, không thay đổi dữ liệu tài khoản.

Hiện `ddl-auto=validate` yêu cầu bốn bảng HS-01 tồn tại trước khi chạy service. Với một database phát triển riêng, có thể chạy `schema.sql` rồi `catalog.sql` bằng SQL client, đặt `PROFILE_DB_URL/USER/PASSWORD` cho database đó và khởi chạy theo README. Không chạy script vào database dùng chung trước khi nhóm thống nhất migration.

## Các việc chờ NT-01 và hợp đồng nhóm

- Dùng `common-core`, `AuthPrincipal`, JWT/role và `MUST_CHANGE_PASSWORD` từ NT-01; lấy userId thật cho `/me`.
- Chốt `/profiles/**` trong task hay `/profile/**` trong nhánh NT-01, gateway và OpenAPI rồi mới thêm controller.
- Chốt nguồn trạng thái hồ sơ được đồng ý công khai/đã duyệt; người khác đọc hồ sơ riêng phải nhận 404. Chưa mở endpoint đọc hồ sơ.
- Chốt lịch sử Flyway/schema/khóa ngoại tới users và chuyển SQL chuẩn bị sang migration được nhóm duyệt.
- NT-05 cung cấp schema external_records, sourceRef, ngày gốc, trạng thái xác thực và adapter đồng bộ; chính sách thu hồi/cập nhật kết quả.
- Chốt cách cập nhật/tổng hợp nhiều bằng chứng SELF; công thức trọng số nguồn DG-03 không thuộc đợt này.
- Khi có caller HTTP/job đồng thời, chốt cơ chế tuần tự hóa/retry cho import và recompute cùng một ứng viên trước khi mở luồng đó.
- FE nối API client/phiên đăng nhập chung, sau đó nghiệm thu xuyên suốt BE–FE và mở PR `[HS-01]` vào `test`.

## Bằng chứng thực hiện

- Đã chạy RED trước implementation: clean test báo thiếu các lớp lõi HS-01; calculator test cũng báo thiếu API trước khi có production code.
- Đợt kiểm thử đầu có 46 ca thành công. Review độc lập phát hiện sai lệch độ chính xác timestamp và thiếu ca rollback sau khi bắt đầu ghi. Đã sửa kiểm tra timestamp sau khi thấy test RED, bổ sung kiểm tra mốc microsecond sau đọc lại và rollback khi phép tính lỗi; bộ cuối có **49 ca thành công: 31 calculator, 13 JPA/service, 5 validation**, không lỗi/không bỏ qua.
- Đã chạy `mvn -f server/pom.xml '-Dmaven.repo.local=C:/Users/Admin/.m2/repository' -o clean verify`: **11/11 module SUCCESS, BUILD SUCCESS**. Báo cáo test nằm trong `server/services/profile-service/target/surefire-reports/`.
- Chưa thực thi SQL trên PostgreSQL thật: Docker daemon trên máy không hoạt động. Các kiểm thử DB hiện dùng H2 PostgreSQL-mode; cần kiểm tra PostgreSQL và luồng xác thực/quyền khi ghép NT-01.
- Không commit/push, không thay NT-01, frontend, gateway, `mock/`, `.env` hoặc database đang có.
