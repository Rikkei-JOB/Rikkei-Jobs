# Rikkei Jobs Platform

Nền tảng tuyển dụng và phát triển sự nghiệp, sử dụng backend Spring Boot và frontend Next.js. Repo hiện có khung 8 service sau API gateway (`/api/v1`), đăng nhập SSO/HR bằng JWT (NT-01), khung phân quyền theo vai trò, Swagger UI `/api/docs` và layout theo vai trò ở frontend (NT-02), backend Skill Profile (HS-01), hồ sơ doanh nghiệp và tài khoản HR (DN-01), cùng mock Express đủ 11 endpoint hệ thống ngoài (LMS, Khảo thí, lms-pm, Rikkei Meet). API nghiệp vụ của các service còn lại do các task sau bổ sung.

Hướng dẫn chi tiết theo task: đăng nhập và tài khoản mẫu [NT-01](docs/api/NT-01-authentication.md); khung phân quyền và Swagger UI [NT-02](docs/api/NT-02-phan-quyen.md); API kỹ năng, migration và dữ liệu demo [HS-01 backend](docs/hs01-independent-backend.md); hồ sơ doanh nghiệp, tài khoản HR, logo [DN-01](docs/api/DN-01-company-hr.md) (OpenAPI [company-openapi.yaml](docs/api/company-openapi.yaml), request mẫu [DN-01.http](docs/api/DN-01.http)). Với các task này, dùng hướng dẫn chuyên biệt thay cho các ghi chú trạng thái khung bên dưới.

Nhóm giữ cấu trúc `server/`, `web/` và dùng **`test`** làm nhánh tích hợp. Tạo `feature/<mã>-<tên>` từ `test`, mở PR vào `test` với tiêu đề `[NT-01] ...`. Xem [quy ước chung](docs/conventions.md).

---

## 📁 Cấu trúc Dự án (Project Structure)

```text
rikkei-jobs/
│
├── server/                                # Backend Microservices (Spring Boot / Java 21)
│   ├── api-gateway/                       # Cổng vào :8080, định tuyến /api/v1/<khu>/**, Swagger UI /api/docs
│   ├── services/
│   │   ├── identity-service/              # Đăng nhập, LMS SSO, quản lý định danh và phân quyền
│   │   ├── profile-service/               # Skill Profile, CV, Portfolio, bằng chứng
│   │   ├── company-service/               # Doanh nghiệp, tài khoản HR liên quan, JD (DN-01: hồ sơ công ty, HR, logo)
│   │   ├── internship-service/            # Thực tập IOC, ghép ứng viên, nghiệm thu
│   │   ├── assessment-service/            # Phiếu đánh giá mentor, tự đánh giá, chỉ số năng lực
│   │   ├── learning-service/              # Skill gap, gợi ý môn học bổ sung qua LMS
│   │   ├── recruitment-service/           # Ứng tuyển, phỏng vấn, offer, nhận việc
│   │   └── ai-service/                    # Gợi ý nghề nghiệp, hỗ trợ CV, phỏng vấn bằng Gemini
│   ├── libs/
│   │   ├── common-core/                   # JWT, guard phân quyền (Roles, OwnershipGuard), lỗi {code,message}, OpenAPI
│   │   └── common-observability/          # Thư viện dùng chung metrics, tracing (Prometheus, OTel)
│   └── pom.xml                            # Parent POM quản lý dependencies backend
│
├── web/                                   # Frontend Application (Next.js / React / TypeScript)
│   ├── src/
│   │   ├── app/
│   │   │   ├── (pages)/(home)/page.tsx     # Trang mẫu tại URL /
│   │   │   ├── (pages)/(auth)/            # /login, /sso-callback, đổi/quên/đặt lại mật khẩu (NT-01)
│   │   │   ├── (pages)/(app)/             # Trang sau đăng nhập /<vai trò>/..., layout + route guard (NT-02)
│   │   │   ├── components/auth/           # Form đăng nhập, khung trang xác thực
│   │   │   ├── components/layout/         # Layout chung có menu theo vai trò, trang 403
│   │   │   ├── components/ui/             # Button, Card, Input, Tabs...
│   │   │   ├── layout.tsx                 # Root layout
│   │   │   └── globals.css
│   │   ├── config/                        # env, vai trò (roles.ts), menu theo vai trò (menu.ts)
│   │   ├── helper/                        # Hàm hỗ trợ
│   │   ├── hooks/                         # Phiên đăng nhập (zustand), route guard
│   │   ├── interface/                     # Kiểu dữ liệu
│   │   ├── schemas/                       # Schema kiểm tra form (zod)
│   │   ├── services/                      # API client (axios: gắn token, 401 → /login)
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
│   ├── server.js                          # 11 endpoint, kiểm tra service key, công cụ test /mock/*
│   ├── send-evaluation-links.js           # Script TT-04: lms-pm gọi ngược backend
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
│   ├── api/                               # Tài liệu từng task (NT-01, NT-02...), OpenAPI, ảnh màn hình
│   ├── database/                          # ERD và quy ước migration (erd.md)
│   ├── decisions/                         # Architecture Decision Records (ADRs)
│   ├── conventions.md                     # Git/PR, API, vai trò, tài khoản mẫu, kỹ năng
│   └── mock-contract.md                   # Hợp đồng API mock hệ thống ngoài
│
├── .github/                               # CI/CD Workflows & Quản lý repository
│   ├── workflows/                         # GitHub Actions pipelines
│   └── CODEOWNERS                         # Phân quyền phụ trách từng phần codebase
│
├── scripts/                               # Tiện ích phát triển & tự động hoá
├── .env.example                           # Mẫu biến môi trường
└── README.md                              # Hướng dẫn tổng quan dự án
```

