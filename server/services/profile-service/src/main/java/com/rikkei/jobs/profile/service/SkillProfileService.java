package com.rikkei.jobs.profile.service;

import com.rikkei.jobs.profile.dto.SkillProfileView;
import com.rikkei.jobs.profile.dto.SkillProfileView.EvidenceView;
import com.rikkei.jobs.profile.dto.SkillProfileView.SkillInfo;
import com.rikkei.jobs.profile.dto.VerifiedSkillRecord;
import com.rikkei.jobs.profile.entity.*;
import com.rikkei.jobs.profile.repository.*;
import com.rikkei.jobs.profile.service.SkillScoreCalculator.ScoreEvidence;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;

import java.time.Clock;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Calculation/import business logic; HTTP access is checked by SkillProfileAccessService. */
@Service
@Transactional(readOnly = true)
public class SkillProfileService {
    private final SkillRepository skills;
    private final SkillMappingRepository mappings;
    private final SkillEvidenceRepository evidences;
    private final SkillScoreRepository scores;
    private final SkillScoreCalculator calculator;
    private final Clock clock;
    private final ExternalSkillRecordRepository external;
    private final SkillProfileStateRepository states;

    public SkillProfileService(SkillRepository skills, SkillMappingRepository mappings,
                               SkillEvidenceRepository evidences, SkillScoreRepository scores,
                               SkillScoreCalculator calculator,
                               @Qualifier("skillProfileClock") Clock clock,
                               ExternalSkillRecordRepository external, SkillProfileStateRepository states) {
        this.skills = skills;
        this.mappings = mappings;
        this.evidences = evidences;
        this.scores = scores;
        this.calculator = calculator;
        this.clock = clock;
        this.external = external;
        this.states = states;
    }

    /** Incremental import: absent records are retained; corrections use the same stable sourceRef. */
    @Transactional
    public List<SkillProfileView> importVerifiedRecords(UUID userId, List<VerifiedSkillRecord> records) {
        requireUser(userId);
        if (records == null || records.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("Verified records must not contain null");
        }
        lockProfile(userId);
        applyRecords(userId, records, Map.of());
        return recomputeFromEvidences(userId);
    }

    private Set<EvidenceKey> applyRecords(UUID userId, List<VerifiedSkillRecord> records,
                                          Map<SourceReference, UUID> externalIds) {
        Map<MappingKey, List<SkillMapping>> mappingBySource = mappings.findAll().stream()
                .collect(Collectors.groupingBy(m -> new MappingKey(m.getSource(), m.getSourceCode())));
        Map<EvidenceKey, SkillEvidence> existing = evidences.findAllByUserId(userId).stream()
                .collect(Collectors.toMap(SkillProfileService::key, Function.identity()));
        Map<EvidenceKey, SkillEvidence> changed = new LinkedHashMap<>();

        for (VerifiedSkillRecord record : records) {
            for (SkillMapping mapping : mappingBySource.getOrDefault(
                    new MappingKey(record.source(), record.sourceCode()), List.of())) {
                EvidenceSource source = switch (record.source()) {
                    case LMS -> EvidenceSource.LMS;
                    case EXAM -> EvidenceSource.EXAM;
                    case PM -> EvidenceSource.INTERNSHIP;
                };
                EvidenceKey key = new EvidenceKey(mapping.getSkillId(), source, record.sourceRef());
                SkillEvidence evidence = existing.computeIfAbsent(key, ignored -> new SkillEvidence());
                evidence.setUserId(userId);
                evidence.setSkillId(mapping.getSkillId());
                evidence.setSource(source);
                evidence.setSourceRef(record.sourceRef());
                evidence.setTitle(record.title());
                evidence.setScore(record.score());
                evidence.setWeight(mapping.getWeight());
                evidence.setRecordedAt(record.recordedAt());
                evidence.setVerified(true);
                UUID externalId = externalIds.get(new SourceReference(record.source(), record.sourceRef()));
                if (externalId != null) {
                    evidence.setExternalRecordId(externalId);
                }
                changed.put(key, evidence);
            }
        }
        evidences.saveAll(changed.values());
        return changed.keySet();
    }

