package com.rikkei.jobs.recruitment.service;

import com.rikkei.jobs.common.config.exception.ConflictException;
import com.rikkei.jobs.recruitment.entity.InterviewRound;
import com.rikkei.jobs.recruitment.entity.InterviewRoundResult;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class OfferEligibilityPolicy {
    public void requireEligible(List<InterviewRound> rounds) {
        if (rounds.isEmpty() || rounds.stream().anyMatch(round -> round.getResult() == InterviewRoundResult.PENDING)
            || rounds.stream().max(Comparator.comparingInt(InterviewRound::getRoundNo))
                .orElseThrow().getResult() != InterviewRoundResult.PASS) {
            throw new ConflictException("Chưa đủ kết quả phỏng vấn để gửi offer.");
        }
    }
}
