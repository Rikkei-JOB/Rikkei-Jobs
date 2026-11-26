# Điểm khớp ứng viên với JD theo kỹ năng

Tài liệu này là **nguồn duy nhất** cho công thức tính điểm khớp (DN-06). Code ở `company-service` (`util/MatchCalculator.java`) và mọi nơi hiển thị phải khớp với tài liệu này.

> **Muốn đổi công thức phải thống nhất cả nhóm và sửa file này trước**, rồi mới sửa `MatchCalculator`, `MatchCalculatorTest`, `docs/api/DN-06.http` và các ví dụ bên dưới.

## 1. Công thức

Với mỗi kỹ năng của JD (bảng `job_skills`: `required` = bắt buộc hoặc ưu tiên, `min_score` từ 1 đến 100):

- `v` = `verified_score` của ứng viên cho kỹ năng đó (profile-service, 0–100). Không có thì `v = 0`. **`self_score` (điểm tự khai) không bao giờ được dùng.**
- Điểm kỹ năng `s = min(v / min_score, 1)`.
- `R` = trung bình `s` của các kỹ năng **bắt buộc**; `P` = trung bình `s` của các kỹ năng **ưu tiên**.
- Điểm khớp:

| Trường hợp | Công thức |
| --- | --- |
| Có cả bắt buộc và ưu tiên | `70 × R + 30 × P` |
| Chỉ có bắt buộc | `100 × R` |
| Chỉ có ưu tiên | `100 × P` |
| JD không có kỹ năng nào | `0`; `met` và `missing` rỗng |

- `belowMinimum = true` khi có ít nhất một kỹ năng **bắt buộc** với `v < 0,5 × min_score` (`v = 0,5 × min_score` đúng biên thì **không** bật).
- Kỹ năng **đạt** (`met`): `v ≥ min_score` (tức `s = 1`). Kỹ năng **thiếu** (`missing`): `v < min_score`; kỹ năng thiếu vẫn được tính điểm một phần qua `s`.
- API trả `score` là **số nguyên**, làm tròn 0,5 lên (`HALF_UP`). Các phép tính trung gian (`s`, `R`, `P`) tính với 10 chữ số thập phân, chỉ làm tròn số nguyên ở bước cuối.
- Thứ tự trong `met` / `missing`: bắt buộc trước, ưu tiên sau, cùng nhóm theo tên.

## 2. Dữ liệu vào

| Dữ liệu | Nguồn | Ghi chú |
| --- | --- | --- |
| Kỹ năng của JD: `skillId`, `required`, `min_score` | `job_skills` (company-service, DN-02) | Đổi kỹ năng hoặc `min_score` thì điểm đổi ngay ở lần gọi sau |
| `verified_score` của ứng viên | `skill_scores` (profile-service, HS-01), qua `GET /internal/profiles/{userId}/verified-skills` | Chỉ các dòng có `verified_score`; `self_score` không có đường vào |
| Tên kỹ năng | danh mục `GET /skills` (profile-service) | Hiển thị ở trường `skill`; không còn trong danh mục thì dùng chính `skillId` |

## 3. Dữ liệu mẫu

Kỹ năng (UUID cố định của profile-service): Java `…0001-000000000001`, Spring Boot `…02`, SQL `…03`, Git `…04`, Docker `…11`, Giao tiếp `…18`.

`verified_score` của ba ứng viên mẫu, tính tay từ `profile-service/src/main/resources/hs01/candidates.json` theo trọng số `skill_mappings`:

| Ứng viên | Java | Spring Boot | SQL | Git | Docker | Giao tiếp |
| --- | --- | --- | --- | --- | --- | --- |
| C1 `lms-sv-001` | 82,058824 | 80,454545 | 81,153846 | 90 | — | chỉ tự khai 65 |
| C2 `lms-sv-002` | 65 | — | 65 | 80 | — | — |
| C3 `lms-sv-003` | 49,117647 | — | 58,846154 | — | — | — |

Hai JD mẫu (Công ty A, `APPROVED`, seed bởi `DevDataSeeder` của company-service khi chạy profile `dev`):

- **JD6** `00000000-0000-0000-0003-000000000006` "Backend Developer Java (mẫu DN-06)": bắt buộc Java 80, Spring Boot 70, SQL 90; ưu tiên Docker 50, Giao tiếp 60, Git 60.
- **JD7** `00000000-0000-0000-0003-000000000007` "Fresher Java (mẫu DN-06)": chỉ bắt buộc Java 60, SQL 60, Git 60.

## 4. Ba ví dụ tính tay

