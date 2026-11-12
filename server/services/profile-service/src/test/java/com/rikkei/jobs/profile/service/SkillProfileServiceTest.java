package com.rikkei.jobs.profile.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rikkei.jobs.profile.dto.SkillProfileView;
import com.rikkei.jobs.profile.dto.VerifiedSkillRecord;
import com.rikkei.jobs.profile.entity.*;
import com.rikkei.jobs.profile.repository.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;

@DataJpaTest(showSql = false, properties = {
        "spring.datasource.url=jdbc:h2:mem:hs01;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.sql.init.mode=never",
        "spring.jpa.show-sql=false",
        "debug=false",
        "logging.level.root=WARN",
        "logging.level.org.hibernate.SQL=WARN",
        "logging.level.org.hibernate.engine.jdbc.spi.SqlExceptionHelper=OFF"
})
@org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase(replace =
        org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE)
@Import({SkillProfileService.class, SkillProfileServiceTest.FixedClockConfiguration.class, SkillScoreCalculator.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class SkillProfileServiceTest {
    private static final Instant RECORDED_AT = Instant.parse("2026-10-20T08:00:00Z");

    @Autowired SkillProfileService service;
    @Autowired SkillRepository skills;
    @Autowired SkillMappingRepository mappings;
    @Autowired SkillEvidenceRepository evidences;
    @Autowired SkillScoreRepository scores;
    @Autowired Flyway flyway;
    @SpyBean SkillScoreCalculator calculator;

    @TestConfiguration
    static class FixedClockConfiguration {
        @Bean
        Clock skillProfileClock() {
            return Clock.fixed(Instant.parse("2026-11-11T00:00:00Z"), ZoneOffset.UTC);
        }
    }

    @Test
    void mapsOneCourseToMultipleSkillsUsingDatabaseWeights() {
        UUID user = UUID.randomUUID();
        var result = service.importVerifiedRecords(user, List.of(record("JAVA01", "java-1", "85")));

        assertThat(result).extracting(v -> v.skill().code()).containsExactlyInAnyOrder("JAVA", "SQL");
        assertThat(find(result, "JAVA").verifiedScore()).isEqualByComparingTo("85");
        assertThat(find(result, "SQL").evidences()).singleElement().satisfies(e -> {
            assertThat(e.source()).isEqualTo(EvidenceSource.LMS);
            assertThat(e.title()).isEqualTo("Kết quả JAVA01");
            assertThat(e.recordedAt()).isEqualTo(RECORDED_AT);
            assertThat(e.verified()).isTrue();
        });
        assertThat(evidences.findAllByUserId(user)).extracting(SkillEvidence::getWeight)
                .usingComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .containsExactlyInAnyOrder(new BigDecimal("0.7"), new BigDecimal("0.3"));
    }

    @Test
    void addingAndUpdatingResultsRecomputesWithoutDuplicatingEvidence() {
        UUID user = UUID.randomUUID();
        service.importVerifiedRecords(user, List.of(record("JAVA01", "java-1", "80")));
        service.importVerifiedRecords(user, List.of(record("JAVA01", "java-1", "80")));
        var added = service.importVerifiedRecords(user, List.of(record("SQL01", "sql-1", "60")));

        // SQL: (80 * .3 + 60 * 1) / (.3 + 1) = 64.615385.
        assertThat(find(added, "SQL").verifiedScore()).isEqualByComparingTo("64.615385");
        assertThat(evidences.findAllByUserId(user)).hasSize(3);

        var updated = service.importVerifiedRecords(user, List.of(record("SQL01", "sql-1", "100")));
        assertThat(find(updated, "SQL").verifiedScore()).isEqualByComparingTo("95.384615");
        assertThat(evidences.findAllByUserId(user)).hasSize(3);
        assertThat(service.recomputeSkills(user)).isEqualTo(service.getSkills(user));
    }

    @Test
    void neverUpdatesAnotherCandidatesEvidenceWithTheSameExternalReference() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        service.importVerifiedRecords(first, List.of(record("JAVA01", "same-ref", "90")));
        service.importVerifiedRecords(second, List.of(record("JAVA01", "same-ref", "50")));

        assertThat(find(service.getSkills(first), "JAVA").verifiedScore()).isEqualByComparingTo("90");
        assertThat(find(service.getSkills(second), "JAVA").verifiedScore()).isEqualByComparingTo("50");
    }

    @Test
    void recomputePreservesSelfScoreAndNeverVerifiesSelfEvidence() {
        UUID user = UUID.randomUUID();
        Skill git = skills.findByCode("GIT").orElseThrow();
        SkillEvidence self = new SkillEvidence();
        self.setUserId(user);
        self.setSkillId(git.getId());
        self.setSource(EvidenceSource.SELF);
        self.setSourceRef("self-git");
        self.setTitle("Tự đánh giá Git");
        self.setScore(new BigDecimal("75"));
        self.setWeight(BigDecimal.ONE);
        self.setRecordedAt(RECORDED_AT);
        self.setVerified(false);
        evidences.saveAndFlush(self);

        SkillScore stored = new SkillScore();
        stored.setId(new SkillScoreId(user, git.getId()));
        stored.setSelfScore(new BigDecimal("75"));
        stored.setUpdatedAt(RECORDED_AT);
        scores.saveAndFlush(stored);

        var result = find(service.recomputeSkills(user), "GIT");
        assertThat(result.verifiedScore()).isNull();
        assertThat(result.level()).isNull();
        assertThat(result.selfScore()).isEqualByComparingTo("75");
        assertThat(result.evidences()).singleElement().satisfies(e -> assertThat(e.verified()).isFalse());
    }

    @Test
    void recomputeClearsStaleVerifiedScoreAfterEvidenceIsRemoved() {
        UUID user = UUID.randomUUID();
        service.importVerifiedRecords(user, List.of(record("JAVA01", "java-1", "80")));
        evidences.deleteAll(evidences.findAllByUserId(user));

        assertThat(service.recomputeSkills(user)).isEmpty();
        assertThat(scores.findAllByIdUserId(user)).isEmpty();
    }

    @Test
    void ignoresUnmappedCoursesAndDoesNotInventSkills() {
        UUID user = UUID.randomUUID();
        assertThat(service.importVerifiedRecords(user, List.of(record("UNMAPPED", "unknown", "80")))).isEmpty();
        assertThat(evidences.findAllByUserId(user)).isEmpty();
    }

    @Test
    void malformedBatchLeavesNoPartiallyImportedResults() {
        UUID user = UUID.randomUUID();
        assertThatThrownBy(() -> service.importVerifiedRecords(user,
                Arrays.asList(record("JAVA01", "valid", "80"), null)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(evidences.findAllByUserId(user)).isEmpty();
        assertThat(scores.findAllByIdUserId(user)).isEmpty();
    }

    @Test
    void catalogMigrationsCanRunAgainWithoutDuplicateSkillsOrMappings() {
        long mappingCount = mappings.count();
        flyway.migrate();

        assertThat(skills.count()).isEqualTo(20);
        assertThat(mappings.count()).isEqualTo(mappingCount).isGreaterThan(5);
    }

    @Test
    void threeCandidateFixturesProduceDatedAndSourcedSkillProfiles() throws Exception {
        List<DemoCandidate> candidates;
        try (var input = new ClassPathResource("hs01/candidates.json").getInputStream()) {
            candidates = new ObjectMapper().findAndRegisterModules()
                    .readValue(input, new TypeReference<List<DemoCandidate>>() { });
        }
        assertThat(candidates).hasSize(3);
        for (DemoCandidate candidate : candidates) {
            var result = service.importVerifiedRecords(candidate.fixtureUserId(), candidate.records());
            assertThat(result).isNotEmpty();
            if (candidate.lmsUserId().equals("lms-sv-001")) {
                assertThat(result).hasSizeGreaterThanOrEqualTo(5);
            }
            assertThat(result).allSatisfy(skill -> {
                assertThat(skill.evidences()).isNotEmpty().allSatisfy(e -> {
                    assertThat(e.source()).isNotNull();
                    assertThat(e.title()).isNotBlank();
                    assertThat(e.recordedAt()).isNotNull();
                    assertThat(e.verified()).isTrue();
                });
            });
            service.importVerifiedRecords(candidate.fixtureUserId(), candidate.records());
            assertThat(service.getSkills(candidate.fixtureUserId())).usingRecursiveComparison()
                    .withComparatorForType(BigDecimal::compareTo, BigDecimal.class).isEqualTo(result);
        }
        var first = service.getSkills(candidates.getFirst().fixtureUserId());
        // JAVA: (85 * .7 + 80 * 1) / (.7 + 1) = 82.058824.
        assertThat(find(first, "JAVA").verifiedScore()).isEqualByComparingTo("82.058824");
        assertThat(find(first, "REST_API").evidences()).extracting(SkillProfileView.EvidenceView::source)
                .contains(EvidenceSource.INTERNSHIP);
    }

    @Test
    void olderExternalResultKeepsItsOriginalDateAndHalfWeightAfterImport() {
        UUID user = UUID.randomUUID();
        var old = new VerifiedSkillRecord(MappingSource.LMS, "SQL01", "old-sql", "Kết quả cũ",
                new BigDecimal("60"), Instant.parse("2024-10-01T00:00:00Z"));
        var result = service.importVerifiedRecords(user, List.of(record("JAVA01", "new-java", "80"), old));

        // SQL: (80 * .3 + 60 * 1 * .5) / (.3 + 1 * .5) = 67.5.
        assertThat(find(result, "SQL").verifiedScore()).isEqualByComparingTo("67.5");
        assertThat(find(result, "SQL").evidences()).extracting(SkillProfileView.EvidenceView::recordedAt)
                .contains(Instant.parse("2024-10-01T00:00:00Z"));
    }

    @Test
    void microsecondDateAtAgeBoundaryProducesTheSameScoreAfterReloadAndRecompute() {
        UUID user = UUID.randomUUID();
        var old = new VerifiedSkillRecord(MappingSource.EXAM, "JAVA01", "just-before-cutoff", "Thi Java cũ",
                new BigDecimal("100"), Instant.parse("2025-11-10T23:59:59.999999Z"));
        var recent = new VerifiedSkillRecord(MappingSource.EXAM, "JAVA01", "recent", "Thi Java mới",
                BigDecimal.ZERO, Instant.parse("2026-11-11T00:00:00Z"));
        var imported = find(service.importVerifiedRecords(user, List.of(old, recent)), "JAVA");
        var reloaded = find(service.recomputeSkills(user), "JAVA");

        assertThat(imported.verifiedScore()).isEqualByComparingTo("33.333333");
        assertThat(imported.level()).isEqualTo(SkillLevel.BASIC);
        assertThat(reloaded.verifiedScore()).isEqualByComparingTo("33.333333");
        assertThat(reloaded.level()).isEqualTo(SkillLevel.BASIC);
    }

    @Test
    void calculationFailureRollsBackEvidenceCorrectionsAndNewEvidence() {
        UUID user = UUID.randomUUID();
        service.importVerifiedRecords(user, List.of(record("JAVA01", "java-1", "80")));
        doThrow(new IllegalStateException("Calculation interrupted"))
                .when(calculator).calculate(anyList(), any(Instant.class));
        try {
            assertThatThrownBy(() -> service.importVerifiedRecords(user,
                    List.of(record("JAVA01", "java-1", "95"), record("SQL01", "sql-new", "60"))))
                    .isInstanceOf(IllegalStateException.class);
        } finally {
            reset(calculator);
        }

        assertThat(evidences.findAllByUserId(user)).hasSize(2)
                .allSatisfy(e -> assertThat(e.getScore()).isEqualByComparingTo("80"));
        var retained = service.getSkills(user);
        assertThat(find(retained, "JAVA").verifiedScore()).isEqualByComparingTo("80");
        assertThat(find(retained, "SQL").verifiedScore()).isEqualByComparingTo("80");
    }

    @Test
    void databaseRejectsVerifiedSelfAndInvalidScoresOrWeights() {
        UUID user = UUID.randomUUID();
        SkillEvidence invalid = new SkillEvidence();
        invalid.setUserId(user);
        invalid.setSkillId(skills.findByCode("GIT").orElseThrow().getId());
        invalid.setSource(EvidenceSource.SELF);
        invalid.setSourceRef("invalid");
        invalid.setTitle("Invalid SELF");
        invalid.setScore(new BigDecimal("80"));
        invalid.setWeight(BigDecimal.ONE);
        invalid.setRecordedAt(RECORDED_AT);
        invalid.setVerified(true);
        assertThatThrownBy(() -> evidences.saveAndFlush(invalid))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);

        invalid.setId(null);
        invalid.setVerified(false);
        invalid.setScore(new BigDecimal("101"));
        assertThatThrownBy(() -> evidences.saveAndFlush(invalid))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);

        invalid.setId(null);
        invalid.setScore(new BigDecimal("80"));
        invalid.setWeight(BigDecimal.ZERO);
        assertThatThrownBy(() -> evidences.saveAndFlush(invalid))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    private VerifiedSkillRecord record(String code, String reference, String score) {
        return new VerifiedSkillRecord(MappingSource.LMS, code, reference,
                "Kết quả " + code, new BigDecimal(score), RECORDED_AT);
    }

    private SkillProfileView find(List<SkillProfileView> result, String code) {
        return result.stream().filter(v -> v.skill().code().equals(code)).findFirst().orElseThrow();
    }

    private record DemoCandidate(String lmsUserId, UUID fixtureUserId, List<VerifiedSkillRecord> records) { }
}
