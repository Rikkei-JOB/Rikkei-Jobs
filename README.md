# Rikkei Jobs Platform

Nền tảng tuyển dụng và phát triển sự nghiệp toàn diện, xây dựng theo kiến trúc Microservices hướng sự kiện và Frontend hiện đại.

---

## 📁 Cấu trúc Dự án (Project Structure)

```text
rikkei-jobs/
│
├── server/                                # Backend Microservices (Spring Boot / Java 21)
│   ├── api-gateway/                       # Spring Cloud Gateway định tuyến, rate limiting, auth filter
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
│   │   ├── app/                           # App Router / Root layout & routing
│   │   ├── pages/                         # Page-level components
│   │   ├── features/                      # Module hoá theo tính năng (Feature-based structure)
│   │   │   ├── auth/                      # Đăng nhập, đăng ký, quên mật khẩu
│   │   │   ├── profile/                   # Quản lý CV & hồ sơ cá nhân
│   │   │   ├── recruitment/               # Tìm kiếm việc làm, chi tiết công việc, nộp hồ sơ
│   │   │   ├── career/                    # Lộ trình phát triển & gợi ý nghề nghiệp
│   │   │   └── admin/                     # Quản trị viên & Nhà tuyển dụng
│   │   ├── components/                    # UI Components tái sử dụng (Button, Modal, Input,...)
│   │   ├── services/                      # API Client & Axios instances
│   │   ├── hooks/                         # Custom React Hooks
│   │   └── lib/                           # Utilities & Config helpers
│   └── package.json
│
├── infra/                                 # Cấu hình Hạ tầng & Triển khai
│   ├── compose.yaml                       # Docker Compose cho Database, Cache, Proxy, Monitoring
│   ├── docker/                            # Dockerfiles cho các dịch vụ
│   ├── nginx/                             # Reverse Proxy & Load Balancer configuration
│   └── monitoring/                        # Cấu hình Prometheus, Grafana dashboards
│
├── docs/                                  # Tài liệu dự án
│   ├── architecture/                      # Sơ đồ kiến trúc & luồng dữ liệu
│   ├── api/                               # OpenAPI / Swagger specs
│   ├── database/                          # ERD & Database migrations scripts
│   └── decisions/                         # Architecture Decision Records (ADRs)
│
├── .github/                               # CI/CD Workflows & Quản lý repository
│   ├── workflows/                         # GitHub Actions pipelines
│   └── CODEOWNERS                         # Phân quyền phụ trách từng phần codebase
│
├── scripts/                               # Tiện ích phát triển & tự động hoá
├── .env.example                           # Mẫu biến môi trường
└── README.md                              # Hướng dẫn tổng quan dự án
```

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

## 🚀 Hướng dẫn Khởi chạy (Getting Started)

### 1. Chuẩn bị Môi trường
- **Java**: OpenJDK 21+
- **Maven**: 3.9+
- **Node.js**: 20.x+ & npm / pnpm
- **Docker & Docker Compose**: Bản mới nhất

### 2. Thiết lập Biến môi trường
Sao chép tệp mẫu cấu hình:
```bash
cp .env.example .env
```

### 3. Khởi động Hạ tầng (Infrastructures)
Chạy PostgreSQL, Redis, RabbitMQ, Prometheus, Grafana qua Docker:
```bash
cd infra
docker compose up -d
```

### 4. Chạy Backend Services
Di chuyển vào thư mục `server` và build toàn bộ:
```bash
cd server
mvn clean install
```
Chạy `api-gateway` và các microservices bạn cần phát triển (ví dụ `identity-service`):
```bash
cd api-gateway
mvn spring-boot:run
```

### 5. Chạy Frontend Web
Cài đặt thư viện và khởi động dev server:
```bash
cd web
npm install
npm run dev
```
Truy cập ứng dụng tại: `http://localhost:3000`
