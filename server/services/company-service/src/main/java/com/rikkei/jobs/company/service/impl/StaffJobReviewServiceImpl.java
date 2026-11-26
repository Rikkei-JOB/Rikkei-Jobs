package com.rikkei.jobs.company.service.impl;

import com.rikkei.jobs.common.config.exception.BadRequestException;
import com.rikkei.jobs.common.config.exception.ResourceNotFoundException;
import com.rikkei.jobs.common.dto.response.PageResponse;
import com.rikkei.jobs.company.client.IdentityClient;
import com.rikkei.jobs.company.client.IdentityUserStatus;
import com.rikkei.jobs.company.config.exception.CompanyErrorCode;
import com.rikkei.jobs.company.config.exception.CompanyException;
import com.rikkei.jobs.company.dto.response.CompanyRef;
import com.rikkei.jobs.company.dto.response.PublicJobDetail;
import com.rikkei.jobs.company.dto.response.StaffJobDetail;
import com.rikkei.jobs.company.dto.response.StaffJobItem;
import com.rikkei.jobs.company.dto.response.StaffRef;
import com.rikkei.jobs.company.entity.Company;
import com.rikkei.jobs.company.entity.CompanyAction;
import com.rikkei.jobs.company.entity.CompanyActionType;
import com.rikkei.jobs.company.entity.CompanyStatus;
import com.rikkei.jobs.company.entity.Job;
import com.rikkei.jobs.company.entity.JobStatus;
import com.rikkei.jobs.company.mapper.PublicJobMapper;
import com.rikkei.jobs.company.repository.CompanyActionRepository;
import com.rikkei.jobs.company.repository.CompanyRepository;
import com.rikkei.jobs.company.repository.JobRepository;
import com.rikkei.jobs.company.repository.JobSkillRepository;
import com.rikkei.jobs.company.service.StaffJobReviewService;
import com.rikkei.jobs.company.util.JobRules;
import com.rikkei.jobs.company.util.ReasonRules;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Cố ý không @Transactional ở mức lớp: phần ghi DB chạy trong TransactionTemplate và commit xong mới gọi identity
 * lấy tên cán bộ (lời gọi HTTP không nằm trong transaction đang mở, như StaffCompanyServiceImpl).
 */
@Slf4j
@Service
public class StaffJobReviewServiceImpl implements StaffJobReviewService {

    private static final String JOB_NOT_FOUND = "Không tìm thấy JD";

    private final JobRepository jobRepository;
    private final JobSkillRepository jobSkillRepository;
    private final CompanyRepository companyRepository;
    private final CompanyActionRepository companyActionRepository;
    private final PublicJobMapper publicJobMapper;
    private final IdentityClient identity;
    private final Clock clock;
    private final TransactionTemplate writeTx;
    private final TransactionTemplate readTx;

    public StaffJobReviewServiceImpl(JobRepository jobRepository, JobSkillRepository jobSkillRepository,
                                     CompanyRepository companyRepository,
                                     CompanyActionRepository companyActionRepository,
                                     PublicJobMapper publicJobMapper, IdentityClient identity, Clock clock,
                                     PlatformTransactionManager transactionManager) {
        this.jobRepository = jobRepository;
        this.jobSkillRepository = jobSkillRepository;
        this.companyRepository = companyRepository;
        this.companyActionRepository = companyActionRepository;
        this.publicJobMapper = publicJobMapper;
        this.identity = identity;
        this.clock = clock;
        this.writeTx = new TransactionTemplate(transactionManager);
        this.readTx = new TransactionTemplate(transactionManager);
        this.readTx.setReadOnly(true);
    }

    @Override
    public PageResponse<StaffJobItem> list(JobStatus status, UUID companyId, int page, int size) {
        if (page < 1 || size < 1 || size > 100) {
            throw new BadRequestException("Phân trang không hợp lệ.");
        }
        // PENDING không sửa được nên updatedAt là lúc gửi: gửi sớm nhất trước. Còn lại: mới cập nhật trước.
        Sort sort = status == JobStatus.PENDING
                ? Sort.by(Sort.Order.asc("updatedAt"), Sort.Order.asc("id"))
                : Sort.by(Sort.Order.desc("updatedAt"), Sort.Order.desc("id"));
        PageRequest pageable = PageRequest.of(page - 1, size, sort);
        return readTx.execute(s -> {
            Page<Job> result;
            if (status == null) {
                result = companyId == null
                        ? jobRepository.findAll(pageable)
                        : jobRepository.findByCompanyId(companyId, pageable);
            } else {
                result = companyId == null
                        ? jobRepository.findByStatus(status, pageable)
                        : jobRepository.findByCompanyIdAndStatus(companyId, status, pageable);
            }
            // Một truy vấn gom tên công ty cho cả trang, tránh N+1.
            Set<UUID> companyIds = result.getContent().stream().map(Job::getCompanyId).collect(Collectors.toSet());
            Map<UUID, Company> companiesById = companyRepository.findAllById(companyIds).stream()
                    .collect(Collectors.toMap(Company::getId, Function.identity()));
            List<StaffJobItem> items = result.getContent().stream()
                    .map(j -> {
                        Company c = companiesById.get(j.getCompanyId());
                        return new StaffJobItem(j.getId(), j.getTitle(), j.getStatus(), j.getDeadline(),
                                j.getUpdatedAt(), new CompanyRef(j.getCompanyId(), c == null ? null : c.getName()));
                    })
                    .toList();
            return PageResponse.of(result, items);
        });
    }

