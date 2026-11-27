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

    public JobResponse toResponse(Job job, List<JobSkill> skills) {
        List<JobSkillResponse> sorted = skills.stream()
                .sorted(SKILL_ORDER)
                .map(s -> new JobSkillResponse(s.getSkillId(), s.isRequired(), s.getMinScore()))
                .toList();
        return new JobResponse(job.getId(), job.getCompanyId(), job.getTitle(), job.getPosition(), job.getLevel(),
                job.getType(), job.getQuantity(), job.getCity(), job.getAddress(), job.getWorkMode(),
                job.getSalaryMin(), job.getSalaryMax(), job.getDescription(), job.getRequirements(),
                job.getBenefits(), job.getDeadline(), job.getStatus(),
                job.getStatus() == JobStatus.REJECTED ? job.getRejectReason() : null,
                job.getReviewedAt(), job.getCreatedAt(), job.getUpdatedAt(), sorted);
    }

    public JobSummary toSummary(Job job, int requiredSkillCount) {
        return new JobSummary(job.getId(), job.getTitle(), job.getLevel(), job.getStatus(), job.getDeadline(),
                job.getUpdatedAt(), requiredSkillCount,
                job.getStatus() == JobStatus.REJECTED ? job.getRejectReason() : null);
    }
}
