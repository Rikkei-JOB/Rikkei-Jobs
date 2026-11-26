package com.rikkei.jobs.company.dto.response;

import com.rikkei.jobs.company.entity.JobLevel;
import com.rikkei.jobs.company.entity.JobType;
import com.rikkei.jobs.company.entity.WorkMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Một dòng của trang việc làm (GET /jobs); {@code publishedAt} = reviewedAt, thiếu thì updatedAt. */
public record PublicJobSummary(UUID id, String title, JobLevel level, JobType type, WorkMode workMode, String city,
                               Long salaryMin, Long salaryMax, LocalDate deadline, Instant publishedAt,
                               PublicCompanyRef company) {
}
