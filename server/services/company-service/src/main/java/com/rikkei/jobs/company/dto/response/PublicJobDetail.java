package com.rikkei.jobs.company.dto.response;

import com.rikkei.jobs.company.entity.JobLevel;
import com.rikkei.jobs.company.entity.JobType;
import com.rikkei.jobs.company.entity.WorkMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Chi tiết JD cho ứng viên và khách (GET /jobs/{id}). Cố ý không có status, rejectReason, reviewedBy, createdBy.
 * Kỹ năng sắp như DN-02 (bắt buộc trước, rồi điểm tối thiểu giảm dần).
 */
public record PublicJobDetail(UUID id, String title, String position, JobLevel level, JobType type, WorkMode workMode,
                              Integer quantity, String city, String address, Long salaryMin, Long salaryMax,
                              LocalDate deadline, Instant publishedAt, String description, String requirements,
                              String benefits, List<JobSkillResponse> skills, PublicCompanyInfo company) {
}
