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
import com.rikkei.jobs.company.entity.CompanyAction;
import com.rikkei.jobs.company.entity.CompanyActionType;
import com.rikkei.jobs.company.entity.Job;
import com.rikkei.jobs.company.entity.JobStatus;
import com.rikkei.jobs.company.mapper.JobMapper;
import com.rikkei.jobs.company.repository.CompanyActionRepository;
import com.rikkei.jobs.company.repository.CompanyRepository;
import com.rikkei.jobs.company.repository.JobRepository;
import com.rikkei.jobs.company.repository.JobSkillRepository;
import com.rikkei.jobs.company.repository.PublicJobSpecifications;
import com.rikkei.jobs.company.util.JobRules;
import com.rikkei.jobs.company.util.ReasonRules;
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
 * DN-07: cán bộ duyệt hoặc trả lại JD đang chờ (PENDING), đóng JD đang đăng. Cán bộ không thấy bản nháp của HR.
 * Duyệt: PENDING -> APPROVED, ghi reviewedBy/At, xoá lý do cũ. Trả lại: PENDING -> REJECTED kèm lý do.
 * Đóng: APPROVED -> CLOSED kèm lý do. Mỗi thao tác ghi một dòng company_actions trong cùng transaction.
 * JD nháp: không có trong danh sách / summary và xem chi tiết trả 404, nhưng duyệt / trả lại / đóng trả
 * 409 INVALID_JOB_STATUS (JD tồn tại, chỉ sai trạng thái); 404 dành riêng cho id không có trong DB.
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
    private final CompanyActionRepository companyActionRepository;
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
            throw new CompanyException(CompanyErrorCode.JOB_EXPIRED,
                    "Hạn nộp của JD đã qua. Hãy trả lại để HR cập nhật hạn nộp trước khi duyệt.");
        }
        job.setStatus(JobStatus.APPROVED);
        job.setRejectReason(null);
        return review(staffId, job, CompanyActionType.APPROVE_JD, null);
    }

    @Transactional
    public StaffJobDetail reject(UUID staffId, UUID id, String reason) {
        String cleaned = ReasonRules.clean(reason);
        Job job = pending(id);
        job.setStatus(JobStatus.REJECTED);
        job.setRejectReason(cleaned);
        return review(staffId, job, CompanyActionType.REJECT_JD, cleaned);
    }

    /** DN-07: đóng JD đang đăng (vi phạm). Chỉ APPROVED; lý do chỉ nằm trong lịch sử, rejectReason giữ nguyên. */
    @Transactional
    public StaffJobDetail close(UUID staffId, UUID id, String reason) {
        String cleaned = ReasonRules.clean(reason);
        Job job = reviewableJob(id);
        if (job.getStatus() != JobStatus.APPROVED) {
            throw new CompanyException(CompanyErrorCode.INVALID_JOB_STATUS, "Chỉ đóng được JD đang đăng (đã duyệt).");
        }
        job.setStatus(JobStatus.CLOSED);
        return review(staffId, job, CompanyActionType.CLOSE_JD, cleaned);
    }

    /**
     * Cùng transaction với thao tác: đổi JD rồi ghi một dòng lịch sử công ty (DN-07). Lỗi trạng thái ném trước đó nên
     * không có dòng lịch sử; saveAndFlush để xung đột @Version lộ ra ngay.
     */
    private StaffJobDetail review(UUID staffId, Job job, CompanyActionType action, String reason) {
        job.setReviewedBy(staffId);
        job.setReviewedAt(Instant.now(clock));
        Job saved = jobRepository.saveAndFlush(job);
        companyActionRepository.save(new CompanyAction(saved.getCompanyId(), action, saved.getId(), reason, staffId));
        return detail(saved);
    }

    private Job pending(UUID id) {
        Job job = reviewableJob(id);
        if (job.getStatus() != JobStatus.PENDING) {
            throw new CompanyException(CompanyErrorCode.INVALID_JOB_STATUS, "JD không ở trạng thái chờ duyệt (có thể cán bộ khác vừa xử lý).");
        }
        return job;
    }

    /**
     * Đọc JD để duyệt / trả lại / đóng: JD nháp vẫn tồn tại nên trả 409 INVALID_JOB_STATUS (DN-07),
     * 404 chỉ dành cho id không có trong DB. Khác với {@link #visibleJob(UUID)} dùng cho phần xem.
     */
    private Job reviewableJob(UUID id) {
        Job job = jobRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(NOT_FOUND));
        if (job.getStatus() == JobStatus.DRAFT) {
            throw new CompanyException(CompanyErrorCode.INVALID_JOB_STATUS,
                    "JD vẫn là bản nháp, HR chưa gửi duyệt nên cán bộ không xử lý được.");
        }
        return job;
    }

    /** Đọc JD để xem chi tiết: cán bộ không thấy bản nháp của HR, nên JD nháp trả 404 như id không tồn tại. */
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
