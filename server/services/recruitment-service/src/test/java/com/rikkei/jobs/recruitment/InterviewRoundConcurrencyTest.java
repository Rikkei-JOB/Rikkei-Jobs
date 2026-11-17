package com.rikkei.jobs.recruitment;

import com.rikkei.jobs.common.config.security.principle.AuthPrincipal;
import com.rikkei.jobs.common.config.security.principle.Role;
import com.rikkei.jobs.recruitment.dto.request.CreateRoundRequest;
import com.rikkei.jobs.recruitment.dto.request.UpdateRoundRequest;
import com.rikkei.jobs.recruitment.service.ApplicationTransitionService;
import com.rikkei.jobs.recruitment.service.InterviewRoundService;
import com.rikkei.jobs.recruitment.entity.ApplicationStatus;
import com.rikkei.jobs.recruitment.entity.InterviewRoundResult;
import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

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

    @Test void concurrentMentorUpdatesAuditTheCommittedPreviousResult() throws Exception {
        UUID applicationId = apply();
        UUID hrId = UUID.randomUUID();
        transitions.transition(applicationId, ApplicationStatus.SCREENING, hrId, null);
        transitions.transition(applicationId, ApplicationStatus.INTERVIEW, hrId, null);
        UUID roundId = rounds.create(new AuthPrincipal(hrId, Role.HR, company), applicationId,
            new CreateRoundRequest("Kỹ thuật", "mentor@example.test", null)).id();
        UUID mentorId = UUID.randomUUID();
        when(mentors.email(mentorId)).thenReturn("mentor@example.test");
        var mentor = new AuthPrincipal(mentorId, Role.MENTOR, null);
        var executor = Executors.newFixedThreadPool(2);
        var start = new CountDownLatch(1);
        try {
            Callable<Void> pass = () -> { start.await(); rounds.updateInvited(mentor, roundId,
                new UpdateRoundRequest(InterviewRoundResult.PASS, BigDecimal.valueOf(80), null)); return null; };
            Callable<Void> fail = () -> { start.await(); rounds.updateInvited(mentor, roundId,
                new UpdateRoundRequest(InterviewRoundResult.FAIL, BigDecimal.valueOf(40), null)); return null; };
            Future<Void> first = executor.submit(pass);
            Future<Void> second = executor.submit(fail);
            start.countDown();
            first.get(10, TimeUnit.SECONDS);
            second.get(10, TimeUnit.SECONDS);
            assertThat(count("interview_round_revisions")).isEqualTo(2);
            assertThat(jdbc.queryForObject("select count(*) from recruitment.interview_round_revisions where old_result='PENDING'", Integer.class))
                .isEqualTo(1);
        } finally { executor.shutdownNow(); }
    }
}
