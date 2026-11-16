# HS-02 — Giai đoạn 2: Học vấn, chứng chỉ và tệp

Trang `/candidate/profile` bổ sung học vấn, chứng chỉ và avatar, giữ nguyên thông
tin cá nhân giai đoạn 1 và Skill Profile HS-01. Kỹ năng tự khai thuộc giai đoạn 3.

## API qua gateway

Các URL dưới đây nằm sau `/api/v1`, yêu cầu JWT CANDIDATE. Không nhận userId từ
body hay query; luôn lấy chủ sở hữu từ JWT. ID không tồn tại và ID của ứng viên
khác cùng trả 404.

| Method | URL | Kết quả |
| --- | --- | --- |
| GET | `/profiles/me/educations` | Mảng học vấn |
| POST | `/profiles/me/educations` | 201 bản ghi mới |
| PUT / DELETE | `/profiles/me/educations/{id}` | 200 bản ghi sau sửa / 204 |
| GET | `/profiles/me/certificates` | Mảng chứng chỉ |
| POST | `/profiles/me/certificates` | 201 bản ghi mới |
| PUT / DELETE | `/profiles/me/certificates/{id}` | 200 bản ghi sau sửa / 204 |
| POST | `/files` | Multipart field `file`; 201 `{url}` |
| GET | `/files/{id}` | Nội dung nhị phân, chỉ chủ file được tải |
| PUT | `/profiles/me/avatar` | `{fileUrl}`; trả hồ sơ cá nhân sau cập nhật |

Ví dụ học vấn:

```json
{
  "school": "Đại học A", "major": "Công nghệ thông tin",
  "fromDate": "2022-09-01", "toDate": null,
  "gpa": 3.5, "note": "Đang theo học"
}
```

Trường học và ngày bắt đầu bắt buộc. Chuyên ngành có thể bỏ trống; trường học và
chuyên ngành tối đa 255 ký tự, ghi chú tối đa 5000. Ngày kết thúc không được trước
ngày bắt đầu. GPA không bắt buộc; giai đoạn này chấp nhận số 0–10, không tự đổi
điểm hệ 4 sang hệ 10. Đây là quy ước triển khai vì task chưa chỉ định hệ điểm.

Ví dụ chứng chỉ sau khi upload:

```json
{
  "name": "Chứng chỉ Java", "issuer": "Đơn vị cấp",
  "issuedAt": "2026-01-01", "expiresAt": null,
  "fileUrl": "/api/v1/files/00000000-0000-0000-0000-000000000001"
}
```

Tên, đơn vị cấp (tối đa 255 ký tự) và ngày cấp bắt buộc. Ngày hết hạn không được
trước ngày cấp. File đính kèm không bắt buộc; nếu có, phải là file đã upload của
chính ứng viên. Server suy ra `fileType` từ nội dung upload, bỏ qua fileType client
gửi lên. PUT thay thế các trường; `fileUrl:null` bỏ đính kèm. Chuỗi tuỳ chọn rỗng
được chuẩn hoá thành null.

## Tệp và avatar

Chỉ nhận PDF, JPG, PNG tối đa **5 MiB = 5.242.880 byte**, kiểm tra chữ ký nội dung,
không tin đuôi hay MIME do trình duyệt gửi. Tệp rỗng/EXE đổi đuôi PDF bị từ chối
400; vượt dung lượng trả 413 `FILE_TOO_LARGE`. Lỗi lưu/đọc ổ đĩa trả 503
`FILE_STORAGE_UNAVAILABLE`. Kiểm tra chữ ký không phải quét virus hoặc phân tích
toàn bộ cấu trúc tài liệu.

URL upload trả về dạng `/api/v1/files/{uuid}`, yêu cầu Bearer JWT khi tải. Không
có static/public route cho file. Response có Content-Type do server xác định,
Content-Disposition attachment, X-Content-Type-Options nosniff và Cache-Control
private, no-store. Frontend tải binary qua API client rồi tạo blob URL để xem ảnh
hoặc tải file; không gắn token vào URL.

