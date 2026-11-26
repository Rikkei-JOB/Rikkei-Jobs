package com.rikkei.jobs.company.mapper;

import com.rikkei.jobs.company.dto.response.JobResponse;
import com.rikkei.jobs.company.dto.response.JobSkillResponse;
import com.rikkei.jobs.company.dto.response.JobSummary;
import com.rikkei.jobs.company.entity.Job;
import com.rikkei.jobs.company.entity.JobSkill;
import com.rikkei.jobs.company.entity.JobStatus;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class JobMapper {

    /** Bắt buộc trước, rồi điểm tối thiểu giảm dần; cùng điểm thì theo skillId cho thứ tự ổn định. */
    private static final Comparator<JobSkill> SKILL_ORDER = Comparator
            .<JobSkill, Boolean>comparing(JobSkill::isRequired).reversed()
            .thenComparing(Comparator.comparingInt(JobSkill::getMinScore).reversed())
            .thenComparing(JobSkill::getSkillId);

    /** Kỹ năng đã sắp theo {@link #SKILL_ORDER}; trang công khai (PublicJobMapper) dùng chung để cùng thứ tự. */
    public List<JobSkillResponse> toSkillResponses(List<JobSkill> skills) {
        return skills.stream()
                .sorted(SKILL_ORDER)
                .map(s -> new JobSkillResponse(s.getSkillId(), s.isRequired(), s.getMinScore()))
                .toList();
    }

    public JobResponse toResponse(Job job, List<JobSkill> skills) {
        List<JobSkillResponse> sorted = toSkillResponses(skills);
        return new JobResponse(job.getId(), job.getCompanyId(), job.getTitle(), job.getPosition(), job.getLevel(),
                job.getType(), job.getQuantity(), job.getCity(), job.getAddress(), job.getWorkMode(),
                job.getSalaryMin(), job.getSalaryMax(), job.getDescription(), job.getRequirements(),
                job.getBenefits(), job.getDeadline(), job.getStatus(),
                job.getStatus() == JobStatus.REJECTED ? job.getRejectReason() : null,
                job.getReviewedAt(), job.getCreatedAt(), job.getUpdatedAt(), sorted);
    }

    public JobSummary toSummary(Job job) {
        return new JobSummary(job.getId(), job.getTitle(), job.getLevel(), job.getStatus(), job.getDeadline(),
                job.getUpdatedAt());
    }
}
