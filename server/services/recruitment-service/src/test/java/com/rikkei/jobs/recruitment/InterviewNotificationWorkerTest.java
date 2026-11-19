package com.rikkei.jobs.recruitment;
import com.rikkei.jobs.recruitment.service.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
class InterviewNotificationWorkerTest extends InterviewIntegrationSupport {
    @Autowired InterviewNotificationWorker worker;
    @Autowired InterviewReminderJob job;
    @MockBean InterviewMailSender mail;
    @Test void multipleWorkersAndJobsDoNotDuplicateRecipients() throws Exception {
        pick(propose(7200));
        var pool=Executors.newFixedThreadPool(2);
        try {var a=pool.submit(()->{job.enqueueDue();worker.dispatchBatch();});var b=pool.submit(()->{job.enqueueDue();worker.dispatchBatch();});a.get(20,TimeUnit.SECONDS);b.get(20,TimeUnit.SECONDS);}finally{pool.shutdownNow();}
        when(clock.instant()).thenReturn(NOW.plusSeconds(11));worker.dispatchBatch();
        assertEquals(4,jdbc.queryForObject("select count(*) from recruitment.interview_notification_outbox where type in ('CONFIRMATION','REMINDER_24H') and status='SENT'",Integer.class));
        assertEquals(0,jdbc.queryForObject("select count(*) from recruitment.interview_notification_outbox where attempts>1",Integer.class));
    }
    @Test void expiredLeaseRetriesAndFiveFailuresStopRetrying() throws Exception {
        pick(propose(7200));
        jdbc.update("update recruitment.interview_notification_outbox set status='PROCESSING',attempts=4,lease_until=? where type='CONFIRMATION'",java.sql.Timestamp.from(NOW.minusSeconds(1)));
        doThrow(new IllegalStateException("SMTP unavailable")).when(mail).send(any());worker.dispatchBatch();
        assertEquals(2,jdbc.queryForObject("select count(*) from recruitment.interview_notification_outbox where type='CONFIRMATION' and status='FAILED' and attempts=5",Integer.class));
        worker.dispatchBatch();assertEquals(0,jdbc.queryForObject("select count(*) from recruitment.interview_notification_outbox where attempts>5",Integer.class));
    }
}
