package com.rikkei.jobs.company.service.impl;

import com.rikkei.jobs.common.config.exception.ResourceNotFoundException;
import com.rikkei.jobs.company.client.IdentityClient;
import com.rikkei.jobs.company.client.IdentityUserStatus;
import com.rikkei.jobs.company.dto.response.JobApplicationContextResponse;
import com.rikkei.jobs.company.entity.Company;
import com.rikkei.jobs.company.entity.CompanyHr;
import com.rikkei.jobs.company.entity.CompanyStatus;
import com.rikkei.jobs.company.entity.Job;
import com.rikkei.jobs.company.repository.CompanyHrRepository;
import com.rikkei.jobs.company.repository.CompanyRepository;
import com.rikkei.jobs.company.repository.JobRepository;
import com.rikkei.jobs.company.service.JobContextService;
import com.rikkei.jobs.company.util.JobRules;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class JobContextServiceImpl implements JobContextService {

    private static final String NOT_FOUND = "Không tìm thấy JD";
    /** identity chỉ nhận tối đa 100 id mỗi lần lookup (cùng quy ước StaffCompanyServiceImpl). */
    private static final int LOOKUP_BATCH_SIZE = 100;

    private final JobRepository jobs;
    private final CompanyRepository companies;
    private final CompanyHrRepository companyHrs;
    private final IdentityClient identity;

    @Override
    @Transactional(readOnly = true)
    public JobApplicationContextResponse applicationContext(UUID jobId) {
        Job job = jobs.findById(jobId).orElseThrow(() -> new ResourceNotFoundException(NOT_FOUND));
        Company company = companies.findById(job.getCompanyId())
                .orElseThrow(() -> new ResourceNotFoundException(NOT_FOUND));
        // JD chưa có hạn (chỉ DRAFT) trả createdAt để không bao giờ "còn hạn".
        OffsetDateTime deadline = job.getDeadline() != null
                ? JobRules.applicationDeadline(job.getDeadline())
                : job.getCreatedAt().atZone(JobRules.ZONE).toOffsetDateTime();
        return new JobApplicationContextResponse(job.getId(), company.getId(), job.getTitle(), company.getName(),
                job.getStatus().name(), deadline, company.getStatus() == CompanyStatus.LOCKED,
                activeHrEmails(company.getId()));
    }

    private List<String> activeHrEmails(UUID companyId) {
        List<UUID> hrIds = companyHrs.findByCompanyId(companyId).stream()
                .filter(hr -> hr.getLockedAt() == null)
                .map(CompanyHr::getUserId)
                .toList();
        if (hrIds.isEmpty()) {
            return List.of();
        }
        List<IdentityUserStatus> found = new ArrayList<>();
        for (int from = 0; from < hrIds.size(); from += LOOKUP_BATCH_SIZE) {
            found.addAll(identity.lookupUsers(hrIds.subList(from, Math.min(from + LOOKUP_BATCH_SIZE, hrIds.size()))));
        }
        return found.stream()
                .filter(u -> "ACTIVE".equals(u.status()))
                .map(IdentityUserStatus::email)
                .filter(e -> e != null && !e.isBlank())
                .map(e -> e.toLowerCase(Locale.ROOT))
                .distinct()
                .toList();
    }
}
