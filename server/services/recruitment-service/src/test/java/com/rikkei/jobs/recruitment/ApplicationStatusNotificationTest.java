package com.rikkei.jobs.recruitment;

import com.rikkei.jobs.recruitment.client.CandidateIdentity;
import com.rikkei.jobs.recruitment.client.IdentityCandidateClient;
import com.rikkei.jobs.recruitment.entity.ApplicationStatus;
import com.rikkei.jobs.recruitment.service.ApplicationStatusMailSender;
import com.rikkei.jobs.recruitment.service.ApplicationStatusOutboxWorker;
import com.rikkei.jobs.recruitment.service.ApplicationTransitionService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ApplicationStatusNotificationTest extends ApplicationIntegrationSupport {
    @Autowired ApplicationTransitionService transitions;
    @Autowired ApplicationStatusOutboxWorker worker;
    @MockBean IdentityCandidateClient identities;
    @MockBean ApplicationStatusMailSender sender;

    @Test void eachSuccessfulTransitionCreatesOneDurableCandidateEmailEvent() throws Exception {
        UUID id = apply();
        assertThat(count("application_status_notification_outbox")).isZero();
        transitions.transition(id, ApplicationStatus.SCREENING, UUID.randomUUID(), "Ghi chú nội bộ");
        transitions.transition(id, ApplicationStatus.INTERVIEW, UUID.randomUUID(), null);
        assertThat(count("application_status_notification_outbox")).isEqualTo(2);
        assertThat(jdbc.queryForObject("select count(distinct status_history_id) from recruitment.application_status_notification_outbox", Integer.class))
            .isEqualTo(2);
        assertThat(count("application_status_history")).isEqualTo(3);
        assertThat(count("application_notification_outbox")).isEqualTo(1);
    }

    @Test void rejectedTransitionDoesNotCreateEmailEvent() throws Exception {
        UUID id = apply();
        assertThatThrownBy(() -> transitions.transition(id, ApplicationStatus.INTERVIEW, UUID.randomUUID(), null))
            .isInstanceOf(RuntimeException.class);
        assertThat(count("application_status_notification_outbox")).isZero();
        assertThat(count("application_status_history")).isEqualTo(1);
    }

    @Test void workerResolvesCurrentEmailAndSendsEveryTransitionOnce() throws Exception {
        UUID id = apply();
        transitions.transition(id, ApplicationStatus.SCREENING, UUID.randomUUID(), "Không gửi ghi chú này");
        transitions.transition(id, ApplicationStatus.INTERVIEW, UUID.randomUUID(), null);
        when(identities.get(user)).thenReturn(new CandidateIdentity(user, "Nguyễn Văn An", "current@example.test"));
        when(clock.instant()).thenReturn(NOW.plusSeconds(1));
        assertThat(worker.dispatchBatch()).isEqualTo(2);
        assertThat(jdbc.queryForObject("select count(*) from recruitment.application_status_notification_outbox where status='SENT'", Integer.class))
            .isEqualTo(2);
        verify(sender, times(2)).send(any(UUID.class), eq("current@example.test"), any(), any());
        assertThat(worker.dispatchBatch()).isZero();
        verifyNoMoreInteractions(sender);
    }

    @Test void identityFailureRetriesTheSameEventWithoutCreatingAnother() throws Exception {
        UUID id = apply();
        transitions.transition(id, ApplicationStatus.SCREENING, UUID.randomUUID(), null);
        when(identities.get(user)).thenThrow(new IllegalStateException("identity unavailable"))
            .thenReturn(new CandidateIdentity(user, "Nguyễn Văn An", "new@example.test"));
        when(clock.instant()).thenReturn(NOW.plusSeconds(1));
        assertThat(worker.dispatchBatch()).isEqualTo(1);
        assertThat(jdbc.queryForObject("select status from recruitment.application_status_notification_outbox", String.class))
            .isEqualTo("PENDING");
        assertThat(count("application_status_notification_outbox")).isEqualTo(1);
        jdbc.update("update recruitment.application_status_notification_outbox set next_attempt_at=?",
            java.sql.Timestamp.from(NOW));
        assertThat(worker.dispatchBatch()).isEqualTo(1);
        assertThat(jdbc.queryForObject("select status from recruitment.application_status_notification_outbox", String.class))
            .isEqualTo("SENT");
        verify(sender).send(any(UUID.class), eq("new@example.test"), any(), any());
        assertThat(count("application_status_notification_outbox")).isEqualTo(1);
    }
}
