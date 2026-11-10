package com.rikkei.jobs.profile.service;

import com.rikkei.jobs.profile.entity.SkillLevel;
import com.rikkei.jobs.profile.service.SkillScoreCalculator.ScoreEvidence;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SkillScoreCalculatorTest {

    private static final Instant AS_OF = Instant.parse("2026-11-11T10:15:30Z");
    private final SkillScoreCalculator calculator = new SkillScoreCalculator();

    @Test
    void weightsOldEvidenceAtHalfItsMappingWeight() {
        var result = calculator.calculate(List.of(
                evidence("80", "0.7", AS_OF, true),
                evidence("60", "0.3", Instant.parse("2024-11-11T10:15:30Z"), true)
        ), AS_OF);

        assertEquals(new BigDecimal("76.470588"), result.verifiedScore());
        assertEquals(SkillLevel.GOOD, result.level());
    }

    @ParameterizedTest
    @CsvSource({
            "2025-11-11T10:15:30Z, 50.000000",
            "2025-11-11T10:15:29.999999999Z, 33.333333"
    })
    void treatsExactlyOneCalendarYearAsRecent(String recordedAt, String expectedScore) {
        var result = calculator.calculate(List.of(
                evidence("100", "1", Instant.parse(recordedAt), true),
                evidence("0", "1", AS_OF, true)
        ), AS_OF);

        assertEquals(new BigDecimal(expectedScore), result.verifiedScore());
    }

    @ParameterizedTest
    @CsvSource({
            "2024-03-01T10:15:30Z, 2023-03-01T10:15:30Z",
            "2024-02-29T10:15:30Z, 2023-02-28T10:15:30Z"
    })
    void usesUtcCalendarYearsAcrossLeapYears(String asOf, String recordedAt) {
        Instant calculationTime = Instant.parse(asOf);

        var result = calculator.calculate(List.of(
                evidence("100", "1", Instant.parse(recordedAt), true),
                evidence("0", "1", calculationTime, true)
        ), calculationTime);

        assertEquals(new BigDecimal("50.000000"), result.verifiedScore());
    }

    @ParameterizedTest
    @CsvSource({
            "0, 0.000000, BASIC",
            "39.999999, 39.999999, BASIC",
            "39.9999996, 40.000000, BASIC",
            "40, 40.000000, FAIR",
            "69.999999, 69.999999, FAIR",
            "69.9999996, 70.000000, FAIR",
            "70, 70.000000, GOOD",
            "84.999999, 84.999999, GOOD",
            "84.9999996, 85.000000, GOOD",
            "85, 85.000000, EXCELLENT",
            "100, 100.000000, EXCELLENT"
    })
    void derivesLevelBeforeRounding(String score, String expectedScore, SkillLevel expectedLevel) {
        var result = calculator.calculate(List.of(evidence(score, "0.7", AS_OF, true)), AS_OF);

        assertEquals(new BigDecimal(expectedScore), result.verifiedScore());
        assertEquals(expectedLevel, result.level());
    }

    @Test
    void roundsAtSixDecimalsUsingHalfUp() {
        var result = calculator.calculate(List.of(evidence("72.1234565", "1", AS_OF, true)), AS_OF);

        assertEquals(new BigDecimal("72.123457"), result.verifiedScore());
    }

    @Test
    void ignoresUnverifiedSelfEvidenceInTheWeightedMean() {
        var result = calculator.calculate(List.of(
                evidence("60", "1", AS_OF, true),
                evidence("100", "999", AS_OF, false)
        ), AS_OF);

        assertEquals(new BigDecimal("60.000000"), result.verifiedScore());
        assertEquals(SkillLevel.FAIR, result.level());
    }

    @Test
    void returnsNoVerifiedScoreForSelfOnlyEvidence() {
        var result = calculator.calculate(List.of(evidence("90", "1", AS_OF, false)), AS_OF);

        assertNull(result.verifiedScore());
        assertNull(result.level());
    }

    @Test
    void returnsNoVerifiedScoreForAnEmptyProfile() {
        var result = calculator.calculate(List.of(), AS_OF);

        assertNull(result.verifiedScore());
        assertNull(result.level());
    }

    @ParameterizedTest
    @CsvSource({"-0.000001", "100.000001"})
    void rejectsScoresOutsideTheNormalizedRange(String score) {
        assertThrows(IllegalArgumentException.class,
                () -> calculator.calculate(List.of(evidence(score, "1", AS_OF, true)), AS_OF));
    }

    @ParameterizedTest
    @CsvSource({"0", "-0.001"})
    void rejectsNonPositiveWeights(String weight) {
        assertThrows(IllegalArgumentException.class,
                () -> calculator.calculate(List.of(evidence("80", weight, AS_OF, true)), AS_OF));
    }

    @ParameterizedTest
    @MethodSource("invalidEvidences")
    void rejectsMissingEvidenceFields(ScoreEvidence evidence) {
        assertThrows(IllegalArgumentException.class,
                () -> calculator.calculate(Arrays.asList(evidence), AS_OF));
    }

    @Test
    void validatesUnverifiedEvidenceBeforeIgnoringIt() {
        assertThrows(IllegalArgumentException.class,
                () -> calculator.calculate(List.of(evidence("101", "1", AS_OF, false)), AS_OF));
    }

    @Test
    void rejectsMissingEvidenceList() {
        assertThrows(IllegalArgumentException.class, () -> calculator.calculate(null, AS_OF));
    }

    @Test
    void rejectsMissingCalculationTime() {
        assertThrows(IllegalArgumentException.class, () -> calculator.calculate(List.of(), null));
    }

    private static Stream<ScoreEvidence> invalidEvidences() {
        return Stream.of(
                null,
                new ScoreEvidence(null, BigDecimal.ONE, AS_OF, true),
                new ScoreEvidence(BigDecimal.TEN, null, AS_OF, true),
                new ScoreEvidence(BigDecimal.TEN, BigDecimal.ONE, null, true)
        );
    }

    private static ScoreEvidence evidence(String score, String weight, Instant recordedAt, boolean verified) {
        return new ScoreEvidence(new BigDecimal(score), new BigDecimal(weight), recordedAt, verified);
    }
}
