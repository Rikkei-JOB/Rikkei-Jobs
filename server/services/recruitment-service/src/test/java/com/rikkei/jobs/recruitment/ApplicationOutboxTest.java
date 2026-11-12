package com.rikkei.jobs.recruitment;

import com.rikkei.jobs.recruitment.service.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ApplicationOutboxTest extends ApplicationIntegrationSupport {
    @Autowired ApplicationOutboxWorker worker;
    @MockBean ApplicationMailSender mail;
    @Test void committedApplicationNotifiesOncePerDistinctRecipient() throws Exception {
        apply();
        assertThat(worker.dispatchBatch()).isEqualTo(1);
        verify(mail).sendNewApplication(any(UUID.class), eq("hr@company.test"), any());
        assertThat(worker.dispatchBatch()).isZero();
        assertThat(jdbc.queryForObject("select status from recruitment.application_notification_outbox", String.class)).isEqualTo("SENT");
    }
    @Test void smtpFailureKeepsApplicationAndRetriesAfterRestart() throws Exception {
        apply();
        doThrow(new IllegalStateException("SMTP unavailable")).when(mail).sendNewApplication(any(),any(),any());
        worker.dispatchBatch();
        assertThat(count("applications")).isEqualTo(1);
        assertThat(jdbc.queryForObject("select status from recruitment.application_notification_outbox", String.class)).isEqualTo("PENDING");
        verify(mail).sendNewApplication(any(),any(),any());
        assertThat(worker.dispatchBatch()).isZero();
        when(clock.instant()).thenReturn(NOW.plusSeconds(61));
        reset(mail);
        assertThat(worker.dispatchBatch()).isEqualTo(1);
        assertThat(jdbc.queryForObject("select attempts from recruitment.application_notification_outbox", Integer.class)).isEqualTo(2);
    }
    @Test void expiredLeaseCanBeReclaimedButLiveLeaseCannot() throws Exception {
        apply();
        jdbc.update("update recruitment.application_notification_outbox set status='PROCESSING', lease_until=?", java.sql.Timestamp.from(NOW.plusSeconds(60)));
        assertThat(worker.dispatchBatch()).isZero();
        when(clock.instant()).thenReturn(NOW.plusSeconds(61));
        assertThat(worker.dispatchBatch()).isEqualTo(1);
    }

    @Test void twoWorkersCannotSendTheSameLeasedNotification() throws Exception {
        apply();
        var entered = new java.util.concurrent.CountDownLatch(1);
        var release = new java.util.concurrent.CountDownLatch(1);
        doAnswer(call -> { entered.countDown(); release.await(10, java.util.concurrent.TimeUnit.SECONDS); return null; })
            .when(mail).sendNewApplication(any(), any(), any());
        try (var pool = java.util.concurrent.Executors.newSingleThreadExecutor()) {
            var first = pool.submit(worker::dispatchBatch);
            try {
                assertThat(entered.await(10, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
                assertThat(worker.dispatchBatch()).isZero();
            } finally { release.countDown(); }
            assertThat(first.get(10, java.util.concurrent.TimeUnit.SECONDS)).isEqualTo(1);
        }
        verify(mail, times(1)).sendNewApplication(any(), any(), any());
    }

    @Test void fifthFailureIsTerminalUntilOperatorRequeuesIt() throws Exception {
        apply();
        doThrow(new IllegalStateException("SMTP unavailable")).when(mail).sendNewApplication(any(), any(), any());
        long[] elapsed = {0, 60, 360, 1260, 4860};
        for (long seconds : elapsed) {
            when(clock.instant()).thenReturn(NOW.plusSeconds(seconds));
            assertThat(worker.dispatchBatch()).isEqualTo(1);
        }
        assertThat(jdbc.queryForObject("select status from recruitment.application_notification_outbox", String.class)).isEqualTo("FAILED");
        when(clock.instant()).thenReturn(NOW.plusSeconds(10000));
        assertThat(worker.dispatchBatch()).isZero();
        verify(mail, times(5)).sendNewApplication(any(), any(), any());
    }
}
