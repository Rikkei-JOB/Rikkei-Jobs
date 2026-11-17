package com.rikkei.jobs.recruitment.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record MatchResponse(BigDecimal score, boolean belowMinimum,
    List<SkillMatchResponse> met, List<SkillMatchResponse> missing) {
    public record SkillMatchResponse(String skill, boolean required, BigDecimal minScore,
        BigDecimal candidateScore) {}
}
