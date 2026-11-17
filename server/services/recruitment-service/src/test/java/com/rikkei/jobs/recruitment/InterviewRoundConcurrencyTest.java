package com.rikkei.jobs.recruitment;

import com.rikkei.jobs.common.config.security.principle.AuthPrincipal;
import com.rikkei.jobs.common.config.security.principle.Role;
import com.rikkei.jobs.recruitment.dto.request.CreateRoundRequest;
import com.rikkei.jobs.recruitment.service.ApplicationTransitionService;
import com.rikkei.jobs.recruitment.service.InterviewRoundService;
import com.rikkei.jobs.recruitment.entity.ApplicationStatus;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import static org.assertj.core.api.Assertions.*;

class InterviewRoundConcurrencyTest extends ApplicationIntegrationSupport {
    @Autowired InterviewRoundService rounds;
    @Autowired ApplicationTransitionService transitions;

    @Test void concurrentCreatesAssignDifferentSequentialRoundNumbers() throws Exception {
        UUID applicationId = apply();
        UUID hrId = UUID.randomUUID();
        transitions.transition(applicationId, ApplicationStatus.SCREENING, hrId, null);
        transitions.transition(applicationId, ApplicationStatus.INTERVIEW, hrId, null);
        var hr = new AuthPrincipal(hrId, Role.HR, company);
        var executor = Executors.newFixedThreadPool(2);
        var start = new CountDownLatch(1);
        try {
            Callable<Integer> create = () -> {
                start.await();
                return rounds.create(hr, applicationId,
                    new CreateRoundRequest("Kỹ thuật", "interviewer@example.test", null)).roundNo();
            };
            Future<Integer> first = executor.submit(create);
            Future<Integer> second = executor.submit(create);
            start.countDown();
            assertThat(Set.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS)))
                .containsExactlyInAnyOrder(1, 2);
            assertThat(count("interview_rounds")).isEqualTo(2);
        } finally {
            executor.shutdownNow();
        }
    }
}
