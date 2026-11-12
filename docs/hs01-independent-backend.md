# HS-01: backend Skill Profile đã tích hợp NT-01

Phạm vi tài liệu này là backend HS-01: dữ liệu, công thức tính điểm, xác thực JWT, quyền đọc hồ sơ, API, migration và dữ liệu demo. Frontend chưa thuộc đợt này; hoàn tất backend không đồng nghĩa đã nghiệm thu toàn bộ HS-01 có giao diện.

## API và quyền truy cập

Gateway dùng tiền tố `http://localhost:8080/api/v1`; service nhận `/profiles/...` sau khi gateway bỏ `/api/v1`. Route `/api/v1/profile/ping` cũ vẫn hoạt động.

| Method | Đường dẫn sau tiền tố gateway | Quyền / hành vi |
| --- | --- | --- |
| GET | `/profiles/me/skills` | `CANDIDATE`; lấy UUID từ JWT |
| GET | `/profiles/{userId}/skills` | Có JWT; chủ sở hữu đọc được; người khác cần cả `consented=true` và `approved=true` |
| POST | `/profiles/me/skills/recompute` | `CANDIDATE`; không có request body; tính lại từ dữ liệu nguồn đã lưu của chính người gọi |

Thành công trả thẳng mảng JSON, không bọc `data` hoặc phân trang. Mỗi phần tử có `skill {id, code, name, group}`, `verifiedScore`, `selfScore`, `level` và `evidences [{source, title, score, recordedAt, verified}]`. Danh sách sắp theo nhóm/mã kỹ năng; bằng chứng theo ngày giảm dần.

- Gửi `Authorization: Bearer <JWT>` do identity-service cấp. Các service phải cùng `JWT_SECRET`.
- `/me` chỉ dùng UUID trong claim `sub`; `userId` do client tự gửi không thay chủ sở hữu. `lms-sv-001` là định danh LMS, không phải UUID tài khoản.
- Chủ sở hữu chưa có dữ liệu nhận `200 []`. Người khác đọc hồ sơ riêng hoặc UUID không tồn tại nhận cùng `404 NOT_FOUND`, kể cả khi role là STAFF/ADMIN.
- Thiếu/sai/hết hạn JWT: `401 UNAUTHORIZED`; sai role trên `/me`: `403 FORBIDDEN`; token cần đổi mật khẩu: `403 MUST_CHANGE_PASSWORD`; UUID sai định dạng: `400 BAD_REQUEST`.
- HS-01 không mở endpoint nhận bằng chứng đã xác thực từ client, đồng ý công khai hoặc duyệt hồ sơ. Trạng thái công khai được lưu để kiểm tra quyền; workflow cập nhật thuộc phần chức năng tiếp theo.

Đặc tả máy đọc được: [hs01-openapi.yaml](api/hs01-openapi.yaml). Bộ kiểm tra xuyên gateway: [Postman collection](../postman/hs01.postman_collection.json).

## Dữ liệu và migration

Profile sử dụng schema PostgreSQL `profile`, JPA `ddl-auto=validate`, Flyway tự tạo/nâng cấp schema khi service khởi động. Lịch sử nằm ở `profile.flyway_schema_history`, độc lập với bảng `public.users` và lịch sử migration của identity-service.

| Migration | Nội dung |
| --- | --- |
| `V1__skill_profile.sql` | `skills`, `skill_mappings`, `skill_evidences`, `skill_scores` |
| `V2__skill_catalog.sql` | 20 kỹ năng và 15 mapping mẫu |
| `V3__external_records_and_profile_state.sql` | `external_records`, `skill_profile_states`, liên kết bằng chứng với bản ghi nguồn |

Nguồn chính thức nằm trong `server/services/profile-service/src/main/resources/db/migration/`. Không cần chạy thủ công các script chuẩn bị cũ trong `db/hs01/` để khởi động ứng dụng.

Nếu trước đây đã tự chạy SQL nháp và lưu dữ liệu vào các bảng `public.skills`, `public.skill_evidences` hoặc `public.skill_scores`, đợt này không xóa hay tự chuyển dữ liệu đó sang schema mới. Cần kiểm tra và lập migration chuyển dữ liệu trước khi triển khai trên database đã dùng bản nháp; database mới chạy các migration bên trên bình thường.