Các thư mục khung có thể chỉ chứa `.gitkeep`. Mock đã đủ 11 endpoint; cách chạy xem [mock README](mock/README.md), hợp đồng API xem [docs/mock-contract.md](docs/mock-contract.md).

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

Đây là phạm vi nghiệp vụ dự kiến. Hiện đã triển khai: đăng nhập và JWT ở `identity-service` (NT-01),
Skill Profile ở `profile-service` (HS-01), hồ sơ doanh nghiệp, tài khoản HR và logo ở `company-service` (DN-01);
các service khác mới có khung và API `GET /<khu>/ping`. Mọi API đi qua gateway dưới `/api/v1`, xem gộp ở Swagger UI `/api/docs` (NT-02).
Quy ước viết service: [service-conventions](docs/architecture/service-conventions.md);
cấu trúc, biến môi trường và Dockerfile: [Backend services](server/services/README.md).

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

Các kết nối LMS, IOC và Gemini nằm trong package `client/` của service phụ trách.
Hiện có client LMS SSO của `identity-service` (`HttpLmsClient`, gọi mock ở cổng 9000, gắn `X-Service-Key`) và client gọi API nội bộ của identity từ `company-service` (DN-01).

## Hướng dẫn khởi chạy

Mỗi phần bên dưới bắt đầu từ **thư mục gốc repo**, dùng terminal riêng cho từng tiến trình chạy lâu.

### Chạy nhanh để đăng nhập và thử phân quyền

Cần đủ các tiến trình sau, mỗi lệnh chạy lâu dùng một terminal riêng mở từ gốc repo (chi tiết ở các mục 1–6):

```sh
cp .env.example .env                                                       # lần đầu
docker compose --env-file .env -f infra/compose.yaml up -d postgres mailhog
cd mock && npm ci && npm start                                             # :9000, mock LMS
mvn -f server/pom.xml install -DskipTests                                  # lần đầu và mỗi khi đổi code backend
mvn -f server/services/identity-service/pom.xml spring-boot:run "-Dspring-boot.run.profiles=dev"   # :8081, chạy trước
mvn -f server/services/company-service/pom.xml spring-boot:run "-Dspring-boot.run.profiles=dev"    # :8085
mvn -f server/api-gateway/pom.xml spring-boot:run                          # :8080
cd web && cp .env.example .env.local && npm ci && npm run dev              # :3000
```

| Mở | Địa chỉ |
| --- | --- |
| Đăng nhập | `http://localhost:3000/login` |
| Swagger UI | `http://localhost:8080/api/docs` |
| Hộp thư dev (MailHog) | `http://localhost:8025` |

