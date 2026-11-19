package com.rikkei.jobs.company.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Tính điểm phù hợp giữa kỹ năng yêu cầu của tin tuyển dụng và điểm đã xác thực của ứng viên.
 * Lớp thuần, không phụ thuộc Spring hay DB.
 */
public final class MatchCalculator {

    private static final int SCALE = 10;

    private MatchCalculator() {
    }

    public record JobSkillInput(UUID skillId, String skillName, boolean required, int minScore) {
    }

    public record SkillMatch(UUID skillId, String skill, boolean required, int minScore,
                             BigDecimal candidateScore) {
    }

    public record MatchResult(int score, boolean belowMinimum, List<SkillMatch> met,
                              List<SkillMatch> missing) {
    }

    private static final Comparator<SkillMatch> ORDER = Comparator
            .comparing((SkillMatch m) -> !m.required())
            .thenComparing(SkillMatch::skill, String.CASE_INSENSITIVE_ORDER)
            .thenComparing(SkillMatch::skillId);

    public static MatchResult calculate(List<JobSkillInput> jobSkills, Map<UUID, BigDecimal> verifiedScores) {
        List<SkillMatch> met = new ArrayList<>();
        List<SkillMatch> missing = new ArrayList<>();
        BigDecimal requiredSum = BigDecimal.ZERO;
        BigDecimal preferredSum = BigDecimal.ZERO;
        int requiredCount = 0;
        int preferredCount = 0;
        boolean belowMinimum = false;

        for (JobSkillInput js : jobSkills) {
            BigDecimal v = verifiedScores.getOrDefault(js.skillId(), BigDecimal.ZERO);
            BigDecimal min = BigDecimal.valueOf(js.minScore());
            BigDecimal s = v.divide(min, SCALE, RoundingMode.HALF_UP).min(BigDecimal.ONE);

            if (js.required()) {
                requiredSum = requiredSum.add(s);
                requiredCount++;
                if (v.multiply(BigDecimal.valueOf(2)).compareTo(min) < 0) {
                    belowMinimum = true;
                }
            } else {
                preferredSum = preferredSum.add(s);
                preferredCount++;
            }

            SkillMatch match = new SkillMatch(js.skillId(), js.skillName(), js.required(), js.minScore(), v);
            (v.compareTo(min) >= 0 ? met : missing).add(match);
        }
        met.sort(ORDER);
        missing.sort(ORDER);

        BigDecimal hundred = BigDecimal.valueOf(100);
        BigDecimal r = requiredCount == 0 ? BigDecimal.ZERO
                : requiredSum.divide(BigDecimal.valueOf(requiredCount), SCALE, RoundingMode.HALF_UP);
        BigDecimal p = preferredCount == 0 ? BigDecimal.ZERO
                : preferredSum.divide(BigDecimal.valueOf(preferredCount), SCALE, RoundingMode.HALF_UP);

        BigDecimal raw;
        if (requiredCount > 0 && preferredCount > 0) {
            raw = r.multiply(BigDecimal.valueOf(70)).add(p.multiply(BigDecimal.valueOf(30)));
        } else if (requiredCount > 0) {
            raw = r.multiply(hundred);
        } else if (preferredCount > 0) {
            raw = p.multiply(hundred);
        } else {
            raw = BigDecimal.ZERO;
        }
        int score = raw.setScale(0, RoundingMode.HALF_UP).intValueExact();

        return new MatchResult(score, belowMinimum, List.copyOf(met), List.copyOf(missing));
    }
}
