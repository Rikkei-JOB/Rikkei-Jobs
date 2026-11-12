package com.rikkei.jobs.profile.service;

import com.rikkei.jobs.profile.entity.SkillLevel;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

@Component
public class SkillScoreCalculator {

    private static final BigDecimal OLD_EVIDENCE_FACTOR = new BigDecimal("0.5");

    public CalculatedSkillScore calculate(List<ScoreEvidence> evidences, Instant asOf) {
        if (evidences == null || asOf == null) {
            throw new IllegalArgumentException("Evidence list and calculation time are required");
        }

        Instant recentCutoff = asOf.atZone(ZoneOffset.UTC).minusYears(1).toInstant();
        BigDecimal numerator = BigDecimal.ZERO;
        BigDecimal denominator = BigDecimal.ZERO;

        for (ScoreEvidence evidence : evidences) {
            validateEvidence(evidence);
            if (!evidence.verified()) {
                continue;
            }
            BigDecimal effectiveWeight = evidence.recordedAt().isBefore(recentCutoff)
                    ? evidence.weight().multiply(OLD_EVIDENCE_FACTOR)
                    : evidence.weight();
            numerator = numerator.add(evidence.score().multiply(effectiveWeight));
            denominator = denominator.add(effectiveWeight);
        }

        if (denominator.signum() == 0) {
            return new CalculatedSkillScore(null, null);
        }

        return new CalculatedSkillScore(
                numerator.divide(denominator, 6, RoundingMode.HALF_UP),
                determineLevel(numerator, denominator));
    }

    private void validateEvidence(ScoreEvidence evidence) {
        if (evidence == null || evidence.score() == null || evidence.weight() == null
                || evidence.recordedAt() == null) {
            throw new IllegalArgumentException("Evidence, score, weight and recorded time are required");
        }
        if (evidence.score().signum() < 0 || evidence.score().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException("Score must be between 0 and 100");
        }
        if (evidence.weight().signum() <= 0) {
            throw new IllegalArgumentException("Weight must be positive");
        }
    }

    private SkillLevel determineLevel(BigDecimal numerator, BigDecimal denominator) {
        if (numerator.compareTo(denominator.multiply(BigDecimal.valueOf(40))) < 0) {
            return SkillLevel.BASIC;
        }
        if (numerator.compareTo(denominator.multiply(BigDecimal.valueOf(70))) < 0) {
            return SkillLevel.FAIR;
        }
        if (numerator.compareTo(denominator.multiply(BigDecimal.valueOf(85))) < 0) {
            return SkillLevel.GOOD;
        }
        return SkillLevel.EXCELLENT;
    }

    public record ScoreEvidence(BigDecimal score, BigDecimal weight, Instant recordedAt, boolean verified) {
    }

    public record CalculatedSkillScore(BigDecimal verifiedScore, SkillLevel level) {
    }
}
