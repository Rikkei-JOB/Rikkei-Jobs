package com.rikkei.jobs.recruitment;
import com.rikkei.jobs.recruitment.service.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
class InterviewReminderJobTest extends InterviewIntegrationSupport {
    @Autowired InterviewReminderJob job;
    @Autowired InterviewNotificationWorker worker;
    @MockBean InterviewMailSender mail;
    int reminders(){return jdbc.queryForObject("select count(*) from recruitment.interview_notification_outbox where type like 'REMINDER_%'",Integer.class);}
    @Test void boundaryWindowsAndRepeatedRunsOnlyQueueCurrentMilestone() throws Exception {
        UUID slot=propose(90000);pick(slot);
        when(clock.instant()).thenReturn(NOW.plusSeconds(3600)); // exactly 24h remaining
        job.enqueueDue();job.enqueueDue();assertEquals(2,reminders());
        when(clock.instant()).thenReturn(NOW.plusSeconds(86400)); // exactly 1h remaining
        job.enqueueDue();job.enqueueDue();assertEquals(4,reminders());
        when(clock.instant()).thenReturn(NOW.plusSeconds(90000));
        job.enqueueDue();assertEquals(4,reminders());
        jdbc.update("update recruitment.interview_schedules set status='CANCELLED'");
        worker.dispatchBatch();assertEquals(0,jdbc.queryForObject("select count(*) from recruitment.interview_notification_outbox where type like 'REMINDER_%' and status='SENT'",Integer.class));
    }
    @Test void confirmationWithinOneHourSuppressesReminderAndDeduplicatesRecipients() throws Exception {
        jdbc.update("update recruitment.interview_rounds set interviewer_email='CANDIDATE@TEST.LOCAL' where id=?",roundId);
        UUID slot=propose(3600);pick(slot);job.enqueueDue();assertEquals(0,reminders());
        assertEquals(1,jdbc.queryForObject("select count(*) from recruitment.interview_notification_outbox where type='CONFIRMATION'",Integer.class));
    }
    @Test void workerRetriesOnlyFailedRecipientAndSetsMilestoneAfterAllSent() throws Exception {
        UUID slot=propose(7200);pick(slot);worker.dispatchBatch();
        when(clock.instant()).thenReturn(NOW.plusSeconds(3600));job.enqueueDue();
        doAnswer(a->{var n=a.getArgument(0,com.rikkei.jobs.recruitment.entity.InterviewNotificationOutbox.class);if(n.getRecipient().equals("hr@test.local"))throw new IllegalStateException("SMTP down");return null;}).when(mail).send(any());
        worker.dispatchBatch();
        assertEquals(1,jdbc.queryForObject("select count(*) from recruitment.interview_notification_outbox where type='REMINDER_1H' and status='SENT'",Integer.class));
        assertNull(jdbc.queryForObject("select reminded_1h_at from recruitment.interview_schedules",java.sql.Timestamp.class));
        doNothing().when(mail).send(any());when(clock.instant()).thenReturn(NOW.plusSeconds(3661));worker.dispatchBatch();worker.dispatchBatch();
        assertEquals(2,jdbc.queryForObject("select count(*) from recruitment.interview_notification_outbox where type='REMINDER_1H' and status='SENT'",Integer.class));
        assertNotNull(jdbc.queryForObject("select reminded_1h_at from recruitment.interview_schedules",java.sql.Timestamp.class));
    }
}
