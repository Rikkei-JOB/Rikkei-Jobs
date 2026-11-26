package com.rikkei.jobs.company.dto.response;

import com.rikkei.jobs.company.entity.JobLevel;
import com.rikkei.jobs.company.entity.JobType;
import com.rikkei.jobs.company.entity.WorkMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Một dòng trong GET /public/jobs. skills như JobResponse: chỉ id, frontend ghép tên với GET /skills. */
public record PublicJobItem(
        UUID id,
        String title,
        JobLevel level,
        JobType type,
        WorkMode workMode,
        String city,
        Long salaryMin,
        Long salaryMax,
        LocalDate deadline,
        Instant createdAt,
        List<JobSkillResponse> skills,
        PublicCompanySummary company) {
}
