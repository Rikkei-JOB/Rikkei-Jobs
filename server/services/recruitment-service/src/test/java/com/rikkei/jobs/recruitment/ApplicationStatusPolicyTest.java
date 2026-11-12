package com.rikkei.jobs.recruitment;

import com.rikkei.jobs.common.config.exception.ConflictException;
import com.rikkei.jobs.recruitment.entity.ApplicationStatus;
import com.rikkei.jobs.recruitment.service.ApplicationStatusPolicy;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static com.rikkei.jobs.recruitment.entity.ApplicationStatus.*;

class ApplicationStatusPolicyTest {
    private final ApplicationStatusPolicy policy = new ApplicationStatusPolicy();

    @Test void withdrawalIsOnlyAllowedBeforeOffer() {
        for (ApplicationStatus status : ApplicationStatus.values()) {
            assertThat(policy.canWithdraw(status)).isEqualTo(status == SUBMITTED || status == SCREENING || status == INTERVIEW);
        }
    }

    @Test void rejectedAndWithdrawnAllowNewApplicationWithoutReopeningTheOldOne() {
        for (ApplicationStatus status : ApplicationStatus.values()) {
            if (status == REJECTED || status == WITHDRAWN) {
                assertThatCode(() -> policy.requireReapplicationAllowed(status)).doesNotThrowAnyException();
            } else {
                assertThatThrownBy(() -> policy.requireReapplicationAllowed(status)).isInstanceOf(ConflictException.class);
            }
        }
        assertThatThrownBy(() -> policy.requireTransition(REJECTED, SUBMITTED)).isInstanceOf(ConflictException.class);
    }

    @Test void transitionsCannotSkipStepsOrLeaveTerminalStates() {
        var flow = new ApplicationStatus[]{SUBMITTED, SCREENING, INTERVIEW, OFFER, HIRED};
        for (int i = 0; i < flow.length - 1; i++) {
            int step = i;
            assertThatCode(() -> policy.requireTransition(flow[step], flow[step + 1])).doesNotThrowAnyException();
            assertThatCode(() -> policy.requireTransition(flow[step], REJECTED)).doesNotThrowAnyException();
        }
        assertThatThrownBy(() -> policy.requireTransition(SUBMITTED, OFFER)).isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> policy.requireTransition(HIRED, REJECTED)).isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> policy.requireTransition(OFFER, WITHDRAWN)).isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> policy.requireTransition(WITHDRAWN, SCREENING)).isInstanceOf(ConflictException.class);
    }
}
