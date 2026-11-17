package com.rikkei.jobs.recruitment;

import com.rikkei.jobs.common.config.exception.ConflictException;
import com.rikkei.jobs.recruitment.entity.InterviewRound;
import com.rikkei.jobs.recruitment.entity.InterviewRoundResult;
import com.rikkei.jobs.recruitment.service.OfferEligibilityPolicy;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class OfferEligibilityPolicyTest {
    private final OfferEligibilityPolicy policy = new OfferEligibilityPolicy();

    @Test void noRoundsCannotSendOffer() {
        assertThatThrownBy(() -> policy.requireEligible(List.of())).isInstanceOf(ConflictException.class);
    }

    @Test void anyPendingRoundCannotSendOffer() {
        assertThatThrownBy(() -> policy.requireEligible(List.of(round(1, InterviewRoundResult.PENDING),
            round(2, InterviewRoundResult.PASS)))).isInstanceOf(ConflictException.class);
    }

    @Test void lastFailedRoundCannotSendOffer() {
        assertThatThrownBy(() -> policy.requireEligible(List.of(round(1, InterviewRoundResult.PASS),
            round(2, InterviewRoundResult.FAIL)))).isInstanceOf(ConflictException.class);
    }

    @Test void lastPassedRoundAllowsOfferEvenWhenEarlierRoundFailed() {
        assertThatCode(() -> policy.requireEligible(List.of(round(2, InterviewRoundResult.PASS),
            round(1, InterviewRoundResult.FAIL)))).doesNotThrowAnyException();
    }

    private InterviewRound round(int number, InterviewRoundResult result) {
        var round = new InterviewRound();
        round.setRoundNo(number);
        round.setResult(result);
        return round;
    }
}