`profile.skill_profile_states` có `user_id` UUID làm khóa chính, `consented` và `approved` mặc định đều `false`. Import/recompute tạo trạng thái riêng tư cho ứng viên chưa có trạng thái. Các lần ghi cùng ứng viên được tuần tự hóa bằng khóa hàng trạng thái trong transaction.

`profile.external_records` là hợp đồng lưu trữ để adapter nội bộ cung cấp kết quả:

| Cột | Quy tắc |
| --- | --- |
| `id` | UUID khóa chính |
| `user_id` | UUID nội bộ từ identity |
| `source` | `LMS`, `EXAM` hoặc `PM` |
| `source_code` | Tra `skill_mappings`, ví dụ `JAVA01` |
| `source_ref` | ID ổn định của kết quả/lần thi/task; duy nhất theo `(user_id, source, source_ref)` |
| `title` | Tên bằng chứng không rỗng |
| `score`, `score_max` | `0 <= score <= score_max`, `score_max > 0`; hỗ trợ thang 10 hoặc 100 |
| `recorded_at` | Ngày gốc có múi giờ, độ chính xác tối đa microsecond |
| `verified` | Mặc định `false`; chỉ bản ghi `true` tham gia đồng bộ bằng chứng xác thực |

Recompute đọc nguồn trong database, chuẩn hóa `score / score_max × 100`, áp dụng mapping hiện hành và lưu bằng chứng cùng trọng số. Nguồn `PM` thành bằng chứng `INTERNSHIP`. Sửa điểm nguồn được cập nhật; nguồn bị thu hồi xác thực, xóa hoặc mất mapping thì bằng chứng có `external_record_id` tương ứng bị loại. Điểm tổng hợp được tính lại sau đó.

Bằng chứng SELF/MENTOR và import nội bộ cũ không có `external_record_id` được giữ. `self_score` được giữ riêng; recompute không suy ra công thức tự đánh giá và không cộng điểm tự khai vào điểm xác thực. Hàm `importVerifiedRecords` tiếp tục là API Java nội bộ nhận đầu vào đã kiểm chứng, không phải endpoint HTTP.

Adapter NT-05 gọi các hệ thống LMS/EXAM/PM không thuộc đợt HS-01 này. Adapter phải xác nhận điểm đã chốt/task đã nghiệm thu, giữ ID và ngày gốc trước khi lưu nguồn. `attendanceRate`, `homeworkRate`, `rpoint` không tự được coi là điểm môn học.

## Công thức và ví dụ tính tay

```text
hệ số thời gian = 1   nếu recordedAt >= thời điểm tính - 1 năm lịch UTC
                 0.5 nếu cũ hơn
verified_score = Σ(score × weight × hệ số) / Σ(weight × hệ số)
```

Mốc đúng một năm vẫn dùng hệ số 1. Chỉ dùng bằng chứng `verified=true`; SELF luôn bị loại. Không có bằng chứng xác thực thì `verifiedScore=null`, `level=null`.

- 80 điểm mới, trọng số 0.7 và 60 điểm cũ, trọng số 0.3: `(80×0.7 + 60×0.3×0.5) / (0.7 + 0.3×0.5) = 76.470588`, bậc `GOOD`.
- Java candidate1 tại mốc demo 2026-11-11: LMS 85 trọng số 0.7 và EXAM 80 trọng số 1, đều mới: `(85×0.7 + 80×1) / 1.7 = 82.058824`, bậc `GOOD`.
- Điểm nguồn `8.5`, `score_max=10` thành 85 trước khi áp dụng trọng số. Fixture demo vốn đã ở thang 100 nên seeder lưu `score_max=100`.

Bậc dựa trên giá trị chưa làm tròn: `<40 BASIC`, `<70 FAIR`, `<85 GOOD`, còn lại `EXCELLENT`. Điểm tổng hợp trả ra/lưu sáu chữ số thập phân; không dùng điểm đã làm tròn để phân bậc.

## Chạy local qua identity, profile và gateway

Yêu cầu Java 21, Maven, Node.js và PostgreSQL. Tất cả lệnh dưới đây chạy từ gốc repo, bằng các terminal riêng cho tiến trình chạy lâu. Java không tự đọc file `.env`; đặt biến môi trường trong đúng terminal trước khi chạy. Nếu dùng URL/database riêng, đặt cả `IDENTITY_DB_URL` và `PROFILE_DB_URL` về cùng database khi bật dữ liệu demo.

