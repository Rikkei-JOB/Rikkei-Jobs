package com.rikkei.jobs.company.service.impl;

import com.rikkei.jobs.common.config.exception.BadRequestException;
import com.rikkei.jobs.common.config.exception.ResourceNotFoundException;
import com.rikkei.jobs.common.dto.response.PageResponse;
import com.rikkei.jobs.company.dto.response.PublicJobDetail;
import com.rikkei.jobs.company.dto.response.PublicJobSummary;
import com.rikkei.jobs.company.entity.Company;
import com.rikkei.jobs.company.entity.Job;
import com.rikkei.jobs.company.mapper.PublicJobMapper;
import com.rikkei.jobs.company.repository.CompanyRepository;
import com.rikkei.jobs.company.repository.JobRepository;
import com.rikkei.jobs.company.repository.JobSkillRepository;
import com.rikkei.jobs.company.service.PublicJobService;
import com.rikkei.jobs.company.util.JobRules;
import java.time.Clock;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PublicJobServiceImpl implements PublicJobService {

    private static final String JOB_NOT_FOUND = "Không tìm thấy JD";

    private final JobRepository jobRepository;
    private final JobSkillRepository jobSkillRepository;
    private final CompanyRepository companyRepository;
    private final PublicJobMapper publicJobMapper;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PublicJobSummary> list(int page, int size) {
        if (page < 1 || size < 1 || size > 100) {
            throw new BadRequestException("Phân trang không hợp lệ.");
        }
        // Thứ tự nằm trong truy vấn, PageRequest không mang sort.
        Page<Job> result = jobRepository.findVisibleToCandidates(JobRules.today(clock), PageRequest.of(page - 1, size));
        // Một truy vấn gom công ty cho cả trang, tránh N+1.
        Set<UUID> companyIds = result.getContent().stream().map(Job::getCompanyId).collect(Collectors.toSet());
        Map<UUID, Company> companiesById = companyRepository.findAllById(companyIds).stream()
                .collect(Collectors.toMap(Company::getId, Function.identity()));
        return PageResponse.of(result, result.getContent().stream()
                .map(job -> publicJobMapper.toSummary(job, companiesById.get(job.getCompanyId())))
                .toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PublicJobDetail get(UUID id) {
        Job job = jobRepository.findVisibleToCandidatesById(id, JobRules.today(clock))
                .orElseThrow(() -> new ResourceNotFoundException(JOB_NOT_FOUND));
        // Truy vấn trên đã join công ty ACTIVE nên công ty chắc chắn còn.
        Company company = companyRepository.findById(job.getCompanyId())
                .orElseThrow(() -> new ResourceNotFoundException(JOB_NOT_FOUND));
        return publicJobMapper.toDetail(job, jobSkillRepository.findByJobId(job.getId()), company);
    }
}
