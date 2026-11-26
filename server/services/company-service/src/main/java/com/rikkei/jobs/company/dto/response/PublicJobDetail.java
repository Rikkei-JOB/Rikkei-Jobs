package com.rikkei.jobs.company.dto.response;

import com.rikkei.jobs.company.entity.JobLevel;
import com.rikkei.jobs.company.entity.JobType;
import com.rikkei.jobs.company.entity.WorkMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** GET /public/jobs/{id}: nội dung JD ứng viên được xem; không có trạng thái duyệt, lý do trả lại hay người tạo. */
public record PublicJobDetail(
        UUID id,
        String title,
        String position,
        JobLevel level,
        JobType type,
        Integer quantity,
        String city,
        String address,
        WorkMode workMode,
        Long salaryMin,
        Long salaryMax,
        String description,
        String requirements,
        String benefits,
        LocalDate deadline,
        Instant createdAt,
        List<JobSkillResponse> skills,
        PublicCompanySummary company) {
}
