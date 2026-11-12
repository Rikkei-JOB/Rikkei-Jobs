package com.rikkei.jobs.profile.service.sync;

import com.rikkei.jobs.profile.entity.ExternalSkillRecord;
import com.rikkei.jobs.profile.entity.ManualOverride;
import com.rikkei.jobs.profile.entity.OverrideField;
import com.rikkei.jobs.profile.repository.ExternalSkillRecordRepository;
import com.rikkei.jobs.profile.repository.ManualOverrideRepository;
import com.rikkei.jobs.profile.repository.SkillProfileStateRepository;
import com.rikkei.jobs.profile.service.SkillProfileService;
import java.time.Instant;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ghi kết quả đã lấy về vào external_records trong một transaction rồi gọi recomputeSkills của HS-01.
 * Upsert theo (source, source_key) nên đồng bộ nhiều lần không sinh bản ghi trùng; trường đã sửa tay giữ nguyên.
 */
@Component
@RequiredArgsConstructor
public class SyncApplier {
    private final ExternalSkillRecordRepository external;
    private final ManualOverrideRepository overrides;
    private final SkillProfileStateRepository states;
    private final SkillProfileService skills;

    @Transactional
    public SyncCounts apply(UUID userId, List<SourceRecord> records, int fetched, int skipped, Instant now) {
        // Khoá hồ sơ trước khi đọc để hai lần đồng bộ cùng ứng viên (nút "Đồng bộ lại" và đồng bộ nền) chạy lần lượt.
        states.ensureExists(userId);
        states.lockByUserId(userId).orElseThrow();

        Map<String, ExternalSkillRecord> existing = external.findAllByUserIdAndSourceKeyNotNull(userId).stream()
                .collect(Collectors.toMap(r -> r.getSource() + "|" + r.getSourceKey(), Function.identity()));
        Map<UUID, Set<OverrideField>> locked = new HashMap<>();
        for (ManualOverride override : overrides.findAllByRecordIdInOrderByCreatedAtDesc(
                existing.values().stream().map(ExternalSkillRecord::getId).toList())) {
            locked.computeIfAbsent(override.getRecordId(), id -> EnumSet.noneOf(OverrideField.class))
                    .add(override.getField());
        }

        int inserted = 0;
        int updated = 0;
        int unchanged = 0;
        for (SourceRecord source : records) {
            ExternalSkillRecord record = existing.get(source.source() + "|" + source.sourceKey());
            if (record == null) {
                external.save(newRecord(userId, source, now));
                inserted++;
            } else if (update(record, source, locked.getOrDefault(record.getId(), Set.of()), now)) {
                updated++;
            } else {
                unchanged++;
            }
        }
        external.flush();
        skills.recomputeSkills(userId);
        return new SyncCounts(fetched, inserted, updated, unchanged, skipped);
    }

    private static ExternalSkillRecord newRecord(UUID userId, SourceRecord source, Instant now) {
        ExternalSkillRecord record = new ExternalSkillRecord();
        record.setUserId(userId);
        record.setSource(source.source());
        record.setSourceKey(source.sourceKey());
        record.setSourceRef(source.sourceKey());
        record.setSourceCode(source.sourceCode());
        record.setTitle(source.title());
        record.setScore(source.score());
        record.setScoreMax(source.scoreMax());
        record.setRecordedAt(source.recordedAt() == null ? now : source.recordedAt());
        record.setVerified(true);
        record.setPayload(source.payload());
        record.setSyncedAt(now);
        return record;
    }

    /** Cập nhật các trường không bị sửa tay; trả true nếu có gì thay đổi. synced_at luôn được ghi. */
    private static boolean update(ExternalSkillRecord record, SourceRecord source, Set<OverrideField> locked,
                                  Instant now) {
        boolean changed = false;
        if (!locked.contains(OverrideField.TITLE) && !record.getTitle().equals(source.title())) {
            record.setTitle(source.title());
            changed = true;
        }
        if (!locked.contains(OverrideField.SCORE) && record.getScore().compareTo(source.score()) != 0) {
            record.setScore(source.score());
            if (source.recordedAt() == null) {
                // Nguồn không có ngày (LMS): điểm đổi thì coi là kết quả mới ghi nhận lúc này.
                record.setRecordedAt(now);
            }
            changed = true;
        }
        if (record.getScoreMax().compareTo(source.scoreMax()) != 0) {
            record.setScoreMax(source.scoreMax());
            changed = true;
        }
        if (!record.getSourceCode().equals(source.sourceCode())) {
            record.setSourceCode(source.sourceCode());
            changed = true;
        }
        if (source.recordedAt() != null && !source.recordedAt().equals(record.getRecordedAt())) {
            record.setRecordedAt(source.recordedAt());
            changed = true;
        }
        if (!Objects.equals(record.getPayload(), source.payload())) {
            record.setPayload(source.payload());
            changed = true;
        }
        if (!record.isVerified()) {
            record.setVerified(true);
            changed = true;
        }
        record.setSyncedAt(now);
        return changed;
    }
}