Tài khoản: tab **Qua LMS** chọn candidate1, mentor1, staff1, admin1 (không cần mật khẩu); tab **Doanh nghiệp** dùng `hr1@rikkeijobs.test`, `hr2@rikkeijobs.test`, `hr-new@rikkeijobs.test` mật khẩu `Test@123`. Các service khác chỉ cần khi gọi API của chúng; ví dụ màn Hồ sơ kỹ năng của ứng viên cần `profile-service` (HS-01, dữ liệu demo bật bằng `PROFILE_DEMO_DATA_ENABLED=true`). Không dùng `mvn -pl <module> -am spring-boot:run`: `-am` chạy cả parent POM và báo `Unable to find a suitable main class`.

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
docker compose --env-file .env -f infra/compose.yaml up -d postgres mailhog redis rabbitmq
docker compose --env-file .env -f infra/compose.yaml ps
docker compose --env-file .env -f infra/compose.yaml exec postgres pg_isready -U postgres -d rikkei_jobs
```

Chờ PostgreSQL báo `accepting connections` trước khi chạy service JPA. Nếu đổi user/database, thay hai giá trị trong lệnh kiểm tra cho phù hợp.

| Thành phần | Địa chỉ local | Mặc định |
| --- | --- | --- |
| PostgreSQL | `localhost:5432` | DB `rikkei_jobs`, user/password `postgres`/`postgres` |
| MailHog | SMTP `localhost:1025`, hộp thư `http://localhost:8025` | Nhận email đặt lại mật khẩu |
| Redis | `localhost:6379` | Chưa đặt mật khẩu trong Compose |
| RabbitMQ AMQP | `localhost:5672` | `guest`/`guest` |
| RabbitMQ Management | `http://localhost:15672` | `guest`/`guest` |
| MailHog SMTP | `localhost:1025` | Không cần đăng nhập; backend gửi email dev vào đây |
| MailHog hộp thư | `http://localhost:8025` | Xem email backend đã gửi (quên mật khẩu, thông báo...) |
| Grafana (tuỳ chọn) | `http://localhost:3001` | `admin`/`admin`; bật bằng `up -d prometheus grafana` |

Grafana chạy ở cổng **3001** (đổi bằng `GRAFANA_PORT`), Prometheus ở 9090 (`PROMETHEUS_PORT`), không còn trùng frontend 3000. Script `scripts/setup.sh` bật toàn bộ stack, kể cả nginx; luồng local nên bật từng service như lệnh ở trên.

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

Chạy service khác bằng cách thay tên ở cả lệnh build lẫn đường dẫn JAR, dùng cổng trong bảng phạm vi service. Gateway chạy được (cổng 8080). `identity-service` đã cấp JWT; chạy kèm seed tài khoản mẫu bằng profile `dev`. Để chạy bằng `spring-boot:run`, cài thư viện dùng chung trước rồi chạy riêng từng module (không dùng `-am spring-boot:run`, vì `-am` chạy cả parent POM và báo `Unable to find a suitable main class`). Trên PowerShell đặt tham số `-D...` trong dấu nháy:

```sh
mvn -f server/pom.xml -pl services/identity-service,services/company-service,api-gateway -am install -DskipTests
mvn -f server/services/identity-service/pom.xml spring-boot:run "-Dspring-boot.run.profiles=dev"   # :8081
mvn -f server/api-gateway/pom.xml spring-boot:run                                                  # :8080
```

Các service JPA dùng `ddl-auto: validate`; identity-service, profile-service và company-service đã có Flyway migration (company-service tạo schema Postgres riêng `company`); các service còn lại chưa có bảng. Dữ liệu mẫu chỉ có khi chạy profile `dev`.

**company-service (DN-01).** Chạy sau khi Postgres và identity-service (profile `dev`) đã lên; profile `dev` seed Công ty A, Công ty B và liên kết HR mẫu:

```sh
mvn -f server/services/company-service/pom.xml spring-boot:run "-Dspring-boot.run.profiles=dev"   # :8085 (sau bước install ở trên)
```

Biến môi trường mới (đã có trong `.env.example`): `INTERNAL_SERVICE_KEY` (khoá `X-Internal-Key` company-service gửi khi gọi `/internal/**` của identity, mặc định `dev-internal-key`, hai bên phải giống nhau), `IDENTITY_SERVICE_URL` (mặc định `http://localhost:8081`), `COMPANY_UPLOAD_DIR` (thư mục lưu logo, mặc định `./data/uploads`, đã được `.gitignore` bỏ qua), `COMPANY_DB_SCHEMA` (mặc định `company`), `DEV_DATA_ENABLED`. Hướng dẫn đầy đủ và kịch bản kiểm thử: [DN-01](docs/api/DN-01-company-hr.md).

`identity-service` kiểm tra kết nối SMTP trong health check: không chạy MailHog thì đặt `MAIL_ENABLED=false` (link đặt lại mật khẩu sẽ in ra log).

Tài liệu API của mọi service (Swagger UI): `http://localhost:8080/api/docs`. Bấm **Authorize**, dán `accessToken` lấy từ `POST /auth/login` để thử API cần đăng nhập. Sơ đồ bảng: [docs/database/erd.md](docs/database/erd.md).

Kiểm tra biên dịch/test toàn backend từ gốc repo (dùng H2, không cần Docker):

