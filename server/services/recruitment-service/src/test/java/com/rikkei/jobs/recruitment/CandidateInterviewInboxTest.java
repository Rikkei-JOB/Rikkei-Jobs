package com.rikkei.jobs.recruitment;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.rikkei.jobs.recruitment.service.InterviewReminderJob;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CandidateInterviewInboxTest extends InterviewIntegrationSupport {
    @Autowired InterviewReminderJob reminders;

    @Test void invitationAndConfirmationAreStoredOnlyOnceDespitePickRetry() throws Exception {
        UUID slot = propose(7200, 10800);
        pick(slot).andExpect(status().isOk());
        pick(slot).andExpect(status().isOk());
        assertEquals(2, count("candidate_notifications"));
        mvc.perform(get("/recruitment/me/notifications").header("Authorization", candidate()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.unreadCount").value(2))
            .andExpect(jsonPath("$.items[0].href").value("/candidate/interviews"));
    }

    @Test void reschedulingCreatesCancellationEvenWhenEmailWasNeverDelivered() throws Exception {
        pick(propose(7200)).andExpect(status().isOk());
        mvc.perform(post("/recruitment/rounds/{r}/reschedule", roundId).header("Authorization", hr()).contentType("application/json").content(slots(14400)))
            .andExpect(status().isOk());
        assertEquals(4, count("candidate_notifications"));
        assertEquals(1, jdbc.queryForObject("select count(*) from recruitment.candidate_notifications where type='INTERVIEW_CANCELLED'", Integer.class));
        assertEquals(0, jdbc.queryForObject("select count(*) from recruitment.interview_notification_outbox where type='CANCELLATION'", Integer.class));
    }

    @Test void reminderJobStoresOneNotificationPerReminderWindow() throws Exception {
        pick(propose(7200)).andExpect(status().isOk());
        reminders.enqueueDue(); reminders.enqueueDue();
        assertEquals(1, jdbc.queryForObject("select count(*) from recruitment.candidate_notifications where type='INTERVIEW_REMINDER'", Integer.class));
        org.mockito.Mockito.when(clock.instant()).thenReturn(NOW.plusSeconds(5400));
        reminders.enqueueDue(); reminders.enqueueDue();
        assertEquals(2, jdbc.queryForObject("select count(*) from recruitment.candidate_notifications where type='INTERVIEW_REMINDER'", Integer.class));
    }
}
