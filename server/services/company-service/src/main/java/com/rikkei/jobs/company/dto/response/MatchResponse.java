package com.rikkei.jobs.company.dto.response;

import java.math.BigDecimal;
import java.util.List;

/** Điểm khớp JD - ứng viên. Hợp đồng với UT-02: đúng 4 trường, mỗi phần tử đúng 4 trường. */
public record MatchResponse(int score, boolean belowMinimum, List<SkillMatchResponse> met,
                            List<SkillMatchResponse> missing) {

    public record SkillMatchResponse(String skill, boolean required, int minScore, BigDecimal candidateScore) {
    }
}
