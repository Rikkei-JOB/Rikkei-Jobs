package com.rikkei.jobs.profile.service;

import com.rikkei.jobs.profile.entity.*;
import com.rikkei.jobs.profile.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.datasource.url=${HS01_TEST_DB_URL:jdbc:h2:mem:hs01_external;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1}",
        "spring.datasource.username=${HS01_TEST_DB_USER:sa}",
        "spring.datasource.password=${HS01_TEST_DB_PASSWORD:}",
        "app.jwt.secret=hs01_test_secret_at_least_32_bytes_long",
        "logging.level.root=WARN", "debug=false"
})
class ExternalRecordRecomputeTest {
    @Autowired SkillProfileService service;
    @Autowired ExternalSkillRecordRepository external;
    @Autowired SkillEvidenceRepository evidences;
    @Autowired SkillRepository skills;
    @Autowired SkillScoreRepository scores;

    @Test
    void recomputeReadsStoredVerifiedRecordsAndNormalizesTenPointScale() {
        UUID user = UUID.randomUUID();
        external.saveAndFlush(record(user, "exam-1", "8.5", true));
        external.saveAndFlush(record(user, "pending-exam", "10", false));

        var result = service.recomputeSkills(user);
        assertThat(result).singleElement().satisfies(view -> {
            assertThat(view.skill().code()).isEqualTo("JAVA");
            assertThat(view.verifiedScore()).isEqualByComparingTo("85");
            assertThat(view.level()).isEqualTo(SkillLevel.EXCELLENT);
            assertThat(view.evidences()).hasSize(1);
        });
        assertThat(evidences.findAllByUserId(user)).hasSize(1);
    }

    @Test
    void correctionRevocationAndDeletionReplaceStaleImportedEvidence() {
        UUID user = UUID.randomUUID();
        ExternalSkillRecord first = external.saveAndFlush(record(user, "exam-1", "8", true));
        ExternalSkillRecord second = external.saveAndFlush(record(user, "exam-2", "6", true));
        assertThat(service.recomputeSkills(user).getFirst().verifiedScore()).isEqualByComparingTo("70");

        first.setScore(new BigDecimal("10"));
        external.saveAndFlush(first);
        assertThat(service.recomputeSkills(user).getFirst().verifiedScore()).isEqualByComparingTo("80");
        second.setVerified(false);
        external.saveAndFlush(second);
        assertThat(service.recomputeSkills(user).getFirst().verifiedScore()).isEqualByComparingTo("100");
        assertThat(evidences.findAllByUserId(user)).hasSize(1);

        external.deleteById(first.getId());
        assertThat(service.recomputeSkills(user)).isEmpty();
        assertThat(evidences.findAllByUserId(user)).isEmpty();
    }

    @Test
    void removingMappingRemovesPreviouslyMappedEvidence() {
        UUID user = UUID.randomUUID();
        ExternalSkillRecord record = external.saveAndFlush(record(user, "exam-1", "8", true));
        service.recomputeSkills(user);
        record.setSourceCode("NO_LONGER_MAPPED");
        external.saveAndFlush(record);

        assertThat(service.recomputeSkills(user)).isEmpty();
        assertThat(evidences.findAllByUserId(user)).isEmpty();
    }

    @Test
    void sourceRefreshPreservesSelfEvidenceAndSeparateSelfScore() {
        UUID user = UUID.randomUUID();
        UUID skillId = skills.findByCode("ENGLISH").orElseThrow().getId();
        SkillEvidence self = new SkillEvidence();
        self.setUserId(user);
        self.setSkillId(skillId);
        self.setSource(EvidenceSource.SELF);
        self.setSourceRef("self-english");
        self.setTitle("Tự đánh giá tiếng Anh");
        self.setScore(new BigDecimal("65"));
        self.setWeight(BigDecimal.ONE);
        self.setRecordedAt(Instant.parse("2026-10-01T00:00:00Z"));
        evidences.saveAndFlush(self);
        SkillScore score = new SkillScore();
        score.setId(new SkillScoreId(user, skillId));
        score.setSelfScore(new BigDecimal("65"));
        score.setUpdatedAt(Instant.parse("2026-10-01T00:00:00Z"));
        scores.saveAndFlush(score);

        var view = service.recomputeSkills(user).getFirst();
        assertThat(view.verifiedScore()).isNull();
        assertThat(view.level()).isNull();
        assertThat(view.selfScore()).isEqualByComparingTo("65");
        assertThat(view.evidences()).singleElement().satisfies(e -> assertThat(e.verified()).isFalse());
    }

    @Test
    void concurrentRecomputeForOneCandidateDoesNotDuplicateOrLoseEvidence() {
        UUID user = UUID.randomUUID();
        external.saveAndFlush(record(user, "concurrent-1", "8", true));
        var first = CompletableFuture.runAsync(() -> service.recomputeSkills(user));
        var second = CompletableFuture.runAsync(() -> service.recomputeSkills(user));
        CompletableFuture.allOf(first, second).join();

        assertThat(evidences.findAllByUserId(user)).hasSize(1);
        assertThat(service.getSkills(user)).singleElement()
                .satisfies(v -> assertThat(v.verifiedScore()).isEqualByComparingTo("80"));
    }

    @Test
    void refreshDoesNotLoadAnotherCandidatesResults() {
        UUID user = UUID.randomUUID();
        external.saveAndFlush(record(UUID.randomUUID(), "other-exam", "10", true));
        assertThat(service.recomputeSkills(user)).isEmpty();
    }

    private ExternalSkillRecord record(UUID user, String reference, String rawScore, boolean verified) {
        ExternalSkillRecord result = new ExternalSkillRecord();
        result.setUserId(user);
        result.setSource(MappingSource.EXAM);
        result.setSourceCode("JAVA01");
        result.setSourceRef(reference);
        result.setTitle("Thi Java");
        result.setScore(new BigDecimal(rawScore));
        result.setScoreMax(BigDecimal.TEN);
        result.setRecordedAt(Instant.parse("2026-10-20T00:00:00Z"));
        result.setVerified(verified);
        return result;
    }
}
