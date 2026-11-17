package com.rikkei.jobs.company.service.impl;

import static com.rikkei.jobs.company.mapper.CompanyMapper.blankToNull;

import com.rikkei.jobs.common.config.exception.BadRequestException;
import com.rikkei.jobs.common.config.exception.ResourceNotFoundException;
import com.rikkei.jobs.common.dto.response.PageResponse;
import com.rikkei.jobs.company.client.SkillCatalogClient;
import com.rikkei.jobs.company.config.exception.CompanyErrorCode;
import com.rikkei.jobs.company.config.exception.CompanyException;
import com.rikkei.jobs.company.config.exception.JobValidationException;
import com.rikkei.jobs.company.dto.request.JobRequest;
import com.rikkei.jobs.company.dto.request.JobSkillRequest;
import com.rikkei.jobs.company.dto.response.JobResponse;
import com.rikkei.jobs.company.dto.response.JobSummary;
import com.rikkei.jobs.company.entity.Company;
import com.rikkei.jobs.company.entity.CompanyStatus;
import com.rikkei.jobs.company.entity.Job;
import com.rikkei.jobs.company.entity.JobSkill;
import com.rikkei.jobs.company.entity.JobStatus;
import com.rikkei.jobs.company.mapper.JobMapper;
import com.rikkei.jobs.company.repository.JobRepository;
import com.rikkei.jobs.company.repository.JobSkillRepository;
import com.rikkei.jobs.company.service.CompanyService;
import com.rikkei.jobs.company.service.JobService;
import com.rikkei.jobs.company.util.JobRules;
import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Thứ tự kiểm tra khi ghi: công ty (LOCKED) → JD của công ty (404) → trạng thái → dữ liệu → danh mục kỹ năng
 * (chỉ khi có skills) → ghi. Danh mục được gọi trước mọi lệnh ghi nên profile-service lỗi không để lại lưu dở dang.
 * Xung đột @Version (ObjectOptimisticLockingFailureException) được CompanyExceptionHandler đổi thành 409 CONFLICT.
 */
@Service
@RequiredArgsConstructor
public class JobServiceImpl implements JobService {

    private static final int MAX_SKILLS = 20;
    private static final String SKILLS = "skills";

    private final CompanyService companyService;
    private final JobRepository jobRepository;
    private final JobSkillRepository jobSkillRepository;
    private final SkillCatalogClient skillCatalog;
    private final JobMapper jobMapper;
    private final Clock clock;

    @Override
    @Transactional
    public JobResponse create(UUID hrUserId, JobRequest req, String authorization) {
        Company company = writableCompany(hrUserId);
        validateSave(req, authorization);

        Job job = new Job();
        job.setCompanyId(company.getId());
        job.setCreatedBy(hrUserId);
        job.setStatus(JobStatus.DRAFT);
        applyGeneral(job, req);
        Job saved = jobRepository.saveAndFlush(job);

        List<JobSkill> skills = req.getSkills() == null
                ? List.of()
                : jobSkillRepository.saveAll(toEntities(saved.getId(), req.getSkills()));
        return jobMapper.toResponse(saved, skills);
    }

    @Override
    @Transactional
    public JobResponse update(UUID hrUserId, UUID jobId, JobRequest req, String authorization) {
        Company company = writableCompany(hrUserId);
        Job job = editableJob(company, jobId);
        validateSave(req, authorization);
        applyGeneral(job, req);
        return finishEdit(job, req.getSkills());
    }

    @Override
    @Transactional
    public JobResponse replaceSkills(UUID hrUserId, UUID jobId, List<JobSkillRequest> skills, String authorization) {
        Company company = writableCompany(hrUserId);
        Job job = editableJob(company, jobId);
        String error = skillsError(skills);
        if (error != null) {
            throw new JobValidationException(Map.of(SKILLS, error));
        }
        checkCatalog(skills, authorization);
        return finishEdit(job, skills);
    }

