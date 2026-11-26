package com.rikkei.jobs.company;

import static org.assertj.core.api.Assertions.assertThat;

import com.rikkei.jobs.company.util.MatchCalculator;
import com.rikkei.jobs.company.util.MatchCalculator.JobSkillInput;
import com.rikkei.jobs.company.util.MatchCalculator.MatchResult;
import com.rikkei.jobs.company.util.MatchCalculator.SkillMatch;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MatchCalculatorTest {

    private static final UUID JAVA = UUID.fromString("00000000-0000-0000-0001-000000000001");
    private static final UUID SPRING_BOOT = UUID.fromString("00000000-0000-0000-0001-000000000002");
    private static final UUID SQL = UUID.fromString("00000000-0000-0000-0001-000000000003");
    private static final UUID GIT = UUID.fromString("00000000-0000-0000-0001-000000000004");
    private static final UUID DOCKER = UUID.fromString("00000000-0000-0000-0001-000000000011");
    private static final UUID COMMUNICATION = UUID.fromString("00000000-0000-0000-0001-000000000018");

    private static JobSkillInput req(UUID id, String name, int min) {
        return new JobSkillInput(id, name, true, min);
    }

    private static JobSkillInput pref(UUID id, String name, int min) {
        return new JobSkillInput(id, name, false, min);
    }

    private static List<JobSkillInput> jd6() {
        return List.of(
                req(JAVA, "Java", 80),
                req(SPRING_BOOT, "Spring Boot", 70),
                req(SQL, "SQL", 90),
                pref(DOCKER, "Docker", 50),
                pref(COMMUNICATION, "Giao tiếp", 60),
                pref(GIT, "Git", 60));
    }

    private static List<JobSkillInput> jd7() {
        return List.of(req(JAVA, "Java", 60), req(SQL, "SQL", 60), req(GIT, "Git", 60));
    }

    private static List<String> names(List<SkillMatch> list) {
        return list.stream().map(SkillMatch::skill).toList();
    }

    @Test
    void example1BackendJdPartialMatch() {
        Map<UUID, BigDecimal> c1 = Map.of(
                JAVA, new BigDecimal("82.058824"),
                SPRING_BOOT, new BigDecimal("80.454545"),
                SQL, new BigDecimal("81.153846"),
                GIT, new BigDecimal("90"));

        MatchResult r = MatchCalculator.calculate(jd6(), c1);

        assertThat(r.score()).isEqualTo(78);
        assertThat(r.belowMinimum()).isFalse();
        assertThat(names(r.met())).containsExactly("Java", "Spring Boot", "Git");
        assertThat(names(r.missing())).containsExactly("SQL", "Docker", "Giao tiếp");
        SkillMatch sql = r.missing().get(0);
        assertThat(sql.candidateScore()).isEqualByComparingTo("81.153846");
        assertThat(sql.minScore()).isEqualTo(90);
        SkillMatch english = r.missing().get(2);
        assertThat(english.candidateScore().compareTo(BigDecimal.ZERO)).isZero();
    }

    @Test
    void example2RequiredOnlyBelowMinimum() {
        Map<UUID, BigDecimal> c3 = Map.of(
                JAVA, new BigDecimal("49.117647"),
                SQL, new BigDecimal("58.846154"));

        MatchResult r = MatchCalculator.calculate(jd7(), c3);

        assertThat(r.score()).isEqualTo(60);
        assertThat(r.belowMinimum()).isTrue();
        assertThat(r.met()).isEmpty();
        assertThat(names(r.missing())).containsExactly("Git", "Java", "SQL");
    }

    @Test
    void example3NoSkills() {
        MatchResult r = MatchCalculator.calculate(jd6(), Map.of());

        assertThat(r.score()).isZero();
        assertThat(r.belowMinimum()).isTrue();
        assertThat(r.met()).isEmpty();
        assertThat(names(r.missing()))
                .containsExactly("Java", "Spring Boot", "SQL", "Docker", "Git", "Giao tiếp");
        assertThat(r.missing()).allSatisfy(
                m -> assertThat(m.candidateScore().compareTo(BigDecimal.ZERO)).isZero());
    }

    @Test
    void requiredOnlyUses100R() {
        MatchResult r = MatchCalculator.calculate(
                List.of(req(JAVA, "Java", 80)), Map.of(JAVA, new BigDecimal("40")));

        assertThat(r.score()).isEqualTo(50);
    }

    @Test
    void preferredOnlyUses100P() {
        MatchResult r = MatchCalculator.calculate(
                List.of(pref(DOCKER, "Docker", 50)), Map.of(DOCKER, new BigDecimal("25")));

        assertThat(r.score()).isEqualTo(50);
        assertThat(r.belowMinimum()).isFalse();
    }

    @Test
    void emptyJobIsZero() {
        MatchResult r = MatchCalculator.calculate(List.of(), Map.of(JAVA, new BigDecimal("90")));

        assertThat(r.score()).isZero();
        assertThat(r.belowMinimum()).isFalse();
        assertThat(r.met()).isEmpty();
        assertThat(r.missing()).isEmpty();
    }

    @Test
    void scoreAboveMinIsCappedAtOne() {
        MatchResult r = MatchCalculator.calculate(
                List.of(req(JAVA, "Java", 40)), Map.of(JAVA, new BigDecimal("100")));

        assertThat(r.score()).isEqualTo(100);
    }

    @Test
    void scoreEqualToMinIsMet() {
        MatchResult r = MatchCalculator.calculate(
                List.of(req(JAVA, "Java", 70)), Map.of(JAVA, new BigDecimal("70.000000")));

        assertThat(names(r.met())).containsExactly("Java");
        assertThat(r.missing()).isEmpty();
        assertThat(r.score()).isEqualTo(100);
    }

    @Test
    void belowMinimumBoundary() {
        List<JobSkillInput> job = List.of(req(JAVA, "Java", 60));

        assertThat(MatchCalculator.calculate(job, Map.of(JAVA, new BigDecimal("30"))).belowMinimum())
                .isFalse();
        assertThat(MatchCalculator.calculate(job, Map.of(JAVA, new BigDecimal("29.999999"))).belowMinimum())
                .isTrue();
    }

    @Test
    void roundsHalfUp() {
        MatchResult r = MatchCalculator.calculate(
                List.of(req(JAVA, "Java", 100), req(SQL, "SQL", 100)),
                Map.of(JAVA, new BigDecimal("100"), SQL, new BigDecimal("51")));

        assertThat(r.score()).isEqualTo(76);
    }

    @Test
    void ignoresScoresOfSkillsNotInJob() {
        Map<UUID, BigDecimal> c3 = Map.of(
                JAVA, new BigDecimal("49.117647"),
                SQL, new BigDecimal("58.846154"),
                DOCKER, new BigDecimal("100"));

        MatchResult r = MatchCalculator.calculate(jd7(), c3);

        assertThat(r.score()).isEqualTo(60);
    }
}
