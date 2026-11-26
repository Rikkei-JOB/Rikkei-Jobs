package com.rikkei.jobs.company.service;

import com.rikkei.jobs.common.dto.response.PageResponse;
import com.rikkei.jobs.company.dto.response.StaffJobDetail;
import com.rikkei.jobs.company.dto.response.StaffJobItem;
import com.rikkei.jobs.company.entity.JobStatus;
import java.util.UUID;

/** Cán bộ xử lý JD (DN-07 §4.4). Mọi thao tác ghi kèm một dòng company_actions trong cùng transaction. */
public interface StaffJobReviewService {

    /**
     * {@code status} null = mọi trạng thái; {@code companyId} null = mọi công ty. PENDING: gửi sớm nhất trước,
     * còn lại: mới cập nhật trước. Page từ 1, size 1–100.
     */
    PageResponse<StaffJobItem> list(JobStatus status, UUID companyId, int page, int size);

    StaffJobDetail detail(UUID jobId);

    /** PENDING → APPROVED; hạn nộp đã qua → JOB_EXPIRED. */
    StaffJobDetail approve(UUID staffId, UUID jobId);

    /** PENDING → REJECTED, lưu lý do (đã trim). */
    StaffJobDetail reject(UUID staffId, UUID jobId, String reason);

    /** APPROVED → CLOSED, lý do chỉ ghi vào lịch sử. */
    StaffJobDetail close(UUID staffId, UUID jobId, String reason);
}