    @Override
    public StaffJobDetail detail(UUID jobId) {
        Snapshot snapshot = readTx.execute(s -> snapshot(findJob(jobId)));
        return toDetail(snapshot);
    }

    @Override
    public StaffJobDetail approve(UUID staffId, UUID jobId) {
        return act(staffId, jobId, JobStatus.PENDING, CompanyActionType.APPROVE_JD, null, job -> {
            if (job.getDeadline() != null && job.getDeadline().isBefore(JobRules.today(clock))) {
                throw new CompanyException(CompanyErrorCode.JOB_EXPIRED);
            }
            job.setStatus(JobStatus.APPROVED);
            job.setRejectReason(null);
        });
    }

    @Override
    public StaffJobDetail reject(UUID staffId, UUID jobId, String reason) {
        String cleaned = ReasonRules.clean(reason);
        return act(staffId, jobId, JobStatus.PENDING, CompanyActionType.REJECT_JD, cleaned, job -> {
            job.setStatus(JobStatus.REJECTED);
            job.setRejectReason(cleaned);
        });
    }

    @Override
    public StaffJobDetail close(UUID staffId, UUID jobId, String reason) {
        String cleaned = ReasonRules.clean(reason);
        // rejectReason giữ nguyên: lý do đóng chỉ nằm trong lịch sử.
        return act(staffId, jobId, JobStatus.APPROVED, CompanyActionType.CLOSE_JD, cleaned,
                job -> job.setStatus(JobStatus.CLOSED));
    }

    /**
     * Một transaction: kiểm tra trạng thái, đổi JD, ghi lịch sử. Lỗi trạng thái ném trước khi ghi nên không có
     * dòng lịch sử; saveAndFlush để xung đột @Version lộ ra ngay (→ 409 CONFLICT).
     */
    private StaffJobDetail act(UUID staffId, UUID jobId, JobStatus from, CompanyActionType action, String reason,
                               Consumer<Job> change) {
        Snapshot snapshot = writeTx.execute(s -> {
            Job job = findJob(jobId);
            if (job.getStatus() != from) {
                throw new CompanyException(CompanyErrorCode.INVALID_JOB_STATUS);
            }
            change.accept(job);
            job.setReviewedBy(staffId);
            job.setReviewedAt(Instant.now(clock));
            Job saved = jobRepository.saveAndFlush(job);
            companyActionRepository.save(new CompanyAction(saved.getCompanyId(), action, saved.getId(), reason, staffId));
            return snapshot(saved);
        });
        return toDetail(snapshot);
    }

    private Job findJob(UUID jobId) {
        return jobRepository.findById(jobId).orElseThrow(() -> new ResourceNotFoundException(JOB_NOT_FOUND));
    }

    /** Đọc đủ dữ liệu từ DB khi còn trong transaction; phần tra identity làm sau. */
    private Snapshot snapshot(Job job) {
        Company company = companyRepository.findById(job.getCompanyId())
                .orElseThrow(() -> new ResourceNotFoundException(JOB_NOT_FOUND));
        PublicJobDetail view = publicJobMapper.toDetail(job, jobSkillRepository.findByJobId(job.getId()), company);
        return new Snapshot(view, job.getStatus(), job.getRejectReason(), job.getReviewedAt(), job.getReviewedBy(),
                company.getStatus() == CompanyStatus.LOCKED);
    }

    private StaffJobDetail toDetail(Snapshot s) {
        StaffRef reviewedBy = s.reviewedBy() == null ? null : new StaffRef(s.reviewedBy(), staffName(s.reviewedBy()));
        return new StaffJobDetail(s.view(), s.status(), s.rejectReason(), s.reviewedAt(), reviewedBy,
                s.companyLocked());
    }

    /** Tên cán bộ chỉ để hiển thị: identity lỗi thì trả null, chi tiết JD không được hỏng theo. */
    private String staffName(UUID staffId) {
        try {
            return identity.lookupUsers(List.of(staffId)).stream()
                    .filter(u -> staffId.equals(u.userId()))
                    .map(IdentityUserStatus::fullName)
                    .findFirst()
                    .orElse(null);
        } catch (RuntimeException e) {
            log.warn("Không tra được tên cán bộ {} từ identity-service: {}", staffId, e.getMessage());
            return null;
        }
    }

    private record Snapshot(PublicJobDetail view, JobStatus status, String rejectReason, Instant reviewedAt,
                            UUID reviewedBy, boolean companyLocked) {
    }
}
