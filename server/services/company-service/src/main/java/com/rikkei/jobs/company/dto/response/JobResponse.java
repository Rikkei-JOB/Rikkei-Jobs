package com.rikkei.jobs.company.dto.response;

import com.rikkei.jobs.company.entity.JobLevel;
import com.rikkei.jobs.company.entity.JobStatus;
import com.rikkei.jobs.company.entity.JobType;
import com.rikkei.jobs.company.entity.WorkMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Chi tiết JD: mọi cột của jobs trừ created_by/version. rejectReason chỉ khác null khi REJECTED;
 * skills sắp bắt buộc trước rồi minScore giảm dần.
 */
public record JobResponse(
        UUID id,
        UUID companyId,
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
        JobStatus status,
        String rejectReason,
        Instant reviewedAt,
        Instant createdAt,
        Instant updatedAt,
        List<JobSkillResponse> skills) {
}