    @Override
    @Transactional
    public JobResponse submit(UUID hrUserId, UUID jobId) {
        Job job = ownJob(writableCompany(hrUserId), jobId);
        if (job.getStatus() != JobStatus.DRAFT && job.getStatus() != JobStatus.REJECTED) {
            throw new CompanyException(CompanyErrorCode.INVALID_JOB_STATUS);
        }
        List<JobSkill> skills = jobSkillRepository.findByJobId(job.getId());
        Map<String, String> errors = JobRules.submitErrors(job, skills, JobRules.today(clock));
        if (!errors.isEmpty()) {
            throw new JobValidationException(errors);
        }
        job.setStatus(JobStatus.PENDING);
        job.setRejectReason(null);
        return jobMapper.toResponse(jobRepository.saveAndFlush(job), skills);
    }

    @Override
    @Transactional
    public JobResponse close(UUID hrUserId, UUID jobId) {
        Job job = ownJob(writableCompany(hrUserId), jobId);
        if (job.getStatus() != JobStatus.APPROVED) {
            throw new CompanyException(CompanyErrorCode.INVALID_JOB_STATUS);
        }
        job.setStatus(JobStatus.CLOSED);
        return jobMapper.toResponse(jobRepository.saveAndFlush(job), jobSkillRepository.findByJobId(job.getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public JobResponse get(UUID hrUserId, UUID jobId) {
        Company company = companyService.findMine(hrUserId);
        Job job = ownJob(company, jobId);
        return jobMapper.toResponse(job, jobSkillRepository.findByJobId(job.getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<JobSummary> listMine(UUID hrUserId, JobStatus status, int page, int size) {
        if (page < 1 || size < 1 || size > 100) {
            throw new BadRequestException("Phân trang không hợp lệ.");
        }
        Company company = companyService.findMine(hrUserId);
        PageRequest pageable = PageRequest.of(page - 1, size,
                Sort.by(Sort.Order.desc("updatedAt"), Sort.Order.desc("id")));
        Page<Job> result = status == null
                ? jobRepository.findByCompanyId(company.getId(), pageable)
                : jobRepository.findByCompanyIdAndStatus(company.getId(), status, pageable);
        return PageResponse.of(result, result.getContent().stream().map(jobMapper::toSummary).toList());
    }

    // ---- các bước dùng chung ----

    private Company writableCompany(UUID hrUserId) {
        Company company = companyService.findMine(hrUserId);
        if (company.getStatus() == CompanyStatus.LOCKED) {
            throw new CompanyException(CompanyErrorCode.COMPANY_LOCKED);
        }
        return company;
    }

    /** JD của công ty khác và id không tồn tại cho cùng một lỗi 404. */
    private Job ownJob(Company company, UUID jobId) {
        return jobRepository.findByIdAndCompanyId(jobId, company.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy JD"));
    }

    private Job editableJob(Company company, UUID jobId) {
        Job job = ownJob(company, jobId);
        if (job.getStatus() == JobStatus.PENDING || job.getStatus() == JobStatus.CLOSED) {
            throw new CompanyException(CompanyErrorCode.JOB_NOT_EDITABLE);
        }
        return job;
    }

    /** Kiểm tra mức "mọi lần lưu", rồi danh mục kỹ năng nếu request có skills. */
    private void validateSave(JobRequest req, String authorization) {
        Map<String, String> errors = new LinkedHashMap<>(JobRules.saveErrors(req, JobRules.today(clock)));
        if (req.getSkills() != null && !errors.containsKey(SKILLS)) {
            String error = skillsError(req.getSkills());
            if (error != null) {
                errors.put(SKILLS, error);
            }
        }
        if (!errors.isEmpty()) {
            throw new JobValidationException(errors);
        }
        if (req.getSkills() != null) {
            checkCatalog(req.getSkills(), authorization);
        }
    }

    /**
     * Quy tắc cấu trúc của danh sách kỹ năng, kiểm ở service vì PUT /jobs/{id}/skills nhận mảng trần
     * (ràng buộc Bean Validation trên tham số List không đổi được thành 400 VALIDATION_ERROR).
     * Với JobRequest, Bean Validation đã chặn phần lớn; ở đây còn chặn phần tử null.
     */
    private static String skillsError(List<JobSkillRequest> skills) {
        if (skills == null) {
            return "Danh sách kỹ năng không được để trống";
        }
        if (skills.size() > MAX_SKILLS) {
            return "Tối đa 20 kỹ năng";
        }
        Set<UUID> seen = new HashSet<>();
        for (JobSkillRequest s : skills) {
            if (s == null || s.getSkillId() == null) {
                return "Mã kỹ năng không được để trống";
            }
            if (s.getRequired() == null) {
                return "Cần chọn kỹ năng bắt buộc hay không";
            }
            if (s.getMinScore() == null || s.getMinScore() < 1 || s.getMinScore() > 100) {
                return "Điểm tối thiểu từ 1 đến 100";
            }
            if (!seen.add(s.getSkillId())) {
                return "Kỹ năng bị trùng";
            }
        }
        return null;
    }

    /** Gọi profile-service (lỗi → 503 EXTERNAL_UNAVAILABLE); id ngoài danh mục → 400 errors.skills. */
    private void checkCatalog(List<JobSkillRequest> skills, String authorization) {
        Set<UUID> catalog = skillCatalog.skillIds(authorization);
        boolean unknown = skills.stream().anyMatch(s -> !catalog.contains(s.getSkillId()));
        if (unknown) {
            throw new JobValidationException(Map.of(SKILLS, "Kỹ năng không có trong danh mục"));
        }
    }

    /** Thay toàn bộ thông tin chung (field không gửi bị xoá), trừ title luôn bắt buộc. */
    private static void applyGeneral(Job job, JobRequest req) {
        job.setTitle(req.getTitle().strip());
        job.setPosition(blankToNull(req.getPosition()));
        job.setLevel(req.getLevel());
        job.setType(req.getType());
        job.setQuantity(req.getQuantity());
        job.setCity(blankToNull(req.getCity()));
        job.setAddress(blankToNull(req.getAddress()));
        job.setWorkMode(req.getWorkMode());
        job.setSalaryMin(req.getSalaryMin());
        job.setSalaryMax(req.getSalaryMax());
        job.setDescription(blankToNull(req.getDescription()));
        job.setRequirements(blankToNull(req.getRequirements()));
        job.setBenefits(blankToNull(req.getBenefits()));
        job.setDeadline(req.getDeadline());
    }

    /**
     * Phần chung của PUT /jobs/{id} và PUT /jobs/{id}/skills sau khi đã áp thông tin mới lên entity.
     * {@code newSkills} null = giữ kỹ năng cũ. JD APPROVED phải qua kiểm tra gửi duyệt trên dữ liệu sau khi sửa
     * (lỗi → ném, transaction rollback, JD vẫn APPROVED) rồi chuyển PENDING, giữ reviewed_by/at cũ.
     */
    private JobResponse finishEdit(Job job, List<JobSkillRequest> newSkills) {
        // Không nạp kỹ năng cũ khi sắp thay: bulk delete không đồng bộ persistence context, nên context
        // không được giữ JobSkill cũ trùng khoá với bản ghi sắp chèn.
        List<JobSkill> skills = newSkills == null
                ? jobSkillRepository.findByJobId(job.getId())
                : toEntities(job.getId(), newSkills);

        if (job.getStatus() == JobStatus.APPROVED) {
            Map<String, String> errors = JobRules.submitErrors(job, skills, JobRules.today(clock));
            if (!errors.isEmpty()) {
                throw new JobValidationException(errors);
            }
            job.setStatus(JobStatus.PENDING);
        }

        if (newSkills != null) {
            // Chỉ đổi kỹ năng thì jobs không có cột nào đổi: gán updatedAt để có UPDATE và @Version tăng.
            job.setUpdatedAt(Instant.now(clock));
            // deleteByJobId flush thay đổi đang chờ trước khi chạy, và lệnh xoá hàng loạt chạy ngay xuống DB,
            // nên các bản ghi chèn sau không đụng khoá cũ.
            jobSkillRepository.deleteByJobId(job.getId());
            skills = jobSkillRepository.saveAll(skills);
        }
        Job saved = jobRepository.saveAndFlush(job);
        return jobMapper.toResponse(saved, skills);
    }

    private static List<JobSkill> toEntities(UUID jobId, List<JobSkillRequest> skills) {
        return skills.stream()
                .map(s -> new JobSkill(jobId, s.getSkillId(), s.getRequired(), s.getMinScore()))
                .toList();
    }
}