Mỗi ví dụ có test cho đúng con số này (`MatchCalculatorTest.example1…3`, `JobMatchApiTest`) và một request trong [DN-06.http](api/DN-06.http).

### Ví dụ 1: JD6 × C1, đạt một phần → `score 78`, `belowMinimum false`

| Kỹ năng | Loại | `v` | `min_score` | `s = min(v / min_score, 1)` |
| --- | --- | --- | --- | --- |
| Java | bắt buộc | 82,058824 | 80 | 1 |
| Spring Boot | bắt buộc | 80,454545 | 70 | 1 |
| SQL | bắt buộc | 81,153846 | 90 | 0,901709 |
| Docker | ưu tiên | 0 (không có) | 50 | 0 |
| Giao tiếp | ưu tiên | 0 (chỉ tự khai 65, không tính) | 60 | 0 |
| Git | ưu tiên | 90 | 60 | 1 |

- `R = (1 + 1 + 0,901709) / 3 = 0,967236`
- `P = (0 + 0 + 1) / 3 = 0,333333`
- `70 × 0,967236 + 30 × 0,333333 = 67,7065 + 10 = 77,7065` → làm tròn **78**.
- `belowMinimum = false`: kỹ năng bắt buộc thấp nhất là SQL 81,15 ≥ 0,5 × 90 = 45.
- `met`: Java, Spring Boot, Git. `missing`: SQL (81,153846 / 90), Docker (0 / 50), Giao tiếp (0 / 60).
- **Điểm tự khai 65 của Giao tiếp không được tính.** Nếu tính, `s` của Giao tiếp = min(65 / 60, 1) = 1, `P = 2/3`, điểm = 67,7065 + 20 = 87,7 → **88**. Con số 78 chứng minh tự khai không cộng điểm.

### Ví dụ 2: JD7 × C3, có kỹ năng dưới 50% → `score 60`, `belowMinimum true`

| Kỹ năng | Loại | `v` | `min_score` | `s` |
| --- | --- | --- | --- | --- |
| Java | bắt buộc | 49,117647 | 60 | 0,818627 |
| SQL | bắt buộc | 58,846154 | 60 | 0,980769 |
| Git | bắt buộc | 0 (không có) | 60 | 0 |

- JD chỉ có bắt buộc nên điểm = `100 × R`; `R = (0,818627 + 0,980769 + 0) / 3 = 0,599799`.
- `100 × 0,599799 = 59,98` → làm tròn **60**.
- `belowMinimum = true` vì Git `v = 0 < 0,5 × 60 = 30`.
- `met` rỗng (không kỹ năng nào đạt đủ `min_score`); `missing`: Git, Java, SQL.

### Ví dụ 3: JD6 × ứng viên chưa có kỹ năng nào → `score 0`, `belowMinimum true`

- Mọi `v = 0` nên mọi `s = 0`, `R = 0`, `P = 0`, điểm = 0.
- `belowMinimum = true` (ba kỹ năng bắt buộc đều có `v = 0 < 0,5 × min_score`).
- `met` rỗng; `missing` đủ 6 kỹ năng theo thứ tự Java, Spring Boot, SQL (bắt buộc), rồi Docker, Git, Giao tiếp (ưu tiên), mỗi kỹ năng `candidateScore = 0`.
- Ứng viên chỉ có điểm tự khai cũng ra kết quả như vậy (xem ví dụ 1: Giao tiếp).

## 5. Các quy tắc khác đã có test

| Quy tắc | Ví dụ | Test |
| --- | --- | --- |
| JD chỉ bắt buộc: `100 × R` | Java 80, `v = 40` → 50 | `MatchCalculatorTest.requiredOnlyUses100R` |
| JD chỉ ưu tiên: `100 × P` | Docker 50, `v = 25` → 50, `belowMinimum false` | `preferredOnlyUses100P` |
| JD rỗng | → 0, `met`/`missing` rỗng | `emptyJobIsZero` |
| `v` lớn hơn `min_score` bị chặn ở 1 | Java 40, `v = 100` → 100 | `scoreAboveMinIsCappedAtOne` |
| `v = min_score` tính là đạt, kể cả khi khác số chữ số thập phân | `70` và `70.000000` | `scoreEqualToMinIsMet` |
| Biên `belowMinimum` | `min_score 60`: `v = 30` không bật, `v = 29,999999` bật | `belowMinimumBoundary` |
| Làm tròn 0,5 lên | `s = 1` và `0,51` → 75,5 → 76 | `roundsHalfUp` |
| Điểm của kỹ năng không thuộc JD bị bỏ qua | thêm Docker 100 vào C3 vẫn ra 60 | `ignoresScoresOfSkillsNotInJob` |
