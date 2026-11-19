package com.rikkei.jobs.recruitment;
import com.rikkei.jobs.common.config.security.principle.Role;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
class InterviewQueryApiTest extends InterviewIntegrationSupport {
    @Test void candidateSeesOwnInvitationsAndHrSeesCompanyCalendarWithBounds() throws Exception {
        UUID slot=propose(7200);pick(slot);
        mvc.perform(get("/recruitment/me/interviews").header("Authorization",candidate())).andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1)).andExpect(jsonPath("$.items[0].schedule.meetUrl").value("https://meet.test/room"));
        mvc.perform(get("/recruitment/me/interviews").header("Authorization",token(UUID.randomUUID(),Role.CANDIDATE,null))).andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0));
        String from=NOW.plusSeconds(7500).toString(),to=NOW.plusSeconds(8000).toString();
        mvc.perform(get("/recruitment/companies/me/interviews").param("from",from).param("to",to).param("interviewerEmail","HR@TEST.LOCAL").header("Authorization",hr())).andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1));
        mvc.perform(get("/recruitment/companies/me/interviews").param("from",NOW.plusSeconds(9000).toString()).param("to",NOW.plusSeconds(12000).toString()).header("Authorization",hr())).andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0));
        mvc.perform(get("/recruitment/companies/me/interviews").param("from",from).param("to",to).header("Authorization",token(UUID.randomUUID(),Role.HR,UUID.randomUUID()))).andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0));
    }
    @Test void rejectsBadRangesAndPagination() throws Exception {
        mvc.perform(get("/recruitment/me/interviews").param("page","0").header("Authorization",candidate())).andExpect(status().isBadRequest());
        mvc.perform(get("/recruitment/me/interviews").param("size","101").header("Authorization",candidate())).andExpect(status().isBadRequest());
        mvc.perform(get("/recruitment/companies/me/interviews").param("from",NOW.toString()).param("to",NOW.toString()).header("Authorization",hr())).andExpect(status().isBadRequest());
        mvc.perform(get("/recruitment/companies/me/interviews").param("from","2030-01-01T09:00:00").param("to",NOW.toString()).header("Authorization",hr())).andExpect(status().isBadRequest());
    }
    @Test void hrRoundDetailIncludesProposalAndCurrentSchedule() throws Exception {
        var slot=propose(7200);
        mvc.perform(get("/recruitment/applications/{a}",applicationId).header("Authorization",hr())).andExpect(status().isOk()).andExpect(jsonPath("$.rounds[0].slots[0].id").value(slot.toString()));
        pick(slot);
        mvc.perform(get("/recruitment/applications/{a}",applicationId).header("Authorization",hr())).andExpect(status().isOk()).andExpect(jsonPath("$.rounds[0].schedule.meetUrl").value("https://meet.test/room"));
    }
    @Test void currentScheduleWinsOverCancelledHistoryEvenWhenClockMovesBack() throws Exception {
        pick(propose(7200));
        jdbc.update("update recruitment.interview_schedules set created_at=?",java.sql.Timestamp.from(NOW.plusSeconds(1)));
        var text=mvc.perform(post("/recruitment/rounds/{r}/reschedule",roundId).header("Authorization",hr()).contentType("application/json").content(slots(14400))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        pick(UUID.fromString(json.readTree(text).get("slots").get(0).get("id").asText()));
        mvc.perform(get("/recruitment/me/interviews").header("Authorization",candidate())).andExpect(status().isOk()).andExpect(jsonPath("$.items[0].schedule.status").value("SCHEDULED"));
    }
}
