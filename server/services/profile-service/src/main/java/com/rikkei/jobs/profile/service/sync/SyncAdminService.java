package com.rikkei.jobs.profile.service.sync;

import com.rikkei.jobs.common.config.exception.ResourceNotFoundException;
import com.rikkei.jobs.profile.dto.sync.SyncDtos.CandidateSyncView;
import com.rikkei.jobs.profile.dto.sync.SyncDtos.ExternalRecordView;
import com.rikkei.jobs.profile.dto.sync.SyncDtos.OverrideRequest;
import com.rikkei.jobs.profile.dto.sync.SyncDtos.OverrideView;
import com.rikkei.jobs.profile.dto.sync.SyncDtos.PageView;
import com.rikkei.jobs.profile.dto.sync.SyncDtos.SyncRunView;
import com.rikkei.jobs.profile.entity.ExternalSkillRecord;
import com.rikkei.jobs.profile.entity.ManualOverride;
import com.rikkei.jobs.profile.entity.OverrideField;
import com.rikkei.jobs.profile.entity.SyncRun;
import com.rikkei.jobs.profile.entity.SyncStatus;
import com.rikkei.jobs.profile.exception.FieldValidationException;
import com.rikkei.jobs.profile.repository.ExternalSkillRecordRepository;
import com.rikkei.jobs.profile.repository.ManualOverrideRepository;
import com.rikkei.jobs.profile.repository.SkillProfileStateRepository;
import com.rikkei.jobs.profile.repository.SyncRunRepository;
import com.rikkei.jobs.profile.service.SkillProfileService;
import com.rikkei.jobs.profile.service.sync.CandidateDirectory.Candidate;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Phần đọc và sửa tay của màn "Đồng bộ dữ liệu" (STAFF). */
@Service
@RequiredArgsConstructor
public class SyncAdminService {
    private static final int SEARCH_LIMIT = 20;

    private final CandidateDirectory directory;
    private final SyncRunRepository runs;
    private final ExternalSkillRecordRepository external;
    private final ManualOverrideRepository overrides;
    private final SkillProfileStateRepository states;
    private final SkillProfileService skills;
    private final Clock clock;
    private final EntityManager entityManager;

    @Transactional(readOnly = true)
    public PageView<SyncRunView> listRuns(SyncStatus status, UUID userId, int page, int size) {
        if (page < 1) {
            throw new FieldValidationException("page", "page phải từ 1");
        }
        if (size < 1 || size > 100) {
            throw new FieldValidationException("size", "size phải từ 1 đến 100");
        }
        Page<SyncRun> result = runs.search(status, userId, PageRequest.of(page - 1, size,
                Sort.by(Sort.Order.desc("startedAt"), Sort.Order.desc("finishedAt"))));
        return new PageView<>(views(result.getContent()), result.getTotalElements(), page, size);
    }

    @Transactional(readOnly = true)
    public List<CandidateSyncView> searchCandidates(String query) {
        List<Candidate> candidates = directory.search(query, SEARCH_LIMIT);
        Map<UUID, SyncRun> latest = runs.findLatestByUserIds(candidates.stream().map(Candidate::userId).toList())
                .stream().collect(Collectors.toMap(SyncRun::getUserId, Function.identity(), (a, b) -> a));
        return candidates.stream().map(c -> new CandidateSyncView(c.userId(), c.fullName(), c.email(), c.lmsUserId(),
                latest.containsKey(c.userId()) ? view(latest.get(c.userId()), c) : null)).toList();
    }

    public SyncRunView view(SyncRun run) {
        return views(List.of(run)).getFirst();
    }

    @Transactional(readOnly = true)
    public List<ExternalRecordView> listRecords(UUID userId) {
        directory.find(userId).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ứng viên"));
        List<ExternalSkillRecord> records = external.findAllByUserIdOrderBySourceAscSourceCodeAscSourceRefAsc(userId);
        Map<UUID, List<ManualOverride>> byRecord = overrides.findAllByRecordIdInOrderByCreatedAtDesc(
                records.stream().map(ExternalSkillRecord::getId).toList()).stream()
                .collect(Collectors.groupingBy(ManualOverride::getRecordId));
        return records.stream().map(r -> view(r, byRecord.getOrDefault(r.getId(), List.of()))).toList();
    }