```sh
mvn -f server/pom.xml clean test
```

Riêng test phân quyền (NT-02): guard dùng chung cho 5 vai trò, và tiêu chí "candidate gọi API của HR → 403, hr1 đọc Công ty B → 404":

```sh
mvn -f server/pom.xml -pl libs/common-core,services/company-service -am test -Dtest='RoleGuardTest*,CompanyAuthorizationTest' -Dsurefire.failIfNoSpecifiedTests=false
```

### 5. Chạy frontend

Mở terminal mới từ gốc repo:

```sh
cd web
cp .env.example .env.local
npm ci
npm run dev -- --port 3000
```

Truy cập `http://localhost:3000/login`. Các nhóm route trong ngoặc không xuất hiện trong URL.

- Đăng nhập (NT-01): `/login` (tab "Qua LMS" và "Doanh nghiệp"), `/sso-callback?ticket=`, `/change-password`, `/forgot-password`, `/reset-password/<token>`. Khi bật `NEXT_PUBLIC_LMS_MOCK=true`, tab "Qua LMS" có 6 người dùng LMS mẫu (cần chạy `mock/` ở cổng 9000; backend xin vé hộ qua `POST /api/v1/auth/dev/sso-ticket`). HR mẫu `hr1@rikkeijobs.test`, `hr2@rikkeijobs.test` mật khẩu `Test@123`; `hr-new@rikkeijobs.test` phải đổi mật khẩu lần đầu. Chi tiết: [docs/api/NT-01-authentication.md](docs/api/NT-01-authentication.md).
- Sau đăng nhập (NT-02): layout chung với menu theo vai trò tại `/candidate`, `/hr`, `/mentor`, `/staff`, `/admin`. Mở trang của vai trò khác hiện trang 403; chưa đăng nhập thì về `/login`. Menu khai báo ở `web/src/config/menu.ts`. Chi tiết: [docs/api/NT-02-phan-quyen.md](docs/api/NT-02-phan-quyen.md).

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

`.env` ở gốc không tự áp dụng cho Next.js trong `web/`; frontend đọc `web/.env.local` (mẫu ở `web/.env.example`):

```dotenv
NEXT_PUBLIC_API_URL=http://localhost:8080/api/v1   # đi qua gateway
NEXT_PUBLIC_LMS_MOCK=true                          # tab "Qua LMS" hiện danh sách người dùng mock
```

Đổi file này phải khởi động lại Next.js.

Nếu không đặt biến, frontend mặc định gọi `http://localhost:8080/api/v1` (xem `web/src/config/env.ts`).

### 6. Chạy mock ngoài

Mở terminal mới từ gốc repo, không cần backend hoặc Docker:

```sh
cd mock
npm ci
npm start
```

Mặc định server chạy tại `http://localhost:9000`, key `X-Service-Key: dev-key`. Có thể đổi bằng biến môi trường `PORT`, `SERVICE_KEY` trong terminal chạy mock; server không tự đọc `.env` ở gốc. Chỉ backend gọi mock, không đưa service key vào frontend.

