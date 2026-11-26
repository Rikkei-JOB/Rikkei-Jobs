package com.rikkei.jobs.company.dto.response;

import com.rikkei.jobs.company.entity.JobStatus;
import java.time.Instant;

/**
 * Chi tiết JD cho cán bộ (mọi trạng thái, mọi công ty): nội dung như trang công khai cộng thông tin duyệt.
 * {@code reviewedBy} null khi JD chưa được xử lý lần nào.
 */
public record StaffJobDetail(PublicJobDetail job, JobStatus status, String rejectReason, Instant reviewedAt,
                             StaffRef reviewedBy, boolean companyLocked) {
}
