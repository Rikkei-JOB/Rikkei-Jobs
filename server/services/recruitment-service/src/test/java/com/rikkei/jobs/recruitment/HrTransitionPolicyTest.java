package com.rikkei.jobs.recruitment;

import com.rikkei.jobs.recruitment.config.InvalidTransitionException;
import com.rikkei.jobs.recruitment.entity.ApplicationStatus;
import com.rikkei.jobs.recruitment.service.HrTransitionPolicy;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static com.rikkei.jobs.recruitment.entity.ApplicationStatus.*;
import static org.assertj.core.api.Assertions.*;

class HrTransitionPolicyTest {
    private final HrTransitionPolicy policy = new HrTransitionPolicy();
    private final Map<ApplicationStatus, Set<ApplicationStatus>> expected = Map.of(
        SUBMITTED, Set.of(SCREENING, REJECTED),
        SCREENING, Set.of(INTERVIEW, REJECTED),
        INTERVIEW, Set.of(REJECTED));

    @Test void everyStatusPairHasExactlyTheRequiredHrRule() {
        int allowed = 0;
        for (var from : ApplicationStatus.values()) {
            assertThat(policy.allowedFrom(from)).containsExactlyInAnyOrderElementsOf(
                expected.getOrDefault(from, Set.of()));
            for (var to : ApplicationStatus.values()) {
                if (expected.getOrDefault(from, Set.of()).contains(to)) {
                    assertThatCode(() -> policy.requireAllowed(from, to)).doesNotThrowAnyException();
                    allowed++;
                } else {
                    assertThatThrownBy(() -> policy.requireAllowed(from, to))
                        .isInstanceOf(InvalidTransitionException.class);
                }
            }
        }
        assertThat(allowed).isEqualTo(5);
    }
}