Mock có đủ 11 endpoint (LMS SSO, hồ sơ, kết quả môn, khoá học, ghi danh; Khảo thí; lms-pm; Rikkei Meet) và 4 công cụ test `/mock/reload`, `/mock/requests`, `/mock/enrollments`, `/mock/reset`. Danh sách đầy đủ xem [mock README](mock/README.md#endpoint) và [docs/mock-contract.md](docs/mock-contract.md).

Thử luồng SSO ở terminal PowerShell khác:

```powershell
$headers = @{ "X-Service-Key" = "dev-key" }
$issued = Invoke-RestMethod -Method Post -Uri http://localhost:9000/lms/sso/tickets -Headers $headers -ContentType "application/json" -Body '{"lmsUserId":"lms-sv-001"}'
Invoke-RestMethod -Method Post -Uri http://localhost:9000/lms/sso/verify -Headers $headers -ContentType "application/json" -Body (@{ ticket = $issued.ticket } | ConvertTo-Json)
```

Cấp vé trả `201 {ticket, issuedAt}`, verify trả `200` với candidate1. Mock cho phép verify lại cùng vé và không tự kiểm tra hạn vé; backend phải xử lý chống dùng lại/hết hạn. Fixture dùng ID `lms-sv-001`, `lms-sv-002`, `lms-sv-003`. Các request thử khác nằm trong [mock/requests.http](mock/requests.http).

Với request có JSON hợp lệ, thiếu/sai key trả `401 INVALID_SERVICE_KEY`. Request JSON hỏng trả `400 INVALID_JSON` từ middleware parse body, có thể xảy ra trước kiểm tra key. `/mock/*` là công cụ local không yêu cầu key; `POST /mock/reload` chỉ nạp lại fixture, `POST /mock/reset` xoá vé, ghi danh, thành viên đợt và nhật ký (gọi trước mỗi lần demo). Dừng bằng `Ctrl+C`.

Script TT-04 (lms-pm gọi ngược backend lấy link phiếu đánh giá), cần backend đang chạy:

```sh
cd mock
npm run evaluation-links -- IOC-JB-2026-09
```

Đổi địa chỉ backend bằng biến môi trường `BACKEND_URL` (mặc định `http://localhost:8080`).

### 7. Email và tài khoản demo

Email dev đi qua MailHog: `docker compose --env-file .env -f infra/compose.yaml up -d mailhog`, xem thư ở `http://localhost:8025`. Tài khoản demo và Công ty A/B được seed khi chạy `identity-service` và `company-service` với profile `dev` (bảng ở [NT-01-authentication.md](docs/api/NT-01-authentication.md#9-dữ-liệu-mẫu)). Sáu định danh trong `mock/data.json` phục vụ SSO. 20 kỹ năng trong [conventions](docs/conventions.md) chưa được seed. Backend gửi SMTP tới `localhost:1025` theo `MAIL_HOST`, `MAIL_PORT`, `MAIL_FROM` trong `.env.example`; không có thư nào được gửi ra ngoài thật.

### 8. Dừng và xử lý lỗi

Dừng Java/Next.js bằng `Ctrl+C`. Từ gốc repo, dừng hạ tầng và giữ dữ liệu volume:

```sh
docker compose --env-file .env -f infra/compose.yaml down
```

- Không tìm thấy `mvn`: cài Maven 3.9+, thêm thư mục `bin` vào `PATH`, mở lại terminal; kiểm tra `mvn -version` dùng JDK 21.
- Docker báo không kết nối daemon: bật Docker Desktop/Engine rồi kiểm tra lại `docker version`.
- DB connection refused: kiểm tra `ps`, chờ `pg_isready`, đối chiếu biến DB trong terminal Java với Compose.
- Đổi mật khẩu `.env` nhưng DB vẫn từ chối: volume PostgreSQL cũ giữ tài khoản khởi tạo trước đó; dùng cấu hình tương ứng hoặc cập nhật tài khoản DB. Không mặc định xóa volume.
- Cổng 3000 bận khi chạy web: Grafana đã chuyển sang 3001; nếu vẫn bận, tìm tiến trình đang chiếm cổng (`netstat -ano | findstr :3000` trên Windows) hoặc chạy web ở cổng khác bằng `npm run dev -- --port 3002`.
- Build web lỗi tải font Inter: kiểm tra kết nối tới Google Fonts; layout hiện dùng `next/font/google`.
- `npm ci` báo lockfile không đồng bộ: người sửa dependency cần chạy `npm install` và commit cả `package.json`, `package-lock.json`; sau đó máy mentor chạy lại `npm ci`.
- `npm ci` cảnh báo hoặc báo lỗi peer dependency: lockfile hiện có ESLint `10.14.0`, trong khi `eslint-plugin-import@2.32.0` khai báo hỗ trợ đến ESLint 9. Nếu cài đặt/lint thất bại, nhóm cần đồng bộ phiên bản ESLint và các plugin rồi cập nhật lockfile; không coi cảnh báo này là bằng chứng build đã thành công.

### Trạng thái kiểm tra hướng dẫn

Đã đối chiếu đường dẫn, script npm, module/parent POM và kiểm tra cú pháp Compose bằng `.env.example`. Mock đã cài bằng `npm ci` và kiểm tra HTTP trên cổng 9000: key sai/thiếu trả `401`; đủ 11 endpoint trả dữ liệu cho 3 ứng viên; các ca lỗi `400`, `404`, `409 ALREADY_ENROLLED`, `409 ALREADY_MEMBER`; công cụ requests/reload/enrollments/reset; script TT-04 với backend giả và khi backend tắt.

NT-01, NT-02, HS-01 và DN-01 đã chạy thử toàn luồng: gateway, identity, profile, company-service (profile `dev`), mock, MailHog và frontend; `mvn test` toàn backend đạt. Kết quả kiểm tra riêng của DN-01: [docs/api/DN-01-company-hr.md](docs/api/DN-01-company-hr.md#9-trạng-thái-xác-minh). API nghiệp vụ của các service khác vẫn cần được triển khai.
