package com.rikkei.jobs.company.service.impl;

import static com.rikkei.jobs.company.repository.PublicJobSpecifications.cityContains;
import static com.rikkei.jobs.company.repository.PublicJobSpecifications.equal;
import static com.rikkei.jobs.company.repository.PublicJobSpecifications.keyword;
import static com.rikkei.jobs.company.repository.PublicJobSpecifications.ordered;
import static com.rikkei.jobs.company.repository.PublicJobSpecifications.salaryOverlaps;
import static com.rikkei.jobs.company.repository.PublicJobSpecifications.visible;

import com.rikkei.jobs.common.config.exception.BadRequestException;
import com.rikkei.jobs.common.config.exception.ResourceNotFoundException;
import com.rikkei.jobs.common.dto.response.PageResponse;
import com.rikkei.jobs.company.dto.response.JobSkillResponse;
import com.rikkei.jobs.company.dto.response.PublicCompanyDetail;
import com.rikkei.jobs.company.dto.response.PublicCompanyItem;
import com.rikkei.jobs.company.dto.response.PublicCompanySummary;
import com.rikkei.jobs.company.dto.response.PublicJobDetail;
import com.rikkei.jobs.company.dto.response.PublicJobItem;
import com.rikkei.jobs.company.entity.Company;
import com.rikkei.jobs.company.entity.CompanyStatus;
import com.rikkei.jobs.company.entity.Job;
import com.rikkei.jobs.company.entity.JobSkill;
import com.rikkei.jobs.company.entity.JobStatus;
import com.rikkei.jobs.company.repository.CompanyRepository;
import com.rikkei.jobs.company.repository.JobRepository;
import com.rikkei.jobs.company.repository.JobSkillRepository;
import com.rikkei.jobs.company.service.PublicJobService;
import com.rikkei.jobs.company.util.JobRules;
import java.time.Clock;
import java.util.Comparator;
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

@Service
@RequiredArgsConstructor
public class PublicJobServiceImpl implements PublicJobService {

    private static final String JOB_NOT_FOUND = "Không tìm thấy tin tuyển dụng hoặc tin đã hết hạn.";
    private static final String COMPANY_NOT_FOUND = "Không tìm thấy công ty.";
    /** Cùng thứ tự với JobMapper: kỹ năng bắt buộc trước, rồi điểm tối thiểu giảm dần. */
    private static final Comparator<JobSkill> SKILL_ORDER = Comparator
            .comparing((JobSkill s) -> !s.isRequired())
            .thenComparing(JobSkill::getMinScore, Comparator.reverseOrder())
            .thenComparing(JobSkill::getSkillId);