    @Transactional
    public List<SkillProfileView> recomputeSkills(UUID userId) {
        requireUser(userId);
        lockProfile(userId);
        List<ExternalSkillRecord> inputs = external.findAllByUserIdAndVerifiedTrue(userId);
        Map<SourceReference, UUID> externalIds = inputs.stream().collect(Collectors.toMap(
                r -> new SourceReference(r.getSource(), r.getSourceRef()), ExternalSkillRecord::getId));
        Set<EvidenceKey> active = applyRecords(userId,
                inputs.stream().map(ExternalSkillRecord::normalized).toList(), externalIds);
        evidences.deleteAll(evidences.findAllByUserId(userId).stream()
                .filter(e -> e.getExternalRecordId() != null && !active.contains(key(e))).toList());
        return recomputeFromEvidences(userId);
    }

    private List<SkillProfileView> recomputeFromEvidences(UUID userId) {
        Instant now = clock.instant();
        Map<UUID, List<SkillEvidence>> bySkill = evidences.findAllByUserId(userId).stream()
                .collect(Collectors.groupingBy(SkillEvidence::getSkillId));
        Map<UUID, SkillScore> existing = scores.findAllByIdUserId(userId).stream()
                .collect(Collectors.toMap(s -> s.getId().getSkillId(), Function.identity()));
        Set<UUID> skillIds = new HashSet<>(existing.keySet());
        skillIds.addAll(bySkill.keySet());
        List<SkillScore> changed = new ArrayList<>();

        for (UUID skillId : skillIds) {
            SkillScore score = existing.getOrDefault(skillId, new SkillScore());
            List<SkillEvidence> inputs = bySkill.getOrDefault(skillId, List.of());
            if (inputs.isEmpty() && score.getSelfScore() == null) {
                scores.delete(score);
                continue;
            }
            var result = calculator.calculate(inputs.stream().map(e -> new ScoreEvidence(
                    e.getScore(), e.getWeight(), e.getRecordedAt(),
                    e.isVerified() && e.getSource() != EvidenceSource.SELF)).toList(), now);
            score.setId(new SkillScoreId(userId, skillId));
            score.setVerifiedScore(result.verifiedScore());
            score.setLevel(result.level());
            score.setUpdatedAt(now);
            changed.add(score);
        }
        scores.saveAll(changed);
        return getSkills(userId);
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public List<SkillProfileView> getSkills(UUID userId) {
        requireUser(userId);
        Map<UUID, SkillScore> bySkill = scores.findAllByIdUserId(userId).stream()
                .collect(Collectors.toMap(s -> s.getId().getSkillId(), Function.identity()));
        Map<UUID, List<SkillEvidence>> evidenceBySkill = evidences.findAllByUserId(userId).stream()
                .collect(Collectors.groupingBy(SkillEvidence::getSkillId));
        Set<UUID> skillIds = new HashSet<>(bySkill.keySet());
        skillIds.addAll(evidenceBySkill.keySet());
        return skills.findAllById(skillIds).stream()
                .sorted(Comparator.comparing(Skill::getGroup).thenComparing(Skill::getCode))
                .map(skill -> {
                    SkillScore score = bySkill.get(skill.getId());
                    List<EvidenceView> details = evidenceBySkill.getOrDefault(skill.getId(), List.of()).stream()
                            .sorted(Comparator.comparing(SkillEvidence::getRecordedAt).reversed()
                                    .thenComparing(e -> e.getSource().name()).thenComparing(SkillEvidence::getSourceRef))
                            .map(e -> new EvidenceView(e.getSource(), e.getTitle(), e.getScore(),
                                    e.getRecordedAt(), e.isVerified())).toList();
                    return new SkillProfileView(new SkillInfo(skill.getId(), skill.getCode(),
                            skill.getName(), skill.getGroup()), score == null ? null : score.getVerifiedScore(),
                            score == null ? null : score.getSelfScore(), score == null ? null : score.getLevel(), details);
                }).toList();
    }

    private static void requireUser(UUID userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId is required");
        }
    }

    private void lockProfile(UUID userId) {
        states.ensureExists(userId);
        states.lockByUserId(userId).orElseThrow();
    }

    private static EvidenceKey key(SkillEvidence evidence) {
        return new EvidenceKey(evidence.getSkillId(), evidence.getSource(), evidence.getSourceRef());
    }

    private record MappingKey(MappingSource source, String code) { }
    private record EvidenceKey(UUID skillId, EvidenceSource source, String reference) { }
    private record SourceReference(MappingSource source, String reference) { }
}
