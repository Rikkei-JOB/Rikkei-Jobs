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
}
