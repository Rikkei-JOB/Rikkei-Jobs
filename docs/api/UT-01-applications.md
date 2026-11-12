# UT-01 — Ứng tuyển bằng CV và theo dõi trạng thái

## Endpoint qua gateway

Các endpoint ứng viên yêu cầu JWT role CANDIDATE. Controller recruitment giữ prefix `/recruitment`; gateway bỏ `/api/v1`.

| Method | URL dưới `/api/v1/recruitment` | Kết quả |
| --- | --- | --- |
| POST | `/jobs/{jobId}/apply` | 201 detail; body `{cvId, coverLetter?}` |
| GET | `/me/applications?status=&page=1&size=20` | 200 `{items,total,page,size}` |
| GET | `/me/applications/{id}` | 200 detail kèm lịch sử |
| POST | `/me/applications/{id}/withdraw` | 200 detail; body `{reason}` |
| GET | `/applications/{id}/cv` | PDF riêng tư; chủ đơn hoặc HR cùng companyId |

Thư giới thiệu tối đa 5.000 ký tự, lý do rút bắt buộc sau trim và tối đa 1.000. Thời gian UTC ISO 8601; FE hiển thị giờ Việt Nam. Phân trang từ 1, size 1–100, mặc định 20. Status không hợp lệ trả 400.

Luật: SUBMITTED → SCREENING → INTERVIEW → OFFER → HIRED; REJECTED từ mọi bước trước HIRED; WITHDRAWN chỉ từ 3 bước đầu. Không mở lại bản ghi terminal. Được nộp lại cùng JD sau REJECTED/WITHDRAWN bằng bản ghi và PDF mới. Các trạng thái khác chặn đơn mới với 409, kể cả request đồng thời.

Lỗi giữ `{code,message}`: 400 validation; 401 token; 403 quyền; 404 tài nguyên; 409 trạng thái/trùng đơn; 502 dữ liệu dependency sai; 503 dependency/storage không sẵn sàng. JD được kiểm tra APPROVED, `deadline > now`, company không khoá trước và sau khi xuất PDF; deadline kiểm lại trong transaction ghi đơn. Thay đổi trạng thái JD ngay sau lần kiểm cuối không có transaction nguyên tử xuyên service.

## Hợp đồng consumer cho các task phụ thuộc

**HS-04 và DN-04 chưa có trong checkout tại lúc triển khai.** Các URL dưới đây là default đề xuất, cấu hình được; owner cần đối chiếu khi tích hợp. Không có production fixture fallback hoặc truy cập bảng của service khác.

- Company: GET `${COMPANY_SERVICE_URL}${RECRUITMENT_JOB_CONTEXT_PATH}`, default path `/company/internal/jobs/{jobId}/application-context`, header `X-Service-Key: ${RECRUITMENT_COMPANY_SERVICE_KEY}`. Chỉ endpoint nội bộ được xác thực mới được trả `hrEmails`; không public endpoint qua gateway và không ghi job_views.
- Body: `{jobId, companyId, jobTitle, companyName, status, deadline, companyLocked, hrEmails: ["hr@company.test"]}`. IDs UUID; deadline ISO 8601 có offset; HR phải thuộc công ty và còn hoạt động. Thiếu hoặc sai dữ liệu phải bị từ chối, không coi là APPROVED.
- Profile: GET `${PROFILE_SERVICE_URL}${RECRUITMENT_CV_PDF_PATH}`, default `/profile/cvs/{cvId}/pdf`, chuyển tiếp JWT ứng viên. HS-04 phải xác minh CV ownership; response `application/pdf`, bytes PDF hoàn chỉnh. Không dùng public CV URL.
- FE: list CV từ `NEXT_PUBLIC_CV_API_PATH`, default `/profile/cvs`, trả mảng hoặc PageResponse có item `{id,name,targetPosition}`. `NEXT_PUBLIC_CV_PAGE_PATH` default `/candidate/cvs`; `NEXT_PUBLIC_JOB_PAGE_PATH` default `/jobs`. Chỉ dùng các route thật khi HS-04/DN-04 cung cấp.
- DN-04 import `ApplyButton` từ `@/app/components/applications/apply-button`, truyền `jobId` và `acceptingApplications`; không tự dựng lại trang JD trong UT-01.
- UT-02 gọi cùng `ApplicationTransitionService` và `ApplicationStatusPolicy`, kiểm quyền HR trước khi gọi. Không update status trực tiếp; transition service khoá hàng và ghi history trong transaction.

## PDF và lịch sử

`cvSnapshotUrl` là URL tương đối qua gateway đến endpoint PDF được bảo vệ. File đặt trong `RECRUITMENT_CV_STORAGE_DIR`, không static/public. Sửa/xoá CV không ảnh hưởng file đã nộp. QR vẫn trỏ trang PUBLIC hiện tại của HS-04, không snapshot quyền công khai. Danh sách đơn dùng metadata công ty/JD tại lúc nộp, không cần gọi endpoint công khai DN-04.

Ứng viên thấy đủ transition; note chỉ hiện với WITHDRAWN do chính ứng viên thực hiện. Ghi chú HR giữ nội bộ.

## Kiểm thử và vận hành

Test PostgreSQL dùng Testcontainers mặc định; có thể dùng DB kiểm thử riêng qua `UT01_TEST_DB_URL`, `UT01_TEST_DB_USER`, `UT01_TEST_DB_PASSWORD`. **Không trỏ vào database sản xuất hoặc DB có dữ liệu cần giữ**, vì test xoá các bảng recruitment giữa các case. Cách chạy và kết quả kiểm chứng được cập nhật khi hoàn thành triển khai.
