# HS-04 — CV dựng từ hồ sơ, xuất PDF mẫu Rikkei có QR

Ứng viên tạo tối đa 5 CV theo vị trí. CV chỉ lưu cấu hình (bật phần nào, chọn mục nào, thứ tự). Nội dung luôn đọc từ hồ sơ lúc xem hoặc xuất, nên sửa hồ sơ thì CV đổi theo. Riêng đơn ứng tuyển (UT-01) giữ snapshot PDF tại lúc nộp.

Mọi đường dẫn dưới đây đi qua gateway với tiền tố `/api/v1`. Đường `/profile/cvs` giữ đúng hợp đồng UT-01, không dùng `/cvs` như mô tả task vì gateway chỉ định tuyến `/api/v1/profile/**` tới profile-service.

## API

| Method | Path | Quyền | Mô tả |
| --- | --- | --- | --- |
| GET | `/profile/cvs` | CANDIDATE | Mảng CV của mình `{id, name, targetPosition, template, publicUrl, createdAt, updatedAt}`; `Cache-Control: private, no-store` |
| POST | `/profile/cvs` | CANDIDATE | Body `{name, targetPosition?}` → 201 chi tiết CV. CV thứ 6 → 409 `CV_LIMIT_REACHED` |
| GET | `/profile/cvs/{id}` | CANDIDATE | Chi tiết CV kèm mọi mục của hồ sơ theo từng phần, đánh dấu `selected` |
| PUT | `/profile/cvs/{id}` | CANDIDATE | Đổi tên, vị trí |
| DELETE | `/profile/cvs/{id}` | CANDIDATE | 204. Đơn đã nộp vẫn giữ snapshot PDF |
| PUT | `/profile/cvs/{id}/sections` | CANDIDATE | Body `{sections: [{section, included, itemIds}]}`, đủ 7 phần, mỗi phần một lần; thứ tự trong mảng là thứ tự trên CV |
| GET | `/profile/cvs/{id}/pdf` | CANDIDATE, chủ CV | `application/pdf`, `attachment; filename="cv-{id}.pdf"`. `?inline=true` cho khung xem trước. Chưa có hồ sơ cá nhân → 409 `PROFILE_REQUIRED` |
| POST | `/profile/cvs/{id}/preview` | CANDIDATE, chủ CV | Body như `PUT .../sections`; dựng PDF `inline` theo bản nháp chưa lưu để khung xem trước cập nhật ngay, không ghi DB. Lỗi giống `PUT .../sections` và `GET .../pdf` |
| GET | `/profile/public/{token}` | Công khai | Trang QR dẫn tới. Token sai → 404. Hồ sơ không PUBLIC → 200 `{visible: false}` |

CV của người khác trả 404, không lộ sự tồn tại. recruitment-service gọi thẳng `GET /profile/cvs/{id}/pdf` trên profile-service, chuyển tiếp JWT ứng viên.

### Phần và mục

`section`: `SUMMARY`, `SKILLS`, `LANGUAGES`, `INTERNSHIP`, `PROJECTS`, `EDUCATION`, `CERTIFICATES` (thứ tự mặc định khi tạo).

| Phần | Nguồn | `itemIds` là |
| --- | --- | --- |
| SUMMARY | `candidate_profiles.summary`, thiếu thì `headline` | luôn `null` |
| SKILLS | điểm kỹ năng HS-01 có điểm xác thực hoặc tự khai | id kỹ năng |
| LANGUAGES | ngoại ngữ HS-02 (`/profiles/me/languages`), trình độ cao trước; PDF in tên, `CEFR <bậc>` và ghi chú | id ngoại ngữ |
| INTERNSHIP | dự án portfolio nguồn `IOC` | id dự án |
| PROJECTS | dự án portfolio nguồn `PERSONAL` | id dự án |
| EDUCATION | học vấn HS-02 | id học vấn |
| CERTIFICATES | chứng chỉ HS-02 | id chứng chỉ |

`itemIds = null` nghĩa là lấy mọi mục, kể cả mục thêm vào hồ sơ sau này. Mảng thì chỉ lấy các mục có trong mảng (tối đa 150). PDF bỏ qua phần tắt và phần không có mục nào.

Kỹ năng trong CV ghi rõ "Đã xác thực · điểm · mức" hoặc "Tự khai · điểm".

### Trang công khai `/p/{token}`

Mỗi CV có `public_token` ngẫu nhiên riêng (24 byte, base64url). QR trong PDF trỏ tới `${WEB_BASE_URL}/p/{token}`. API chỉ trả họ tên, headline, chương trình, khoá học, kỹ năng có điểm xác thực và kỳ thực tập IOC đang hiển thị. Không trả email, điện thoại, địa chỉ, ngày sinh. Hồ sơ phải PUBLIC theo `public_candidate_ids` (HS-05); không thì trang báo "Hồ sơ không công khai".

## DB (V13)

- `cvs(id, user_id, name, target_position, template, public_token unique, created_at, updated_at)`; `template` hiện chỉ có `RIKKEI`.
- `cv_sections(cv_id, section, item_ids, sort_order, included)`, khoá chính `(cv_id, section)`, xoá theo CV. `item_ids` là mảng JSON lưu trong `VARCHAR(8000)` để chạy được cả PostgreSQL và H2 khi test.

## PDF

Render ở server bằng openhtmltopdf (PDFBox), QR bằng ZXing. Font Liberation Sans thường/đậm lấy từ `openpdf-fonts-extra`, có đủ dấu tiếng Việt.

Bố cục hai cột theo ngôn ngữ thiết kế dự án:

- Cột trái nền navy `#18233A`, lặp lại trên mọi trang: chữ "RIKKEI JOBS", ảnh đại diện cắt tròn (không có ảnh thì hiện chữ viết tắt), họ tên, vị trí, liên hệ, QR, rồi các phần Kỹ năng, Học vấn, Ngoại ngữ.
- Cột phải: tiêu đề phần dạng nhãn đỏ `#B4232D` kèm đường kẻ, chứa Giới thiệu, Thực tập, Dự án, Chứng chỉ.
- Thứ tự phần trong mỗi cột theo thứ tự ứng viên chọn.
- QR luôn nằm ở trang 1, ngay dưới liên hệ.
- Chưa có logo ảnh vì repo chưa có tệp logo.

## Cấu hình

| Biến | Mặc định | Ý nghĩa |
| --- | --- | --- |
| `WEB_BASE_URL` | `http://localhost:3000` | Gốc đường dẫn QR `/p/{token}` |

## Kiểm tra

`mvn -pl services/profile-service test -Dtest=CvApiTest` (trong `server/`): giới hạn 5 CV, phần mặc định, chọn mục, PDF có tiếng Việt và nhãn xác thực, quyền chủ sở hữu, trang công khai không lộ liên hệ.
