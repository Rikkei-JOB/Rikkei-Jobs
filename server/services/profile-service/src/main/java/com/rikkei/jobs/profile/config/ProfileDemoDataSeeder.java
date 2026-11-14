package com.rikkei.jobs.profile.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rikkei.jobs.profile.dto.VerifiedSkillRecord;
import com.rikkei.jobs.profile.entity.*;
import com.rikkei.jobs.profile.repository.*;
import com.rikkei.jobs.profile.service.SkillProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
@Profile("dev")
@ConditionalOnProperty(name = "app.profile.demo-data-enabled", havingValue = "true")
@RequiredArgsConstructor
public class ProfileDemoDataSeeder implements ApplicationRunner {
    private final ObjectMapper json;
    private final JdbcTemplate jdbc;
    private final ExternalSkillRecordRepository external;
    private final SkillEvidenceRepository evidences;
    private final SkillRepository skills;
    private final SkillScoreRepository scores;
    private final SkillProfileService core;
    private final SkillProfileStateRepository states;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<DemoCandidate> candidates;
        try (var input = new ClassPathResource("hs01/candidates.json").getInputStream()) {
            candidates = json.readValue(input, new TypeReference<List<DemoCandidate>>() { });
        } catch (IOException ex) {
            throw new IllegalStateException("Không đọc được dữ liệu mẫu HS-01", ex);
        }
        for (DemoCandidate candidate : candidates) {
            List<UUID> ids = jdbc.query("select id from public.users where lms_user_id = ? and role = 'CANDIDATE'",
                    (row, index) -> row.getObject(1, UUID.class), candidate.lmsUserId());
            if (ids.size() != 1) {
                throw new IllegalStateException("Chạy seed NT-01 dev trước, dùng chung database cho demo: " + candidate.lmsUserId());
            }
            UUID userId = ids.getFirst();
            for (VerifiedSkillRecord input : candidate.records()) {
                if (external.findByUserIdAndSourceAndSourceRef(userId, input.source(), input.sourceRef()).isPresent()) {
                    continue;
                }
                ExternalSkillRecord record = new ExternalSkillRecord();
                record.setUserId(userId);
                record.setSource(input.source());
                record.setSourceCode(input.sourceCode());
                record.setSourceRef(input.sourceRef());
                record.setTitle(input.title());
                record.setScore(input.score());
                record.setScoreMax(BigDecimal.valueOf(100));
                record.setRecordedAt(input.recordedAt());
                record.setVerified(true);
                external.save(record);
            }
            if (candidate.lmsUserId().equals("lms-sv-001")) {
                seedSelfOnlySkill(userId);
            }
            core.recomputeSkills(userId);
        }
    }

    private void seedSelfOnlySkill(UUID userId) {
        // Demo SELF belongs only to the first seed; never resurrect a user's removed skill.
        if (states.existsById(userId)) return;
        UUID skillId = skills.findByCode("ENGLISH").orElseThrow().getId();
        boolean present = evidences.findAllByUserId(userId).stream()
                .anyMatch(e -> e.getSource() == EvidenceSource.SELF && e.getSkillId().equals(skillId));
        if (present) {
            return;
        }
        SkillEvidence evidence = new SkillEvidence();
        evidence.setUserId(userId);
        evidence.setSkillId(skillId);
        evidence.setSource(EvidenceSource.SELF);
        evidence.setSourceRef("hs01-demo-self-english");
        evidence.setTitle("Tự đánh giá tiếng Anh (mẫu)");
        evidence.setScore(new BigDecimal("65"));
        evidence.setWeight(BigDecimal.ONE);
        evidence.setRecordedAt(Instant.parse("2026-10-20T00:00:00Z"));
        evidence.setVerified(false);
        evidences.save(evidence);
        SkillScore score = scores.findById(new SkillScoreId(userId, skillId)).orElseGet(SkillScore::new);
        score.setId(new SkillScoreId(userId, skillId));
        if (score.getSelfScore() == null) {
            score.setSelfScore(new BigDecimal("65"));
        }
        score.setUpdatedAt(Instant.now());
        scores.save(score);
    }

    // fixtureUserId is deliberately ignored: actual UUIDs are resolved from NT-01's LMS identity mapping.
    private record DemoCandidate(String lmsUserId, UUID fixtureUserId, List<VerifiedSkillRecord> records) { }
}
