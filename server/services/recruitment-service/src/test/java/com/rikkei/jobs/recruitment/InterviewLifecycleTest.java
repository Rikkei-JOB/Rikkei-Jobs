package com.rikkei.jobs.recruitment;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
class InterviewLifecycleTest extends InterviewIntegrationSupport {
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
}
