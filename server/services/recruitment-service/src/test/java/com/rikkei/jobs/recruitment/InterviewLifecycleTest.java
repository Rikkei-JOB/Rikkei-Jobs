package com.rikkei.jobs.recruitment;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
class InterviewLifecycleTest extends InterviewIntegrationSupport {
    @org.springframework.boot.test.mock.mockito.SpyBean com.rikkei.jobs.recruitment.service.InterviewNotifications notifications;
    @org.springframework.beans.factory.annotation.Autowired com.rikkei.jobs.recruitment.service.InterviewNotificationWorker worker;
    @org.springframework.boot.test.mock.mockito.MockBean com.rikkei.jobs.recruitment.service.InterviewMailSender mail;
    @Test void rescheduleCancelsImmediatelyButInvalidProposalPreservesOldSchedule() throws Exception {
        UUID old=propose(7200);pick(old).andExpect(status().isOk());
        mvc.perform(post("/recruitment/rounds/{r}/reschedule",roundId).header("Authorization",hr()).contentType("application/json").content(slots(-3600))).andExpect(status().isBadRequest());
        assertEquals("SCHEDULED",jdbc.queryForObject("select status from recruitment.interview_schedules",String.class));
        jdbc.update("update recruitment.interview_notification_outbox set attempts=1 where type='CONFIRMATION'");
        mvc.perform(post("/recruitment/rounds/{r}/reschedule",roundId).header("Authorization",hr()).contentType("application/json").content(slots(14400,18000))).andExpect(status().isOk());
        assertEquals("CANCELLED",jdbc.queryForObject("select status from recruitment.interview_schedules",String.class));
        assertEquals(2,jdbc.queryForObject("select count(*) from recruitment.interview_notification_outbox where type='CANCELLATION'",Integer.class));
        assertEquals(2,jdbc.queryForObject("select count(*) from recruitment.interview_slots where status='OPEN'",Integer.class));
        assertNull(jdbc.queryForObject("select scheduled_at from recruitment.interview_rounds where id=?",java.sql.Timestamp.class,roundId));
        pick(old).andExpect(status().isConflict());
    }
    @Test void leavingInterviewCancelsAndCompletedResultDoesNotReopenSchedule() throws Exception {
        UUID slot=propose(7200);pick(slot).andExpect(status().isOk());
        mvc.perform(put("/recruitment/applications/{a}/rounds/{r}",applicationId,roundId).header("Authorization",hr()).contentType("application/json").content("{\"result\":\"PASS\",\"score\":80}")).andExpect(status().isOk());
        assertEquals("DONE",jdbc.queryForObject("select status from recruitment.interview_schedules",String.class));
        mvc.perform(put("/recruitment/applications/{a}/rounds/{r}",applicationId,roundId).header("Authorization",hr()).contentType("application/json").content("{\"result\":\"PENDING\"}")).andExpect(status().isOk());
        assertEquals("DONE",jdbc.queryForObject("select status from recruitment.interview_schedules",String.class));
        mvc.perform(post("/recruitment/rounds/{r}/reschedule",roundId).header("Authorization",hr()).contentType("application/json").content(slots(14400))).andExpect(status().isOk());
        mvc.perform(post("/recruitment/applications/{a}/transition",applicationId).header("Authorization",hr()).contentType("application/json").content("{\"toStatus\":\"REJECTED\",\"note\":\"Không phù hợp\"}")).andExpect(status().isOk());
        assertEquals(0,jdbc.queryForObject("select count(*) from recruitment.interview_slots where status='OPEN'",Integer.class));
    }
    @Test void preservesLegacyTimeAndCompletedPickedSlotWhenApplicationLeavesInterview() throws Exception {
        jdbc.update("update recruitment.interview_rounds set scheduled_at=? where id=?",java.sql.Timestamp.from(NOW.plusSeconds(7200)),roundId);
        mvc.perform(post("/recruitment/applications/{a}/transition",applicationId).header("Authorization",hr()).contentType("application/json").content("{\"toStatus\":\"REJECTED\",\"note\":\"Reason\"}")).andExpect(status().isOk());
        org.junit.jupiter.api.Assertions.assertNotNull(jdbc.queryForObject("select scheduled_at from recruitment.interview_rounds where id=?",java.sql.Timestamp.class,roundId));
    }
    @Test void completedBookingRetainsPickedSlotAndHistoricalTime() throws Exception {
        var slot=propose(7200);pick(slot);
        mvc.perform(put("/recruitment/applications/{a}/rounds/{r}",applicationId,roundId).header("Authorization",hr()).contentType("application/json").content("{\"result\":\"PASS\",\"score\":80}")).andExpect(status().isOk());
        mvc.perform(post("/recruitment/applications/{a}/transition",applicationId).header("Authorization",hr()).contentType("application/json").content("{\"toStatus\":\"REJECTED\",\"note\":\"Reason\"}")).andExpect(status().isOk());
        assertEquals("PICKED",jdbc.queryForObject("select status from recruitment.interview_slots where id=?",String.class,slot));
        assertNotNull(jdbc.queryForObject("select scheduled_at from recruitment.interview_rounds where id=?",java.sql.Timestamp.class,roundId));
    }
    @Test void cancellationCannotMissWorkerStartingBetweenRecipientReadAndCancel() throws Exception {
        pick(propose(7200));
        org.mockito.Mockito.doAnswer(inv->{
            Object recipients=inv.callRealMethod();
            var pool=java.util.concurrent.Executors.newSingleThreadExecutor();
            try{pool.submit(()->worker.dispatchBatch()).get(10,java.util.concurrent.TimeUnit.SECONDS);}finally{pool.shutdownNow();}
            return recipients;
        }).when(notifications).attemptedRecipients(org.mockito.ArgumentMatchers.any());
        mvc.perform(post("/recruitment/rounds/{r}/reschedule",roundId).header("Authorization",hr()).contentType("application/json").content(slots(14400))).andExpect(status().isOk());
        int accepted=jdbc.queryForObject("select count(*) from recruitment.interview_notification_outbox where type='CONFIRMATION' and attempts>0",Integer.class);
        int cancelled=jdbc.queryForObject("select count(*) from recruitment.interview_notification_outbox where type='CANCELLATION'",Integer.class);
        org.junit.jupiter.api.Assertions.assertTrue(cancelled>=accepted,"Every attempted confirmation needs cancellation");
    }
}
