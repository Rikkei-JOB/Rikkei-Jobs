package com.rikkei.jobs.recruitment;
import com.rikkei.jobs.common.config.security.principle.Role;
import com.rikkei.jobs.recruitment.config.RecruitmentDependencyException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class InterviewSchedulingApiTest extends InterviewIntegrationSupport {
    @Test void validatesWholeProposal() throws Exception {
        for(String body:new String[]{"[]",slots(-3600),slots(3600,3700),slots(3600,7200,10800,14400,18000,21600),"[null]","[{\"startAt\":\"2030-01-01T09:00:00\",\"endAt\":\"2030-01-01T10:00:00\"}]","[{\"startAt\":\"2030-01-01T09:00:00Z\",\"endAt\":\"2030-01-01T09:29:59Z\"}]"})
            mvc.perform(post("/recruitment/rounds/{r}/slots",roundId).header("Authorization",hr()).contentType("application/json").content(body)).andExpect(status().isBadRequest());
        assertEquals(0,count("interview_slots"));
        propose(3600,5400,7200,9000,10800);
        assertEquals(5,count("interview_slots"));
    }
    @Test void picksOneCancelsOthersAndRetriesWithoutDuplicateSideEffects() throws Exception {
        UUID slot=propose(3600,7200);
        pick(slot).andExpect(status().isOk()).andExpect(jsonPath("$.meetUrl").value("https://meet.test/room"));
        pick(slot).andExpect(status().isOk());
        assertEquals(1,count("interview_schedules"));
        assertEquals(2,jdbc.queryForObject("select count(*) from recruitment.interview_notification_outbox where type='CONFIRMATION'",Integer.class));
        assertEquals(1,jdbc.queryForObject("select count(*) from recruitment.interview_slots where status='CANCELLED'",Integer.class));
        UUID cancelled=jdbc.queryForObject("select id from recruitment.interview_slots where status='CANCELLED'",UUID.class);
        pick(cancelled).andExpect(status().isConflict());
        verify(meet,times(1)).create(any(),any(),any(),any());
    }
    @Test void enforcesOwnershipRolesAndSlotMembership() throws Exception {
        UUID slot=propose(3600);
        mvc.perform(post("/recruitment/me/interviews/{r}/pick",roundId).header("Authorization",token(UUID.randomUUID(),Role.CANDIDATE,null)).contentType("application/json").content("{\"slotId\":\""+slot+"\"}")).andExpect(status().isNotFound());
        mvc.perform(post("/recruitment/rounds/{r}/slots",roundId).header("Authorization",token(UUID.randomUUID(),Role.HR,UUID.randomUUID())).contentType("application/json").content(slots(3600))).andExpect(status().isNotFound());
        mvc.perform(post("/recruitment/rounds/{r}/slots",roundId).header("Authorization",candidate()).contentType("application/json").content(slots(3600))).andExpect(status().isForbidden());
        pick(UUID.randomUUID()).andExpect(status().isNotFound());
    }
    @Test void upstreamFailureLeavesProposalOpen() throws Exception {
        UUID slot=propose(3600);
        when(meet.create(any(),any(),any(),any())).thenThrow(new RecruitmentDependencyException(503,"MEET_UNAVAILABLE","Meet unavailable"));
        pick(slot).andExpect(status().isServiceUnavailable());
        assertEquals(0,count("interview_schedules"));
        assertEquals("OPEN",jdbc.queryForObject("select status from recruitment.interview_slots where id=?",String.class,slot));
    }
}
