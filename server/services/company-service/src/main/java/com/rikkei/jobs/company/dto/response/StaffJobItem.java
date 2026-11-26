package com.rikkei.jobs.company.dto.response;

import com.rikkei.jobs.company.entity.JobStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Một dòng trong hàng chờ / danh sách JD của cán bộ; {@code submittedAt} = {@code updatedAt} của JD. */
public record StaffJobItem(UUID id, String title, JobStatus status, LocalDate deadline, Instant submittedAt,
                           CompanyRef company) {
}