Avatar chỉ nhận JPG/PNG đã upload của chủ hồ sơ, PDF trả 400. Gửi
`{"fileUrl":null}` để bỏ avatar. API PUT thông tin cá nhân của giai đoạn 1 vẫn
không cho sửa avatarUrl trực tiếp. Thay/bỏ avatar hoặc xoá chứng chỉ không xoá
file vật lý để tránh làm hỏng các tham chiếu khác; giai đoạn này chưa có API xoá
file hay job thu gom file chưa được gắn. Cần theo dõi dung lượng lưu trữ khi vận hành.

## Migration và cấu hình

- V6 tạo `profile.uploaded_files`, lưu owner, MIME, size và thời điểm upload.
- V7 tạo `profile.educations`, `profile.certificates`; không sửa migration cũ.
- `PROFILE_FILES_DIR` mặc định `.runtime/profile-files` khi chạy Java, tính từ
  working directory. Docker đặt `/app/data/profile-files`, dùng named volume
  `profile_files`, writable cho uid 10001. Giữ volume khi dừng stack.
- Multipart giới hạn từng file 5MB, toàn request 6MB để chừa phần header; Nginx
  `/api/` cho body tới 6m. Nếu dùng proxy riêng, phải cấu hình giới hạn tương ứng.
- Backup database và volume file cùng nhau. Không dùng `docker compose down -v`
  nếu muốn giữ dữ liệu.

OpenAPI: [hs02-openapi.yaml](hs02-openapi.yaml), Swagger UI `/api/docs` nhóm
profile-service. Chạy lại backend sau khi thay code:

```powershell
docker compose up -d --build profile-service api-gateway
```

Frontend dùng cấu hình và cách chạy giai đoạn 1. Khi mở trang hồ sơ có thể tự thêm
học vấn, chọn tệp chứng chỉ và thay avatar; không cần dữ liệu demo riêng cho giai
đoạn 2.

## Kiểm thử

```powershell
mvn -f server/pom.xml -pl services/profile-service,api-gateway -am test
npm --prefix web run type-check
npm --prefix web run lint
# Trong web/:
$env:UT01_BROWSER_CHANNEL='msedge'
npm run test:e2e
```

Các test tự động dùng database kiểm thử và API doubles cho trình duyệt, không thay
đổi hồ sơ thực tế đang chạy.

### Kết quả xác minh 2026-11-13

- Hồi quy profile-service, gateway và common-core: 133 tests, 0 failures/errors,
  1 test read consistency có sẵn được skip. Profile 107, gateway 12, common 14.
- 15 test CRUD/upload/avatar chạy lại trên PostgreSQL 18, database riêng:
  `hs02_phase2_verify_20261113`, tất cả qua. Hỗ trợ các biến
  `HS02_PHASE2_TEST_DB_URL`, `HS02_PHASE2_TEST_DB_USER`,
  `HS02_PHASE2_TEST_DB_PASSWORD`; không trỏ vào database ứng dụng cần giữ.
- Test HTTP thật xác minh multipart 6MB trả JSON 413 FILE_TOO_LARGE (không trả
  401 hay reset kết nối). Tomcat drain tối đa 8MB cho request bị từ chối.
- Type-check, lint và production build frontend qua; lint có 1 warning cũ ở
  commitlint.config.mjs. Playwright Edge 17/17 qua (4 giai đoạn 2, 4 giai đoạn 1,
  9 UT-01), dùng workers=2 để ổn định thời gian chạy trên máy local.
- Đã xem ảnh desktop 1440px và mobile 390px, không tràn ngang; ảnh local
  `web/test-results/hs02-phase2-desktop.png` và `hs02-phase2-mobile.png`.
- Review độc lập phát hiện lỗi response upload quá cỡ; đã sửa và có test
  RED→GREEN qua HTTP thật. Chưa thao tác thêm/sửa/xoá trên hồ sơ ứng viên thực tế;
  người dùng tự nghiệm thu trên ứng dụng đang chạy.
- Đã build lại profile-service/gateway và xác nhận healthy. PostgreSQL 16 trong
  Docker đã áp dụng V6/V7 thành công từ schema profile version 5; giữ nguyên các
  volume cũ, bổ sung volume profile_files. Frontend local tại cổng 3000 đã trả 200.
