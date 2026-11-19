package com.rikkei.jobs.company.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rikkei.jobs.common.config.exception.ResourceNotFoundException;
import com.rikkei.jobs.common.config.security.principle.AuthPrincipal;
import com.rikkei.jobs.common.config.security.principle.Role;
import com.rikkei.jobs.company.client.ApplicationClient;
import com.rikkei.jobs.company.client.CatalogSkill;
import com.rikkei.jobs.company.client.ProfileSkillClient;
import com.rikkei.jobs.company.client.SkillCatalogClient;
import com.rikkei.jobs.company.dto.response.MatchResponse;
import com.rikkei.jobs.company.dto.response.MatchResponse.SkillMatchResponse;
import com.rikkei.jobs.company.entity.Company;
import com.rikkei.jobs.company.entity.Job;
import com.rikkei.jobs.company.entity.JobSkill;
import com.rikkei.jobs.company.entity.JobStatus;
import com.rikkei.jobs.company.entity.MatchScore;
import com.rikkei.jobs.company.entity.MatchScoreId;
import com.rikkei.jobs.company.repository.JobRepository;
import com.rikkei.jobs.company.repository.JobSkillRepository;
import com.rikkei.jobs.company.repository.MatchScoreRepository;
import com.rikkei.jobs.company.service.CompanyService;
import com.rikkei.jobs.company.service.MatchService;
import com.rikkei.jobs.company.util.MatchCalculator;
import com.rikkei.jobs.company.util.MatchCalculator.JobSkillInput;
import com.rikkei.jobs.company.util.MatchCalculator.MatchResult;
import com.rikkei.jobs.company.util.MatchCalculator.SkillMatch;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/**
 * Cố ý không dùng @Transactional ở mức method: các lời gọi HTTP (profile, recruitment, danh mục) không được chạy
 * trong transaction DB; mỗi lời gọi repository tự có transaction riêng.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MatchServiceImpl implements MatchService {

    private static final String NOT_FOUND = "Không tìm thấy JD";

    private final JobRepository jobRepository;
    private final JobSkillRepository jobSkillRepository;
    private final MatchScoreRepository matchScoreRepository;
    private final CompanyService companyService;
    private final SkillCatalogClient skillCatalog;
    private final ProfileSkillClient profileSkills;
    private final ApplicationClient applications;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    /** Dữ liệu vào của một kỹ năng JD; verifiedScore null = ứng viên chưa có điểm xác thực. */
    record Input(UUID skillId, boolean required, int minScore, BigDecimal verifiedScore) {
    }

    record Item(UUID skillId, String skill, boolean required, int minScore, BigDecimal candidateScore) {
    }

    record Detail(List<Input> inputs, boolean belowMinimum, List<Item> met, List<Item> missing) {
    }

    @Override
    public MatchResponse forCandidate(UUID jobId, UUID candidateId, String authorization) {
        Job job = jobRepository.findById(jobId).orElseThrow(() -> new ResourceNotFoundException(NOT_FOUND));
        if (job.getStatus() != JobStatus.APPROVED) {
            throw new ResourceNotFoundException(NOT_FOUND);
        }
        return getOrCompute(job, candidateId, authorization);
    }

    @Override
    public MatchResponse forViewer(UUID jobId, UUID userId, AuthPrincipal viewer, String authorization) {
        Job job = jobRepository.findById(jobId).orElseThrow(() -> new ResourceNotFoundException(NOT_FOUND));
        if (viewer.getRole() == Role.HR) {
            Company company = companyService.findMine(viewer.getUserId());
            if (!company.getId().equals(job.getCompanyId())
                    || !applications.hasApplied(jobId, userId, authorization)) {
                throw new ResourceNotFoundException(NOT_FOUND);
            }
        }
        return getOrCompute(job, userId, authorization);
    }

    private MatchResponse getOrCompute(Job job, UUID userId, String authorization) {
        List<JobSkill> jobSkills = jobSkillRepository.findByJobId(job.getId());
        Map<UUID, BigDecimal> verified = profileSkills.verifiedScores(userId, authorization);
        List<Input> inputs = jobSkills.stream()
                .map(s -> new Input(s.getSkillId(), s.isRequired(), s.getMinScore(), verified.get(s.getSkillId())))
                .sorted(Comparator.comparing(Input::skillId))
                .toList();

        Optional<MatchScore> cached = matchScoreRepository.findById(new MatchScoreId(job.getId(), userId));
        if (cached.isPresent() && !cached.get().isStale()) {
            Optional<MatchResponse> hit = fromCache(cached.get(), inputs);
            if (hit.isPresent()) {
                return hit.get();
            }
        }

        Map<UUID, String> names = new HashMap<>();
        for (CatalogSkill skill : skillCatalog.skills(authorization)) {
            if (skill.name() != null && !skill.name().isBlank()) {
                names.put(skill.id(), skill.name());
            }
        }
        List<JobSkillInput> calcInputs = jobSkills.stream()
                .map(s -> new JobSkillInput(s.getSkillId(),
                        names.getOrDefault(s.getSkillId(), s.getSkillId().toString()),
                        s.isRequired(), s.getMinScore()))
                .toList();
        MatchResult result = MatchCalculator.calculate(calcInputs, verified);

        Detail detail = new Detail(inputs, result.belowMinimum(), items(result.met()), items(result.missing()));
        save(job.getId(), userId, result.score(), detail);
        return toResponse(result.score(), detail);
    }

    private Optional<MatchResponse> fromCache(MatchScore row, List<Input> inputs) {
        Detail detail;
        try {
            detail = objectMapper.readValue(row.getDetail(), Detail.class);
        } catch (JsonProcessingException | RuntimeException e) {
            log.debug("Không đọc được detail của match_scores, tính lại", e);
            return Optional.empty();
        }
        if (detail == null || !sameInputs(detail.inputs(), inputs)) {
            return Optional.empty();
        }
        return Optional.of(toResponse(row.getScore(), detail));
    }

    private static boolean sameInputs(List<Input> stored, List<Input> current) {
        if (stored == null || stored.size() != current.size()) {
            return false;
        }
        List<Input> sorted = stored.stream().sorted(Comparator.comparing(Input::skillId)).toList();
        for (int i = 0; i < sorted.size(); i++) {
            Input a = sorted.get(i);
            Input b = current.get(i);
            if (!Objects.equals(a.skillId(), b.skillId()) || a.required() != b.required()
                    || a.minScore() != b.minScore() || !sameScore(a.verifiedScore(), b.verifiedScore())) {
                return false;
            }
        }
        return true;
    }

    private static boolean sameScore(BigDecimal a, BigDecimal b) {
        return a == null || b == null ? a == b : a.compareTo(b) == 0;
    }

    private void save(UUID jobId, UUID userId, int score, Detail detail) {
        try {
            String json = objectMapper.writeValueAsString(detail);
            matchScoreRepository.save(new MatchScore(jobId, userId, score, json, false, Instant.now(clock)));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Không ghi được detail của match_scores", e);
        } catch (DataIntegrityViolationException e) {
            log.debug("Xung đột khi lưu match_scores, vẫn trả kết quả vừa tính", e);
        }
    }

    private static List<Item> items(List<SkillMatch> matches) {
        return matches.stream()
                .map(m -> new Item(m.skillId(), m.skill(), m.required(), m.minScore(), m.candidateScore()))
                .toList();
    }

    private static MatchResponse toResponse(int score, Detail detail) {
        return new MatchResponse(score, detail.belowMinimum(), responses(detail.met()), responses(detail.missing()));
    }

    private static List<SkillMatchResponse> responses(List<Item> items) {
        return (items == null ? List.<Item>of() : items).stream()
                .map(i -> new SkillMatchResponse(i.skill(), i.required(), i.minScore(), i.candidateScore()))
                .toList();
    }
}
