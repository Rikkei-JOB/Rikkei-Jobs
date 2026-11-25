package com.rikkei.jobs.company.service;

import com.rikkei.jobs.common.dto.response.PageResponse;
import com.rikkei.jobs.company.dto.request.JobRequest;
import com.rikkei.jobs.company.dto.request.JobSkillRequest;
import com.rikkei.jobs.company.dto.response.JobResponse;
import com.rikkei.jobs.company.dto.response.JobStatusSummary;
import com.rikkei.jobs.company.dto.response.JobSummary;
import com.rikkei.jobs.company.entity.JobStatus;
import java.util.List;
import java.util.UUID;

/**
 * JD của HR (DN-02). Công ty luôn lấy từ company_hr theo userId; JD của công ty khác trả 404 như id không tồn tại.
 * Ghi khi công ty LOCKED → COMPANY_LOCKED; sửa JD PENDING/CLOSED → JOB_NOT_EDITABLE; sửa JD APPROVED → PENDING.
 */
public interface JobService {

    /** Tạo JD nháp (DRAFT) của công ty HR; {@code authorization} chuyển tiếp sang profile-service khi có skills. */
    JobResponse create(UUID hrUserId, JobRequest req, String authorization);

    /** Thay toàn bộ thông tin chung; {@code skills} khác null thì thay luôn kỹ năng trong cùng transaction. */
    JobResponse update(UUID hrUserId, UUID jobId, JobRequest req, String authorization);

    /** Thay toàn bộ danh sách kỹ năng (mảng rỗng = xoá hết). */
    JobResponse replaceSkills(UUID hrUserId, UUID jobId, List<JobSkillRequest> skills, String authorization);

    /** Gửi duyệt: chỉ từ DRAFT/REJECTED (xoá rejectReason), qua kiểm tra gửi duyệt; sai trạng thái → INVALID_JOB_STATUS. */
    JobResponse submit(UUID hrUserId, UUID jobId);

    /** Đóng JD: chỉ từ APPROVED; sai trạng thái → INVALID_JOB_STATUS. */
    JobResponse close(UUID hrUserId, UUID jobId);

    JobResponse get(UUID hrUserId, UUID jobId);

    /** page từ 1, size 1–100; {@code status} null = mọi trạng thái; mới cập nhật trước. */
    PageResponse<JobSummary> listMine(UUID hrUserId, JobStatus status, int page, int size);

    /** Số JD theo từng trạng thái (đủ 5 khoá) và số JD APPROVED hết hạn trong 7 ngày; công ty LOCKED vẫn xem được. */
    JobStatusSummary summaryMine(UUID hrUserId);
}
