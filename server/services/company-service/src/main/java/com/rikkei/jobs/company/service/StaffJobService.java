package com.rikkei.jobs.company.service;

import com.rikkei.jobs.common.config.exception.BadRequestException;
import com.rikkei.jobs.common.config.exception.ResourceNotFoundException;
import com.rikkei.jobs.common.dto.response.PageResponse;
import com.rikkei.jobs.company.config.exception.CompanyErrorCode;
import com.rikkei.jobs.company.config.exception.CompanyException;
import com.rikkei.jobs.company.dto.response.StaffJobDtos.StaffJobCompany;
import com.rikkei.jobs.company.dto.response.StaffJobDtos.StaffJobDetail;
import com.rikkei.jobs.company.dto.response.StaffJobDtos.StaffJobItem;
import com.rikkei.jobs.company.dto.response.StaffJobDtos.StaffJobSummary;
import com.rikkei.jobs.company.entity.Company;
import com.rikkei.jobs.company.entity.Job;
import com.rikkei.jobs.company.entity.JobStatus;
import com.rikkei.jobs.company.mapper.JobMapper;
import com.rikkei.jobs.company.repository.CompanyRepository;
import com.rikkei.jobs.company.repository.JobRepository;
import com.rikkei.jobs.company.repository.JobSkillRepository;
import com.rikkei.jobs.company.repository.PublicJobSpecifications;
import com.rikkei.jobs.company.util.JobRules;
import java.time.Clock;
import java.time.Instant;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * DN-07: cán bộ duyệt hoặc trả lại JD đang chờ (PENDING). Cán bộ không thấy bản nháp của HR.
 * Duyệt: PENDING -> APPROVED, ghi reviewedBy/At, xoá lý do cũ. Trả lại: PENDING -> REJECTED kèm lý do.
 * Hai cán bộ thao tác cùng lúc: @Version của Job làm người sau nhận 409.
 */
@Service
@RequiredArgsConstructor
public class StaffJobService {

    private static final String NOT_FOUND = "Không tìm thấy JD.";
    private static final List<JobStatus> VISIBLE = List.of(JobStatus.PENDING, JobStatus.APPROVED, JobStatus.REJECTED, JobStatus.CLOSED);

    private final JobRepository jobRepository;
    private final JobSkillRepository jobSkillRepository;
    private final CompanyRepository companyRepository;
    private final JobMapper jobMapper;
    private final Clock clock;

    @Transactional(readOnly = true)
    public PageResponse<StaffJobItem> list(JobStatus status, String q, UUID companyId, int page, int size) {
        if (page < 1 || size < 1 || size > 100) throw new BadRequestException("Phân trang không hợp lệ.");
        if (status == JobStatus.DRAFT) throw new BadRequestException("Cán bộ không xem được JD nháp.");
        Specification<Job> spec = Specification.<Job>where((root, query, cb) -> root.get("status").in(VISIBLE))
                .and(PublicJobSpecifications.equal("status", status))
                .and(PublicJobSpecifications.equal("companyId", companyId))
                .and(PublicJobSpecifications.keyword(likePattern(q), null))
                // Hàng chờ duyệt: gửi sớm xử lý trước. Các trạng thái khác: mới cập nhật trước.
                .and((root, query, cb) -> {
                    query.orderBy(status == JobStatus.PENDING
                            ? List.of(cb.asc(root.get("updatedAt")), cb.asc(root.get("id")))
                            : List.of(cb.desc(root.get("updatedAt")), cb.desc(root.get("id"))));
                    return null;
                });
        Page<Job> result = jobRepository.findAll(spec, PageRequest.of(page - 1, size));

        List<UUID> jobIds = result.getContent().stream().map(Job::getId).toList();
        Map<UUID, Integer> required = new HashMap<>();
        if (!jobIds.isEmpty()) {
            for (Object[] row : jobSkillRepository.countRequiredByJobIds(jobIds)) {
                required.put((UUID) row[0], ((Number) row[1]).intValue());
            }
        }
        Map<UUID, Company> companies = companyRepository.findAllById(result.getContent().stream().map(Job::getCompanyId).distinct().toList())
                .stream().collect(Collectors.toMap(Company::getId, Function.identity()));

        return PageResponse.of(result, result.getContent().stream().map(job -> {
            Company c = companies.get(job.getCompanyId());
            return new StaffJobItem(job.getId(), job.getTitle(), job.getLevel(), job.getStatus(), job.getDeadline(),
                    job.getUpdatedAt(), job.getReviewedAt(), required.getOrDefault(job.getId(), 0),
                    job.getStatus() == JobStatus.REJECTED ? job.getRejectReason() : null,
                    job.getCompanyId(), c == null ? null : c.getName(), c == null ? null : c.getStatus());
        }).toList());
    }

    @Transactional(readOnly = true)
    public StaffJobSummary summary() {
        Map<JobStatus, Long> byStatus = new EnumMap<>(JobStatus.class);
        VISIBLE.forEach(s -> byStatus.put(s, 0L));
        long total = 0;
        for (Object[] row : jobRepository.countAllByStatusExcluding(JobStatus.DRAFT)) {
            long count = ((Number) row[1]).longValue();
            byStatus.put((JobStatus) row[0], count);
            total += count;
        }
        return new StaffJobSummary(total, byStatus);
    }

    @Transactional(readOnly = true)
    public StaffJobDetail get(UUID id) {
        return detail(visibleJob(id));
    }

    @Transactional
    public StaffJobDetail approve(UUID staffId, UUID id) {
        Job job = pending(id);
        if (job.getDeadline() != null && job.getDeadline().isBefore(JobRules.today(clock))) {
            throw new CompanyException(CompanyErrorCode.INVALID_JOB_STATUS,
                    "Hạn nộp của JD đã qua. Hãy trả lại để HR cập nhật hạn nộp trước khi duyệt.");
        }
        job.setStatus(JobStatus.APPROVED);
        job.setRejectReason(null);
        return review(staffId, job);
    }

    @Transactional
    public StaffJobDetail reject(UUID staffId, UUID id, String reason) {
        Job job = pending(id);
        job.setStatus(JobStatus.REJECTED);
        job.setRejectReason(reason);
        return review(staffId, job);
    }

    private StaffJobDetail review(UUID staffId, Job job) {
        job.setReviewedBy(staffId);
        job.setReviewedAt(Instant.now(clock));
        return detail(jobRepository.saveAndFlush(job));
    }

    private Job pending(UUID id) {
        Job job = visibleJob(id);
        if (job.getStatus() != JobStatus.PENDING) {
            throw new CompanyException(CompanyErrorCode.INVALID_JOB_STATUS, "JD không ở trạng thái chờ duyệt (có thể cán bộ khác vừa xử lý).");
        }
        return job;
    }

    private Job visibleJob(UUID id) {
        return jobRepository.findById(id).filter(job -> job.getStatus() != JobStatus.DRAFT)
                .orElseThrow(() -> new ResourceNotFoundException(NOT_FOUND));
    }

    private StaffJobDetail detail(Job job) {
        Company c = companyRepository.findById(job.getCompanyId()).orElse(null);
        StaffJobCompany company = c == null ? null : new StaffJobCompany(c.getId(), c.getName(), c.getStatus(),
                c.getIndustry(), c.getSize(), c.getCity(), c.getLogoUrl());
        return new StaffJobDetail(jobMapper.toResponse(job, jobSkillRepository.findByJobId(job.getId())), job.getReviewedBy(), company);
    }

    private static String likePattern(String value) {
        if (value == null || value.isBlank()) return null;
        return "%" + value.trim().toLowerCase(Locale.ROOT).replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
    }
}
