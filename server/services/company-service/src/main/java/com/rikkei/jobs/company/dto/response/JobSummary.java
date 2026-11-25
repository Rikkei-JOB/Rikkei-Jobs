package com.rikkei.jobs.company.dto.response;

import com.rikkei.jobs.company.entity.JobLevel;
import com.rikkei.jobs.company.entity.JobStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Một dòng trong danh sách JD của HR. {@code rejectReason} chỉ có khi status là REJECTED. */
public record JobSummary(UUID id, String title, JobLevel level, JobStatus status, LocalDate deadline,
                         Instant updatedAt, int requiredSkillCount, String rejectReason) {
}