Build và cài các module nội bộ vào Maven local trước, để lệnh chạy từng service tìm được thư viện chung:

```powershell
mvn -f server/pom.xml clean install
```

PostgreSQL mẫu được publish ở cổng **5432**, database/user/password đều theo mặc định của Compose:

```powershell
docker compose -f infra/compose.yaml up -d postgres
```

Terminal mock:

```powershell
$env:SERVICE_KEY = "dev-key"
$env:PORT = "9000"
npm --prefix mock ci
npm --prefix mock start
```

Terminal identity, chạy **trước profile** và chờ khởi động xong để tạo tài khoản thật:

```powershell
$env:JWT_SECRET = "hs01_local_development_secret_change_me_32bytes"
$env:SPRING_PROFILES_ACTIVE = "dev"
$env:DEV_DATA_ENABLED = "true"
$env:LMS_DEV_TICKETS_ENABLED = "true"
$env:LMS_BASE_URL = "http://localhost:9000"
$env:LMS_SERVICE_KEY = "dev-key"
$env:MAIL_ENABLED = "false"
mvn -f server/pom.xml -pl services/identity-service spring-boot:run
```

Terminal profile:

```powershell
$env:JWT_SECRET = "hs01_local_development_secret_change_me_32bytes"
$env:SPRING_PROFILES_ACTIVE = "dev"
$env:PROFILE_DEMO_DATA_ENABLED = "true"
mvn -f server/pom.xml -pl services/profile-service spring-boot:run
```

Terminal gateway:

```powershell
mvn -f server/pom.xml -pl api-gateway spring-boot:run
```

Các URL mặc định là identity 8081, profile 8082, gateway 8080 và mock 9000. Có thể đổi qua `IDENTITY_SERVICE_PORT`, `PROFILE_SERVICE_PORT`, `GATEWAY_PORT`, `IDENTITY_SERVICE_URL`, `PROFILE_SERVICE_URL` và cấu hình LMS tương ứng.

Seeder HS-01 chỉ chạy khi đồng thời có profile Spring `dev` và `PROFILE_DEMO_DATA_ENABLED=true`; mặc định tắt. Nó tra `public.users.lms_user_id` để lấy UUID thật, không dùng `fixtureUserId`, không tạo/sửa tài khoản và không tự công khai hồ sơ. Demo vì vậy cần identity và profile chung database vật lý; ứng dụng bình thường chỉ xác thực JWT, không cần truy vấn identity khi đọc hồ sơ.

Fixture chính: `server/services/profile-service/src/main/resources/hs01/candidates.json`. Các ngày là ngày mẫu được ghi rõ, không phải dữ liệu live lấy từ mock. Chạy lại seeder không nhân đôi nguồn đã có và giữ chỉnh sửa của nguồn đó.

| Tài khoản LMS | Kết quả sau seed mới |
| --- | --- |
| `lms-sv-001` | 9 kỹ năng: 8 có điểm xác thực, ENGLISH chỉ tự khai 65 (`verifiedScore=null`, `level=null`) |
| `lms-sv-002` | 7 kỹ năng; không tự tạo thực tập |
| `lms-sv-003` | 4 kỹ năng; không tự tạo thực tập |

## Kiểm tra backend

```powershell
mvn -f server/pom.xml clean verify
postman collection run postman/hs01.postman_collection.json
```

Collection cần các tiến trình local và demo seed ở trên. Nó tự xin vé SSO rồi đổi thành JWT cho ba ứng viên, lấy UUID từ response NT-01; không chứa token hoặc UUID tài khoản cố định. Chạy cả collection đúng thứ tự. Nó kiểm tra cấu trúc/nguồn/ngày/điểm, điểm SELF riêng, recompute không đổi kết quả khi nguồn giữ nguyên, cách ly ứng viên, private/missing cùng 404, UUID sai và xác thực 401. Collection không sửa nguồn hoặc trạng thái công khai; nên dùng database demo riêng với dữ liệu seed ban đầu.

Kiểm thử tự động bao gồm calculator, validation, JPA/service, cập nhật/thu hồi nguồn, transaction/concurrency, JWT/quyền đọc qua MockMvc và gateway chuyển tiếp path/query/token. Kết quả build, kiểm tra PostgreSQL và chạy collection phải được ghi theo lần chạy thực tế trong phần nghiệm thu; lệnh ở trên là hướng dẫn tái hiện.

