package com.rikkei.jobs.company.dto.response;

import com.rikkei.jobs.company.entity.CompanyStatus;
import com.rikkei.jobs.company.entity.JobLevel;
import com.rikkei.jobs.company.entity.JobStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

/** DN-07: cán bộ duyệt / trả lại JD. */
public final class StaffJobDtos {
    private StaffJobDtos() {
    }

    /** Một dòng trong GET /staff/jobs. submittedAt = updatedAt (thời điểm HR gửi duyệt hoặc sửa lần cuối). */
    public record StaffJobItem(UUID id, String title, JobLevel level, JobStatus status, LocalDate deadline,
                               Instant submittedAt, Instant reviewedAt, int requiredSkillCount, String rejectReason,
                               UUID companyId, String companyName, CompanyStatus companyStatus) {
    }

    /** GET /staff/jobs/summary: số JD theo trạng thái (không tính DRAFT), đủ 4 khoá. */
    public record StaffJobSummary(long total, Map<JobStatus, Long> byStatus) {
    }

    /** GET /staff/jobs/{id}: nội dung JD như HR thấy, kèm công ty. */
    public record StaffJobDetail(JobResponse job, UUID reviewedBy, StaffJobCompany company) {
    }

    public record StaffJobCompany(UUID id, String name, CompanyStatus status, String industry, String size,
                                  String city, String logoUrl) {
    }
}
