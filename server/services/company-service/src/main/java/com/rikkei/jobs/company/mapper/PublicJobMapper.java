package com.rikkei.jobs.company.mapper;

import com.rikkei.jobs.company.dto.response.PublicCompanyInfo;
import com.rikkei.jobs.company.dto.response.PublicCompanyRef;
import com.rikkei.jobs.company.dto.response.PublicJobDetail;
import com.rikkei.jobs.company.dto.response.PublicJobSummary;
import com.rikkei.jobs.company.entity.Company;
import com.rikkei.jobs.company.entity.Job;
import com.rikkei.jobs.company.entity.JobSkill;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** JD cho trang việc làm công khai (DN-07 §4.3); chỉ dùng cho JD đã qua quy tắc hiển thị của JobRepository. */
@Component
@RequiredArgsConstructor
public class PublicJobMapper {

    private final JobMapper jobMapper;

    public PublicJobSummary toSummary(Job job, Company company) {
        return new PublicJobSummary(job.getId(), job.getTitle(), job.getLevel(), job.getType(), job.getWorkMode(),
                job.getCity(), job.getSalaryMin(), job.getSalaryMax(), job.getDeadline(), publishedAt(job),
                new PublicCompanyRef(company.getId(), company.getName(), company.getLogoUrl()));
    }

    public PublicJobDetail toDetail(Job job, List<JobSkill> skills, Company company) {
        PublicCompanyInfo info = new PublicCompanyInfo(company.getId(), company.getName(), company.getLogoUrl(),
                company.getIndustry(), company.getSize(), company.getCity(), company.getWebsite(),
                company.getDescription());
        return new PublicJobDetail(job.getId(), job.getTitle(), job.getPosition(), job.getLevel(), job.getType(),
                job.getWorkMode(), job.getQuantity(), job.getCity(), job.getAddress(), job.getSalaryMin(),
                job.getSalaryMax(), job.getDeadline(), publishedAt(job), job.getDescription(), job.getRequirements(),
                job.getBenefits(), jobMapper.toSkillResponses(skills), info);
    }

    /** Thời điểm duyệt gần nhất; dữ liệu cũ chưa có reviewedAt thì lấy updatedAt (khớp thứ tự sắp của danh sách). */
    private static Instant publishedAt(Job job) {
        return job.getReviewedAt() != null ? job.getReviewedAt() : job.getUpdatedAt();
    }
}
