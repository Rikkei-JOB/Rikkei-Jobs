# Rikkei Jobs Platform

Nền tảng tuyển dụng và phát triển sự nghiệp, sử dụng backend Spring Boot và frontend Next.js. Repo có khung 8 service, gateway, mock Express, luồng đăng nhập NT-01 và backend Skill Profile HS-01. Các nghiệp vụ khác tiếp tục được phát triển theo task.

Hướng dẫn đăng nhập và tài khoản mẫu: [NT-01](docs/api/NT-01-authentication.md). API kỹ năng, migration và cách chạy dữ liệu demo: [HS-01 backend](docs/hs01-independent-backend.md). Với hai task này, dùng hướng dẫn chuyên biệt thay cho các ghi chú trạng thái khung bên dưới.

Nhóm giữ cấu trúc `server/`, `web/` và dùng **`test`** làm nhánh tích hợp. Tạo `feature/<mã>-<tên>` từ `test`, mở PR vào `test` với tiêu đề `[NT-01] ...`. Xem [quy ước chung](docs/conventions.md).

---

## 📁 Cấu trúc Dự án (Project Structure)

```text
rikkei-jobs/
│
├── server/                                # Backend Microservices (Spring Boot / Java 21)
│   ├── api-gateway/                       # Khung gateway; chưa có Application/routes
│   ├── services/
│   │   ├── identity-service/              # Đăng nhập, LMS SSO, quản lý định danh và phân quyền
│   │   ├── profile-service/               # Skill Profile, CV, Portfolio, bằng chứng
│   │   ├── company-service/               # Doanh nghiệp, tài khoản HR liên quan, JD
│   │   ├── internship-service/            # Thực tập IOC, ghép ứng viên, nghiệm thu
│   │   ├── assessment-service/            # Phiếu đánh giá mentor, tự đánh giá, chỉ số năng lực
│   │   ├── learning-service/              # Skill gap, gợi ý môn học bổ sung qua LMS
│   │   ├── recruitment-service/           # Ứng tuyển, phỏng vấn, offer, nhận việc
│   │   └── ai-service/                    # Gợi ý nghề nghiệp, hỗ trợ CV, phỏng vấn bằng Gemini
│   ├── libs/
│   │   └── common-observability/          # Thư viện dùng chung metrics, tracing (Prometheus, OTel)
│   └── pom.xml                            # Parent POM quản lý dependencies backend
│
├── web/                                   # Frontend Application (Next.js / React / TypeScript)
│   ├── src/
│   │   ├── app/
│   │   │   ├── (pages)/(home)/page.tsx     # Trang mẫu tại URL /
│   │   │   ├── components/ui/             # Button, Card, Input
│   │   │   ├── layout.tsx                 # Root layout
│   │   │   └── globals.css
│   │   ├── config/                        # Khung cấu hình
│   │   ├── helper/                        # Khung hàm hỗ trợ
│   │   ├── hooks/                         # Khung custom hooks
│   │   ├── interface/                     # Khung kiểu dữ liệu
│   │   ├── schemas/                       # Khung schema kiểm tra dữ liệu
│   │   ├── services/                      # Khung API client, chưa kết nối backend
│   │   └── lib/utils.ts
│   ├── public/
│   ├── next.config.ts
│   ├── tsconfig.json
│   ├── eslint.config.mjs
│   ├── postcss.config.mjs
│   ├── components.json
│   ├── package-lock.json
│   └── package.json
│
├── mock/                                  # Mock ngoài, chạy độc lập cổng 9000
│   ├── server.js                          # SSO, kiểm tra service key, công cụ test
│   ├── data.json                          # Fixture LMS, khảo thí, thực tập, khóa học
│   ├── requests.http                      # Request mẫu cho IntelliJ / REST Client
│   ├── package.json
│   ├── package-lock.json
│   └── README.md
│
├── infra/                                 # Cấu hình Hạ tầng & Triển khai
│   ├── compose.yaml                       # Docker Compose cho Database, Cache, Proxy, Monitoring
│   ├── docker/                            # Placeholder; Dockerfile nằm trong từng service
│   ├── nginx/                             # Reverse Proxy & Load Balancer configuration
│   └── monitoring/                        # Cấu hình Prometheus, Grafana dashboards
│
├── docs/                                  # Tài liệu dự án
│   ├── architecture/                      # Sơ đồ kiến trúc & luồng dữ liệu
│   ├── api/                               # OpenAPI / Swagger specs
│   ├── database/                          # ERD & Database migrations scripts
│   ├── decisions/                         # Architecture Decision Records (ADRs)
│   └── conventions.md                     # Git/PR, API, tài khoản mẫu, hợp đồng mock
│
├── .github/                               # CI/CD Workflows & Quản lý repository
│   ├── workflows/                         # GitHub Actions pipelines
│   └── CODEOWNERS                         # Phân quyền phụ trách từng phần codebase
│
├── scripts/                               # Tiện ích phát triển & tự động hoá
├── .env.example                           # Mẫu biến môi trường
└── README.md                              # Hướng dẫn tổng quan dự án
```