GET đọc điểm và bằng chứng trong transaction `REPEATABLE_READ` để không trộn điểm cũ với bằng chứng mới khi có recompute đồng thời. Test hồi quy snapshot chỉ chạy khi `HS01_TEST_DB_URL` là PostgreSQL: H2 có hành vi snapshot khác và không thay thế kiểm tra này. Khi không đặt biến đó, riêng test PostgreSQL này được bỏ qua.

Có thể chạy bộ tích hợp trên PostgreSQL thử nghiệm riêng bằng biến môi trường:

```powershell
$env:HS01_TEST_DB_URL = "jdbc:postgresql://localhost:5432/rikkei_jobs_hs01_test"
$env:HS01_TEST_DB_USER = "postgres"
$env:HS01_TEST_DB_PASSWORD = "postgres"
mvn -f server/pom.xml -pl services/profile-service -am test
```

Database thử nghiệm phải được tạo trước; các biến trên chỉ đổi các test tích hợp hỗ trợ chúng, không chuyển toàn bộ test H2 sang PostgreSQL. Không trỏ chúng vào dữ liệu demo hoặc database dùng chung của nhóm.

Với cache Maven của máy hiện tại, có thể thêm `'-Dmaven.repo.local=C:/Users/Admin/.m2/repository'`; chỉ thêm `-o` khi các dependency đã có đủ trong cache.

## Phần còn lại ngoài backend HS-01

- Frontend hiển thị bảng/biểu đồ, lọc và trải nghiệm chứng cứ; kiểm tra trực quan BE–FE.
- Workflow ứng viên đồng ý/rút công khai và cán bộ duyệt hồ sơ; HS-01 hiện chỉ lưu/đọc hai cờ để thi hành quyền.
- Adapter NT-05 lấy và xác nhận nguồn thật; cập nhật bản ghi nguồn phải đi qua luồng backend tin cậy.
- Luồng tự đánh giá/mentor và chính sách tổng hợp nhiều bằng chứng SELF thuộc chức năng đánh giá tương ứng.

## Kết quả xác minh ngày 2026-11-11

- Lượt `mvn ... verify` cuối toàn backend: **12/12 module SUCCESS, 142 test, 0 lỗi, 0 bỏ qua**, với URL database PostgreSQL thử nghiệm được bật.
- 80 kiểm thử của profile-service thành công, không bỏ qua khi bật URL PostgreSQL thử nghiệm: 30 ca HTTP/source/snapshot/ping dùng PostgreSQL 18.4, các ca calculator/validation/JPA cốt lõi/demo còn lại dùng H2.
- Gateway có 3 kiểm thử thành công, gồm chuyển tiếp đường dẫn số ít/số nhiều, query và Authorization.
- Đã khởi chạy JAR thật của identity, profile, gateway và mock trên cổng test riêng, cùng database tạm. Postman CLI: **18 request, 31 assertion, 0 lỗi**; JWT/UUID được lấy thật qua SSO.
- Đã khởi động lại profile với seed demo: không nhân đôi nguồn/bằng chứng và không công khai hồ sơ.
- Thêm EXAM Java 10/10 rồi gọi API recompute: Java đổi từ `82.058824` lên `(85×0.7+80+100)/(0.7+1+1)=88.703704`, bậc EXCELLENT. Thu hồi nguồn và recompute đưa điểm về `82.058824`, bằng chứng bị thu hồi biến mất.
- Bật cả đồng ý và duyệt trên database test cho phép ứng viên khác đọc `200`; rút đồng ý lập tức trả lại `404` ở request tiếp theo.
- Xác nhận hai lịch sử `public.flyway_schema_history` và `profile.flyway_schema_history` cùng tồn tại; không sửa migration NT-01 hay dữ liệu người dùng đang có.
- Đã qua review độc lập và sửa lỗi snapshot bằng test RED→GREEN trên PostgreSQL. OpenAPI YAML đã parse bằng SafeConstructor; JSON collection đã kiểm tra cấu trúc.
- Các tiến trình và database dùng để kiểm tra được tạo riêng, đã dừng và dọn cluster tạm sau kiểm tra. Không commit hoặc push các thay đổi của đợt này.