    private final JobRepository jobRepository;
    private final JobSkillRepository jobSkillRepository;
    private final CompanyRepository companyRepository;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PublicJobItem> search(JobQuery q) {
        if (q.page() < 1 || q.size() < 1 || q.size() > 100) {
            throw new BadRequestException("Phân trang không hợp lệ.");
        }
        if (q.sort() != null && !q.sort().equals("newest") && !q.sort().equals("salary")) {
            throw new BadRequestException("Kiểu sắp xếp không hợp lệ.");
        }
        Specification<Job> spec = Specification.where(visible(JobRules.today(clock)))
                .and(keyword(likePattern(q.q()), q.skillIds()))
                .and(cityContains(likePattern(q.city())))
                .and(equal("level", q.level()))
                .and(equal("workMode", q.workMode()))
                .and(equal("type", q.type()))
                .and(equal("companyId", q.companyId()))
                .and(salaryOverlaps(q.salaryFrom(), q.salaryTo()))
                .and(ordered("salary".equals(q.sort())));
        Page<Job> page = jobRepository.findAll(spec, PageRequest.of(q.page() - 1, q.size()));

        List<UUID> jobIds = page.getContent().stream().map(Job::getId).toList();
        Map<UUID, List<JobSkillResponse>> skills = jobIds.isEmpty() ? Map.of()
                : jobSkillRepository.findByJobIdIn(jobIds).stream().sorted(SKILL_ORDER).collect(Collectors.groupingBy(
                        JobSkill::getJobId, Collectors.mapping(PublicJobServiceImpl::skill, Collectors.toList())));
        Map<UUID, Company> companies = companyRepository.findAllById(
                        page.getContent().stream().map(Job::getCompanyId).distinct().toList())
                .stream().collect(Collectors.toMap(Company::getId, Function.identity()));

        return PageResponse.of(page, page.getContent().stream().map(job -> new PublicJobItem(
                job.getId(), job.getTitle(), job.getLevel(), job.getType(), job.getWorkMode(), job.getCity(),
                job.getSalaryMin(), job.getSalaryMax(), job.getDeadline(), job.getCreatedAt(),
                skills.getOrDefault(job.getId(), List.of()), summary(companies.get(job.getCompanyId())))).toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PublicJobDetail get(UUID jobId) {
        Job job = jobRepository.findOne(Specification.where(visible(JobRules.today(clock))).and(equal("id", jobId)))
                .orElseThrow(() -> new ResourceNotFoundException(JOB_NOT_FOUND));
        Company company = companyRepository.findById(job.getCompanyId())
                .orElseThrow(() -> new ResourceNotFoundException(JOB_NOT_FOUND));
        List<JobSkillResponse> skills = jobSkillRepository.findByJobId(job.getId()).stream()
                .sorted(SKILL_ORDER).map(PublicJobServiceImpl::skill).toList();
        return new PublicJobDetail(job.getId(), job.getTitle(), job.getPosition(), job.getLevel(), job.getType(),
                job.getQuantity(), job.getCity(), job.getAddress(), job.getWorkMode(), job.getSalaryMin(),
                job.getSalaryMax(), job.getDescription(), job.getRequirements(), job.getBenefits(), job.getDeadline(),
                job.getCreatedAt(), skills, summary(company));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PublicCompanyItem> companies() {
        Map<UUID, Long> open = openJobCounts();
        return companyRepository.findByStatus(CompanyStatus.ACTIVE).stream()
                .map(c -> new PublicCompanyItem(c.getId(), c.getName(), c.getLogoUrl(), c.getIndustry(), c.getSize(),
                        c.getCity(), open.getOrDefault(c.getId(), 0L)))
                .sorted(Comparator.comparingLong(PublicCompanyItem::openJobs).reversed()
                        .thenComparing(PublicCompanyItem::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PublicCompanyDetail company(UUID companyId) {
        Company c = companyRepository.findById(companyId)
                .filter(found -> found.getStatus() == CompanyStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException(COMPANY_NOT_FOUND));
        return new PublicCompanyDetail(c.getId(), c.getName(), c.getLogoUrl(), c.getIndustry(), c.getSize(),
                c.getCity(), c.getAddress(), c.getWebsite(), c.getDescription(),
                openJobCounts().getOrDefault(c.getId(), 0L));
    }

    private Map<UUID, Long> openJobCounts() {
        Map<UUID, Long> counts = new HashMap<>();
        for (Object[] row : jobRepository.countOpenByCompany(JobStatus.APPROVED, JobRules.today(clock))) {
            counts.put((UUID) row[0], ((Number) row[1]).longValue());
        }
        return counts;
    }

    private static JobSkillResponse skill(JobSkill s) {
        return new JobSkillResponse(s.getSkillId(), s.isRequired(), s.getMinScore());
    }

    private static PublicCompanySummary summary(Company c) {
        return new PublicCompanySummary(c.getId(), c.getName(), c.getLogoUrl(), c.getIndustry(), c.getSize(),
                c.getCity());
    }

    /** null/trống -> null (không lọc); còn lại viết thường, thoát ký tự LIKE và bọc %. */
    private static String likePattern(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String escaped = value.trim().toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + escaped + "%";
    }
}