Các thư mục khung có thể chỉ chứa `.gitkeep`. Mock đã có một phần endpoint; cách chạy và trạng thái từng endpoint xem [mock README](mock/README.md), hợp đồng mục tiêu xem [conventions](docs/conventions.md#5-mock-hệ-thống-ngoài).

---

## Phạm vi các service

| Service | Cổng mặc định | Trách nhiệm chính |
| --- | --- | --- |
| api-gateway | 8080 | Định tuyến API, xử lý các chức năng bảo vệ tại điểm vào |
| identity-service | 8081 | Đăng nhập, LMS SSO, quản lý định danh và phân quyền |
| profile-service | 8082 | Skill Profile, CV, Portfolio, bằng chứng |
| company-service | 8085 | Doanh nghiệp, tài khoản HR liên quan, JD |
| internship-service | 8086 | Thực tập IOC, ghép ứng viên, nghiệm thu |
| assessment-service | 8087 | Phiếu đánh giá mentor, tự đánh giá, chỉ số năng lực |
| learning-service | 8084 | Skill gap, gợi ý môn học bổ sung qua LMS |
| recruitment-service | 8083 | Ứng tuyển, phỏng vấn, offer, nhận việc |
| ai-service | 8088 | Gợi ý nghề nghiệp, hỗ trợ CV, phỏng vấn bằng Gemini |

Đây là phạm vi nghiệp vụ dự kiến, chưa phải các tính năng đã triển khai.
Tám service nghiệp vụ hiện có lớp Application và khung thư mục; API gateway
hiện mới có POM và các thư mục nguồn rỗng. Hướng dẫn cấu trúc, biến môi trường
và Dockerfile: [Backend services](server/services/README.md).

Các ranh giới dữ liệu cần giữ khi triển khai:

- `identity-service` sở hữu thông tin đăng nhập, LMS SSO, định danh và quyền;
  `company-service` quản lý hồ sơ HR và quan hệ với doanh nghiệp qua ID định danh.
- `company-service` sở hữu doanh nghiệp và JD; `recruitment-service` tham chiếu
  JD để quản lý ứng tuyển, phỏng vấn, offer và nhận việc.
- `profile-service` sở hữu Skill Profile, CV, Portfolio và bằng chứng;
  `assessment-service` sở hữu phiếu đánh giá và chỉ số năng lực.
- `internship-service` sở hữu ghép ứng viên và nghiệm thu thực tập IOC;
  phiếu đánh giá mentor thuộc `assessment-service`.
- `learning-service` tính skill gap và gợi ý môn học bổ sung qua LMS;
  `ai-service` dùng Gemini để gợi ý nghề nghiệp, hỗ trợ CV và phỏng vấn.

Các kết nối LMS, IOC và Gemini sẽ được triển khai trong package `client/`
của service phụ trách; hiện chưa có client gọi hệ thống ngoài.

## Hướng dẫn khởi chạy

Mỗi phần bên dưới bắt đầu từ **thư mục gốc repo**, dùng terminal riêng cho từng tiến trình chạy lâu. Frontend mẫu có thể chạy độc lập; chưa có chức năng đăng nhập hoặc gọi API nghiệp vụ.

### 1. Chuẩn bị môi trường

- **JDK 21**, **Maven 3.9+** trên `PATH`. Repo chưa có Maven Wrapper.
- **Node.js 24.x** và npm để dùng thống nhất khi chạy các lệnh frontend. Lockfile hiện dùng Next.js 16, React 19, TypeScript 6, ESLint 10; yêu cầu Node của ESLint là `^20.19.0 || ^22.13.0 || >=24`, nên hướng dẫn chung `20.x+` là chưa đủ chính xác.
- **Docker Engine/Desktop đang chạy**, có Docker Compose v2 trở lên; Windows dùng Linux containers.
- Internet để tải dependency/image lần đầu. Frontend dùng `next/font/google` tải font Inter khi build hoặc biên dịch trang.

```sh
java -version
mvn -version
node --version
npm --version
docker version
docker compose version
```

PowerShell: nếu `npm.ps1` bị chặn, dùng `npm.cmd` thay cho `npm`. Với các lệnh cURL dưới đây, Windows dùng `curl.exe` để tránh alias PowerShell.

### 2. Tạo cấu hình local

PowerShell, giữ nguyên `.env` nếu đã có:

```powershell
if (-not (Test-Path .env)) { Copy-Item .env.example .env }
```

Bash (Linux/macOS/Git Bash):

```bash
test -f .env || cp .env.example .env
```

Compose đọc cấu hình bằng `--env-file .env`. **Java không tự nạp `.env`**; mặc định các service JPA kết nối `localhost:5432/rikkei_jobs`, user/password `postgres`/`postgres`, khớp Compose mặc định. Nếu đổi thông tin DB, đặt cùng giá trị trong môi trường terminal chạy Java, ví dụ PowerShell:

```powershell
$env:POSTGRES_HOST = "localhost"
$env:POSTGRES_PORT = "5432"
$env:POSTGRES_DB = "rikkei_jobs"
$env:POSTGRES_USER = "postgres"
$env:POSTGRES_PASSWORD = "postgres"
```

Bash dùng `export POSTGRES_HOST=localhost` và tương tự cho các biến còn lại. Cấu hình riêng từng service qua `<NAME>_DB_URL`, `<NAME>_DB_USER`, `<NAME>_DB_PASSWORD` được mô tả trong [backend README](server/services/README.md#local-configuration).

### 3. Chạy hạ tầng cần thiết

Từ gốc repo:

```sh
docker compose --env-file .env -f infra/compose.yaml config --quiet
docker compose --env-file .env -f infra/compose.yaml up -d postgres redis rabbitmq
docker compose --env-file .env -f infra/compose.yaml ps
docker compose --env-file .env -f infra/compose.yaml exec postgres pg_isready -U postgres -d rikkei_jobs
```

Chờ PostgreSQL báo `accepting connections` trước khi chạy service JPA. Nếu đổi user/database, thay hai giá trị trong lệnh kiểm tra cho phù hợp.

| Thành phần | Địa chỉ local | Mặc định |
| --- | --- | --- |
| PostgreSQL | `localhost:5432` | DB `rikkei_jobs`, user/password `postgres`/`postgres` |
| Redis | `localhost:6379` | Chưa đặt mật khẩu trong Compose |
| RabbitMQ AMQP | `localhost:5672` | `guest`/`guest` |
| RabbitMQ Management | `http://localhost:15672` | `guest`/`guest` |

Không dùng `docker compose up -d` cho toàn bộ stack ở bước này: **Grafana đang ánh xạ cổng 3000, trùng frontend**. `GRAFANA_PORT` và `PROMETHEUS_PORT` trong `.env.example` chưa điều khiển port mapping vì Compose ghi cứng cổng. Nếu cần Grafana cùng web, sửa mapping Grafana thành `3001:3000` trước khi bật. Nginx trỏ tới gateway chưa hoàn thiện nên chưa cần bật. Script `scripts/setup.sh` hiện bật toàn bộ stack; dùng các lệnh chọn service ở trên cho luồng local này.

### 4. Build và chạy một backend service

Từ gốc repo, ví dụ `profile-service` (cần PostgreSQL ở bước 3):

```sh
mvn -f server/pom.xml -pl services/profile-service -am clean package
java -jar server/services/profile-service/target/profile-service-1.0.0-SNAPSHOT.jar
```

Mở terminal khác kiểm tra:

```sh
curl --fail http://localhost:8082/actuator/health
```

PowerShell có thể dùng `Invoke-RestMethod http://localhost:8082/actuator/health`. Kỳ vọng khi service và DB hoạt động là `{"status":"UP"}`. Nếu đổi cổng service, thay cổng trong URL kiểm tra.

Để chạy khung service không cần DB, dùng `ai-service`:

```sh
mvn -f server/pom.xml -pl services/ai-service -am clean package
java -jar server/services/ai-service/target/ai-service-1.0.0-SNAPSHOT.jar
```

Health: `http://localhost:8088/actuator/health`. Chưa có Gemini client, nên chưa cần API key Gemini để chạy phần khung này.

Chạy service khác bằng cách thay tên ở cả lệnh build lẫn đường dẫn JAR, dùng cổng trong bảng phạm vi service. **Chưa chạy gateway bằng `mvn spring-boot:run`** vì module thiếu main class. `identity-service` còn Spring Security mặc định, chưa xác thực bằng JWT hoặc tài khoản mẫu.

Các service JPA dùng `ddl-auto: validate`; thư mục migration còn là placeholder, chưa có Flyway hoặc seed. Không có lệnh tạo bảng nghiệp vụ/tài khoản mẫu ở thời điểm hiện tại.

Kiểm tra biên dịch/test toàn backend từ gốc repo:

```sh
mvn -f server/pom.xml clean test
```

### 5. Chạy frontend mẫu

Mở terminal mới từ gốc repo:

```sh
cd web

Các trang đăng nhập (NT-01): `/login` (tab "Qua LMS" và "Doanh nghiệp"), `/sso-callback?ticket=`, `/change-password`, `/forgot-password`, `/reset-password/<token>`. Khi bật `NEXT_PUBLIC_LMS_MOCK=true`, tab "Qua LMS" có 6 người dùng LMS mẫu để đăng nhập thử (cần chạy `mock/` ở cổng 9000; backend xin vé hộ qua `POST /api/v1/auth/dev/sso-ticket`). HR mẫu `hr1@rikkeijobs.test`, `hr2@rikkeijobs.test` mật khẩu `Test@123`; `hr-new@rikkeijobs.test` phải đổi mật khẩu lần đầu. Chi tiết: [docs/api/NT-01-authentication.md](docs/api/NT-01-authentication.md).
npm ci
npm run dev -- --port 3000
```

Truy cập `http://localhost:3000`. Trang hiện tại nằm tại `web/src/app/(pages)/(home)/page.tsx`; các nhóm route trong ngoặc không xuất hiện trong URL. Giao diện có ô tìm công việc và nút tìm kiếm mẫu, chưa xử lý tìm kiếm hoặc kết nối backend.

Frontend đã có `package-lock.json`, dùng `npm ci` để cài đúng phiên bản đã khóa. Sau khi cài, kiểm tra từ thư mục `web/`:

```sh
npm run type-check
npm run lint
npm run build
```

Chạy bản production từ `web/`, sau khi build thành công và đã dừng dev server:

```sh
npm run start -- --port 3000
```

Khi triển khai API client, tạo `web/.env.local` với nội dung sau rồi khởi động lại Next.js:

```dotenv
NEXT_PUBLIC_API_URL=http://localhost:8080/api/v1
```

`.env` ở gốc không tự áp dụng cho Next.js trong `web/`. Giá trị `/api` trong `.env.example` hiện chưa khớp quy ước `/api/v1`; mã frontend hiện chưa đọc biến này và gateway chưa có API để gọi.

### 6. Chạy mock ngoài

Mở terminal mới từ gốc repo, không cần backend hoặc Docker:

```sh
cd mock
npm ci
npm start
```

Mặc định server chạy tại `http://localhost:9000`, key `X-Service-Key: dev-key`. Có thể đổi bằng biến môi trường `PORT`, `SERVICE_KEY` trong terminal chạy mock; server không tự đọc `.env` ở gốc. Chỉ backend gọi mock, không đưa service key vào frontend.

Đã có `POST /lms/sso/tickets`, `POST /lms/sso/verify`, `GET /mock/requests`, `POST /mock/reload`. Chín endpoint nghiệp vụ còn lại (hồ sơ/kết quả LMS, courses, enrollments, khảo thí, lms-pm, Meet) và công cụ reset vẫn chưa có handler. Có fixture không có nghĩa endpoint đã hoạt động; xem [bảng trạng thái](mock/README.md#endpoint).

Thử luồng SSO ở terminal PowerShell khác:

```powershell
$headers = @{ "X-Service-Key" = "dev-key" }
$issued = Invoke-RestMethod -Method Post -Uri http://localhost:9000/lms/sso/tickets -Headers $headers -ContentType "application/json" -Body '{"lmsUserId":"lms-sv-001"}'
Invoke-RestMethod -Method Post -Uri http://localhost:9000/lms/sso/verify -Headers $headers -ContentType "application/json" -Body (@{ ticket = $issued.ticket } | ConvertTo-Json)
```

Cấp vé trả `201 {ticket, issuedAt}`, verify trả `200` với candidate1. Mock cho phép verify lại cùng vé và không tự kiểm tra hạn vé; backend phải xử lý chống dùng lại/hết hạn. Fixture dùng ID `lms-sv-001`, `lms-sv-002`, `lms-sv-003`. Các request thử khác nằm trong [mock/requests.http](mock/requests.http).

Với request có JSON hợp lệ, thiếu/sai key trả `401 INVALID_SERVICE_KEY`. Request JSON hỏng trả `400 INVALID_JSON` từ middleware parse body, có thể xảy ra trước kiểm tra key. `/mock/*` là công cụ local không yêu cầu key; `POST /mock/reload` chỉ nạp lại fixture, không xóa vé hoặc nhật ký. Dừng bằng `Ctrl+C`, khởi động lại để xóa trạng thái trong bộ nhớ.

### 7. Email và tài khoản demo

Chưa có Mailpit/MailHog, `email_outbox` hoặc seed tài khoản backend. Sáu định danh trong `mock/data.json` phục vụ SSO, không phải các tài khoản backend đã có mật khẩu. Yêu cầu 8 tài khoản demo và 20 kỹ năng nằm trong [conventions](docs/conventions.md).

### 8. Dừng và xử lý lỗi

Dừng Java/Next.js bằng `Ctrl+C`. Từ gốc repo, dừng hạ tầng và giữ dữ liệu volume:

```sh
docker compose --env-file .env -f infra/compose.yaml down
```

- Không tìm thấy `mvn`: cài Maven 3.9+, thêm thư mục `bin` vào `PATH`, mở lại terminal; kiểm tra `mvn -version` dùng JDK 21.
- Docker báo không kết nối daemon: bật Docker Desktop/Engine rồi kiểm tra lại `docker version`.
- DB connection refused: kiểm tra `ps`, chờ `pg_isready`, đối chiếu biến DB trong terminal Java với Compose.
- Đổi mật khẩu `.env` nhưng DB vẫn từ chối: volume PostgreSQL cũ giữ tài khoản khởi tạo trước đó; dùng cấu hình tương ứng hoặc cập nhật tài khoản DB. Không mặc định xóa volume.
- Cổng 3000 bận do Grafana: chạy `docker compose --env-file .env -f infra/compose.yaml stop grafana` trước khi bật web.
- Build web lỗi tải font Inter: kiểm tra kết nối tới Google Fonts; layout hiện dùng `next/font/google`.
- `npm ci` báo lockfile không đồng bộ: người sửa dependency cần chạy `npm install` và commit cả `package.json`, `package-lock.json`; sau đó máy mentor chạy lại `npm ci`.
- `npm ci` cảnh báo hoặc báo lỗi peer dependency: lockfile hiện có ESLint `10.14.0`, trong khi `eslint-plugin-import@2.32.0` khai báo hỗ trợ đến ESLint 9. Nếu cài đặt/lint thất bại, nhóm cần đồng bộ phiên bản ESLint và các plugin rồi cập nhật lockfile; không coi cảnh báo này là bằng chứng build đã thành công.

### Trạng thái kiểm tra hướng dẫn

Đã đối chiếu đường dẫn, script npm, module/parent POM và kiểm tra cú pháp Compose bằng `.env.example`. Mock đã cài bằng `npm ci` và kiểm tra HTTP trên cổng 9000: key sai/thiếu trả `401`, SSO cho 3 ứng viên, verify lại vé, vé không tồn tại, JSON hỏng, logs/reload và 9 endpoint TODO trả `404`.

Chưa xác nhận chạy toàn hệ thống: máy kiểm tra chưa có Maven trên `PATH`; lần thử `npm ci` của frontend chưa hoàn tất và có cảnh báo peer dependency nêu trên, nên chưa chạy build/lint/frontend server. Gateway, API nghiệp vụ backend, seed và các endpoint mock TODO vẫn cần được triển khai.