    /** Cán bộ sửa tay một trường: ghi lịch sử, áp giá trị vào bản ghi, tính lại kỹ năng HS-01. */
    @Transactional
    public ExternalRecordView override(UUID recordId, OverrideRequest request, UUID staffId) {
        OverrideField field = OverrideField.fromKey(request.field().trim())
                .orElseThrow(() -> new FieldValidationException("field", "Chỉ sửa được score hoặc title"));
        ExternalSkillRecord found = external.findById(recordId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bản ghi"));
        states.ensureExists(found.getUserId());
        states.lockByUserId(found.getUserId()).orElseThrow();
        // Đọc lại sau khi khoá để không ghi đè thay đổi của một lần đồng bộ vừa chạy xong.
        entityManager.refresh(found);
        ExternalSkillRecord record = found;

        String value = request.value().trim();
        switch (field) {
            case SCORE -> {
                BigDecimal score = parseScore(value, record.getScoreMax());
                record.setScore(score);
                value = score.toPlainString();
            }
            case TITLE -> record.setTitle(value);
        }

        ManualOverride override = new ManualOverride();
        override.setRecordId(recordId);
        override.setField(field);
        override.setValue(value);
        override.setReason(request.reason().trim());
        override.setStaffId(staffId);
        override.setCreatedAt(clock.instant().truncatedTo(ChronoUnit.MICROS));
        overrides.save(override);
        external.saveAndFlush(record);
        skills.recomputeSkills(record.getUserId());
        return view(record, overrides.findAllByRecordIdInOrderByCreatedAtDesc(List.of(recordId)));
    }

    private static BigDecimal parseScore(String value, BigDecimal scoreMax) {
        BigDecimal score;
        try {
            score = new BigDecimal(value);
        } catch (NumberFormatException e) {
            throw new FieldValidationException("value", "Điểm phải là số");
        }
        if (score.signum() < 0 || score.compareTo(scoreMax) > 0) {
            throw new FieldValidationException("value", "Điểm phải từ 0 đến " + scoreMax.stripTrailingZeros().toPlainString());
        }
        if (score.stripTrailingZeros().scale() > 2) {
            throw new FieldValidationException("value", "Điểm có tối đa 2 chữ số thập phân");
        }
        return score.stripTrailingZeros().scale() < 0 ? score.setScale(0) : score.stripTrailingZeros();
    }

    private List<SyncRunView> views(Collection<SyncRun> items) {
        Set<UUID> userIds = items.stream().map(SyncRun::getUserId).filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<UUID, Candidate> candidates = directory.findAll(userIds);
        return items.stream().map(r -> view(r, r.getUserId() == null ? null : candidates.get(r.getUserId()))).toList();
    }

    private static SyncRunView view(SyncRun run, Candidate candidate) {
        return new SyncRunView(run.getId(), run.getTrigger(), run.getUserId(),
                candidate == null ? null : candidate.fullName(), candidate == null ? null : candidate.lmsUserId(),
                run.getStatus(), run.getStartedAt(), run.getFinishedAt(), run.getError(), run.getCounts(),
                run.getTriggeredBy());
    }

    private static ExternalRecordView view(ExternalSkillRecord record, List<ManualOverride> history) {
        List<String> fields = history.stream().map(o -> o.getField().key()).distinct().sorted().toList();
        return new ExternalRecordView(record.getId(), record.getSource(), record.getSourceKey(), record.getSourceCode(),
                record.getTitle(), record.getScore(), record.getScoreMax(), record.getRecordedAt(), record.getSyncedAt(),
                record.isVerified(), record.getPayload(), fields,
                history.stream().map(o -> new OverrideView(o.getField().key(), o.getValue(), o.getReason(),
                        o.getStaffId(), o.getCreatedAt())).toList());
    }
}
