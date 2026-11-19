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
    @Test void activeSenderCannotBeReclaimedWhenItsLeaseExpires() throws Exception {
        pick(propose(7200));var started=new CountDownLatch(1);var release=new CountDownLatch(1);var deliveries=new java.util.concurrent.atomic.AtomicInteger();
        doAnswer(inv->{if(deliveries.incrementAndGet()==1){started.countDown();if(!release.await(10,TimeUnit.SECONDS))throw new IllegalStateException("Test release timeout");}return null;}).when(mail).send(any());
        var pool=Executors.newSingleThreadExecutor();
        try {
            var first=pool.submit(()->worker.dispatchBatch());assertTrue(started.await(10,TimeUnit.SECONDS));
            when(clock.instant()).thenReturn(NOW.plusSeconds(61));worker.dispatchBatch();release.countDown();first.get(10,TimeUnit.SECONDS);
            when(clock.instant()).thenReturn(NOW.plusSeconds(72));worker.dispatchBatch();
            assertEquals(2,deliveries.get());assertEquals(2,jdbc.queryForObject("select count(*) from recruitment.interview_notification_outbox where type='CONFIRMATION' and status='SENT'",Integer.class));
        } finally {release.countDown();pool.shutdownNow();}
    }
    @Test void sentAndFinalMilestoneCommitTogether() throws Exception {
        pick(propose(7200));worker.dispatchBatch();when(clock.instant()).thenReturn(NOW.plusSeconds(3600));job.enqueueDue();
        jdbc.execute("create or replace function recruitment.ut06_milestone_fail() returns trigger language plpgsql as $$ begin if NEW.reminded_1h_at is not null then raise exception 'UT06 milestone failure'; end if; return NEW; end $$");
        jdbc.execute("create trigger ut06_milestone_fail before update on recruitment.interview_schedules for each row execute function recruitment.ut06_milestone_fail()");
        try {worker.dispatchBatch();assertTrue(jdbc.queryForObject("select count(*) from recruitment.interview_notification_outbox where type='REMINDER_1H' and status<>'SENT'",Integer.class)>0);}
        finally{jdbc.execute("drop trigger ut06_milestone_fail on recruitment.interview_schedules");jdbc.execute("drop function recruitment.ut06_milestone_fail()");}
        when(clock.instant()).thenReturn(NOW.plusSeconds(3661));worker.dispatchBatch();assertNotNull(jdbc.queryForObject("select reminded_1h_at from recruitment.interview_schedules",java.sql.Timestamp.class));
    }
}
