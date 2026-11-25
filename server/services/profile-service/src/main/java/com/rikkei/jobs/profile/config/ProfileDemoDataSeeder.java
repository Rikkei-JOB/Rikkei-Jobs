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
    private final PortfolioProjectRepository portfolio;
    private final PortfolioMediaRepository portfolioMedia;
    /** HS-03: mã kỳ thực tập giả cho dự án IOC mẫu, tới khi TT-01 đồng bộ dữ liệu thật. */
    static final UUID DEMO_INTERNSHIP_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");

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
                seedIocProject(userId);
            }
            if (candidate.lmsUserId().equals("lms-sv-003")) {
                // Đủ 5 nguồn bằng chứng để demo nhãn: LMS, EXAM, PM (INTERNSHIP) từ JSON; MENTOR và SELF tại đây.
                seedDirectEvidence(userId, "JAVA", EvidenceSource.MENTOR, "c3-mentor-java-2026-10",
                        "Mentor đánh giá code review Java (mẫu)", "68", true);
                seedDirectEvidence(userId, "SQL", EvidenceSource.SELF, "c3-self-sql",
                        "Tự đánh giá SQL (mẫu)", "70", false);
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

    /** shortcut: chỉ kiểm theo sourceRef, nên người dùng xoá bằng chứng mẫu thì lần khởi động dev sau sẽ thêm lại. */
    private void seedDirectEvidence(UUID userId, String skillCode, EvidenceSource source, String ref,
                                    String title, String score, boolean verified) {
        boolean present = evidences.findAllByUserId(userId).stream()
                .anyMatch(e -> e.getSource() == source && e.getSourceRef().equals(ref));
        if (present) return;
        UUID skillId = skills.findByCode(skillCode).orElseThrow().getId();
        SkillEvidence evidence = new SkillEvidence();
        evidence.setUserId(userId);
        evidence.setSkillId(skillId);
        evidence.setSource(source);
        evidence.setSourceRef(ref);
        evidence.setTitle(title);
        evidence.setScore(new BigDecimal(score));
        evidence.setWeight(BigDecimal.ONE);
        evidence.setRecordedAt(Instant.parse("2026-10-25T00:00:00Z"));
        evidence.setVerified(verified);
        evidences.save(evidence);
        if (source == EvidenceSource.SELF) {
            SkillScore s = scores.findById(new SkillScoreId(userId, skillId)).orElseGet(SkillScore::new);
            s.setId(new SkillScoreId(userId, skillId));
            if (s.getSelfScore() == null) s.setSelfScore(new BigDecimal(score));
            s.setUpdatedAt(Instant.now());
            scores.save(s);
        }
    }

    private void seedIocProject(UUID userId) {
        if (portfolio.existsByUserIdAndInternshipId(userId, DEMO_INTERNSHIP_ID)) return;
        PortfolioProject project = new PortfolioProject();
        project.setUserId(userId);
        project.setSource(PortfolioProject.IOC);
        project.setInternshipId(DEMO_INTERNSHIP_ID);
        project.setName("Rikkei Jobs – Cổng tuyển dụng nội bộ (IOC mẫu)");
        project.setRole("Backend Developer");
        project.setDescription("Dự án thực tập IOC mẫu (seed HS-03, thay bằng dữ liệu TT-01 khi có).");
        project.setTechStack("Java,Spring Boot,PostgreSQL,Next.js");
        project.setRepoUrl("https://github.com/Rikkei-JOB/Rikkei-Jobs");
        project.setStartDate(java.time.LocalDate.parse("2026-06-01"));
        project.setEndDate(java.time.LocalDate.parse("2026-08-31"));
        project.setVisible(true);
        project.setSortOrder(portfolio.maxSortOrder(userId) + 1);
        portfolio.save(project);
        PortfolioMedia evidence = new PortfolioMedia();
        evidence.setProjectId(project.getId());
        evidence.setType(PortfolioMedia.EVIDENCE);
        evidence.setUrl("https://github.com/Rikkei-JOB/Rikkei-Jobs/pulls");
        evidence.setCaption("Biên bản nghiệm thu thực tập (mẫu)");
        evidence.setCreatedAt(Instant.parse("2026-08-31T00:00:00Z"));
        portfolioMedia.save(evidence);
    }

    // fixtureUserId is deliberately ignored: actual UUIDs are resolved from NT-01's LMS identity mapping.
    private record DemoCandidate(String lmsUserId, UUID fixtureUserId, List<VerifiedSkillRecord> records) { }
}
